/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.xml.ws.transport.http.client;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.ha.StickyFeature;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;
import com.sun.xml.ws.client.ClientTransportException;
import com.sun.xml.ws.developer.HttpConfigFeature;
import com.sun.xml.ws.resources.ClientMessages;
import com.sun.xml.ws.transport.Headers;
import com.sun.xml.ws.util.ByteArrayBuffer;
import com.sun.xml.ws.util.RuntimeVersion;
import com.sun.xml.ws.util.StreamUtils;
import ru.codeinside.gses.webui.osgi.LogCustomizer;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPBinding;
import java.io.*;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * {@link com.sun.xml.ws.api.pipe.Tube} that sends a request to a remote HTTP server.
 *
 * TODO: need to create separate HTTP transport pipes for binding. SOAP1.1, SOAP1.2,
 * TODO: XML/HTTP differ in handling status codes.
 *
 * @author Jitendra Kotamraju
 */
public class OepHttpTransportPipe extends AbstractTubeImpl {
    private static final Logger LOGGER = Logger.getLogger(OepHttpTransportPipe.class.getName());

    private final Codec codec;
    private final WSBinding binding;

    private static final List<String> USER_AGENT = Collections.singletonList(RuntimeVersion.VERSION.toString());
    private final CookieHandler cookieJar;      // shared object among the tubes
    private final boolean sticky;

    // Need to use JAXB first to register DatatypeConverter
    static {
        try {
            JAXBContext.newInstance().createUnmarshaller();
        } catch(JAXBException je) {
            // Nothing much can be done. Intentionally left empty
        }
    }

    public OepHttpTransportPipe(Codec codec, WSBinding binding) {
        this.codec = codec;
        this.binding = binding;
        this.sticky = isSticky(binding);
        HttpConfigFeature configFeature = binding.getFeature(HttpConfigFeature.class);
        if (configFeature == null) {
            configFeature = new HttpConfigFeature();
        }
        this.cookieJar = configFeature.getCookieHandler();
    }

    private static boolean isSticky(WSBinding binding) {
        boolean tSticky = false;
        WebServiceFeature[] features = binding.getFeatures().toArray();
        for(WebServiceFeature f : features) {
            if (f instanceof StickyFeature) {
                tSticky = true;
                break;
            }
        }
        return tSticky;
    }

    /*
     * Copy constructor for {@link Tube#copy(TubeCloner)}.
     */
    private OepHttpTransportPipe(OepHttpTransportPipe that, TubeCloner cloner) {
        this(that.codec.copy(), that.binding);
        cloner.add(that,this);
    }

    public NextAction processException(@NotNull Throwable t) {
        return doThrow(t);
    }

    public NextAction processRequest(@NotNull Packet request) {
        return doReturnWith(process(request));
    }

    public NextAction processResponse(@NotNull Packet response) {
        return doReturnWith(response);
    }

    protected OepHttpClientTransport getTransport(Packet request, Map<String, List<String>> reqHeaders) {
        return new OepHttpClientTransport(request, reqHeaders);
    }

    @Override
    public Packet process(Packet request) {
        OepHttpClientTransport con;
        try {
            // get transport headers from message
            Map<String, List<String>> reqHeaders = new Headers();
            @SuppressWarnings("unchecked")
            Map<String, List<String>> userHeaders = (Map<String, List<String>>) request.invocationProperties.get(MessageContext.HTTP_REQUEST_HEADERS);
            boolean addUserAgent = true;
            if (userHeaders != null) {
                // userHeaders may not be modifiable like SingletonMap, just copy them
                reqHeaders.putAll(userHeaders);
                // application wants to use its own User-Agent header
                if (userHeaders.get("User-Agent") != null) {
                    addUserAgent = false;
                }
            }
            if (addUserAgent) {
                reqHeaders.put("User-Agent", USER_AGENT);
            }

            addBasicAuth(request, reqHeaders);
            addCookies(request, reqHeaders);

            con = getTransport(request, reqHeaders);
            request.addSatellite(new HttpResponseProperties(con));

            ContentType ct = codec.getStaticContentType(request);

            String id = getMarker(reqHeaders);

            if (ct == null) {
                ByteArrayBuffer buf = new ByteArrayBuffer();

                ct = codec.encode(request, buf);
                // data size is available, set it as Content-Length
                reqHeaders.put("Content-Length", Collections.singletonList(Integer.toString(buf.size())));
                reqHeaders.put("Content-Type", Collections.singletonList(ct.getContentType()));
                if (ct.getAcceptHeader() != null) {
                    reqHeaders.put("Accept", Collections.singletonList(ct.getAcceptHeader()));
                }
                if (binding instanceof SOAPBinding) {
                    writeSOAPAction(reqHeaders, ct.getSOAPActionHeader());
                }

                if(LogCustomizer.getLogger().shouldWriteClientLog())
                    dump(buf, true, reqHeaders, id);

                buf.writeTo(con.getOutput());
            } else {
                // Set static Content-Type
                reqHeaders.put("Content-Type", Collections.singletonList(ct.getContentType()));
                if (ct.getAcceptHeader() != null) {
                    reqHeaders.put("Accept", Collections.singletonList(ct.getAcceptHeader()));
                }
                if (binding instanceof SOAPBinding) {
                    writeSOAPAction(reqHeaders, ct.getSOAPActionHeader());
                }

                if(LogCustomizer.getLogger().shouldWriteClientLog()) {
                    ByteArrayBuffer buf = new ByteArrayBuffer();
                    codec.encode(request, buf);
                    //"HTTP request - "+request.endpointAddress
                    dump(buf, true, reqHeaders, id);
                    OutputStream out = con.getOutput();
                    if (out != null) {
                        buf.writeTo(out);
                    }
                } else {
                    OutputStream os = con.getOutput();
                    if (os != null) {
                        codec.encode(request, os);
                    }
                }
            }

            con.closeOutput();

            return createResponsePacket(request, con);
        } catch(WebServiceException wex) {
            throw wex;
        } catch(Exception ex) {
            throw new WebServiceException(ex);
        }
    }

    private Packet createResponsePacket(Packet request, OepHttpClientTransport con) throws IOException {
        con.readResponseCodeAndMessage();   // throws IOE
        recordCookies(request, con);

        InputStream responseStream = con.getInput();
        if (LogCustomizer.getLogger().shouldWriteClientLog()) {
            ByteArrayBuffer buf = new ByteArrayBuffer();
            if (responseStream != null) {
                buf.write(responseStream);
                responseStream.close();
            }
            //"HTTP response - "+request.endpointAddress+" - "+con.statusCode
            String marker = getMarker((Map<String, List<String>>) request.invocationProperties.get(MessageContext.HTTP_REQUEST_HEADERS));
            dump(buf, false, con.getHeaders(), marker);
            responseStream = buf.newInputStream();
        }

        // Check if stream contains any data
        int cl = con.contentLength;
        InputStream tempIn = null;
        if (cl == -1) {                     // No Content-Length header
            tempIn = StreamUtils.hasSomeData(responseStream);
            if (tempIn != null) {
                responseStream = tempIn;
            }
        }
        if (cl == 0 || (cl == -1 && tempIn == null)) {
            if(responseStream != null) {
                responseStream.close();         // No data, so close the stream
                responseStream = null;
            }

        }

        // Allows only certain http status codes for a binding. For all
        // other status codes, throws exception
        checkStatusCode(responseStream, con); // throws ClientTransportException

        Packet reply = request.createClientResponse(null);
        reply.wasTransportSecure = con.isSecure();
        if (responseStream != null) {
            String contentType = con.getContentType();
            if (contentType != null && contentType.contains("text/html") && binding instanceof SOAPBinding) {
                throw new ClientTransportException(ClientMessages.localizableHTTP_STATUS_CODE(con.statusCode, con.statusMessage));
            }
            codec.decode(responseStream, contentType, reply);
        }
        return reply;
    }

    private String getMarker(Map<String, List<String>> map) {
      if (map == null) {
        return "";
      }
        List<String> markers = map.get("markerId");
        if(markers == null || markers.isEmpty()){
            return "";
        } else {
            return markers.get(0);
        }
    }

    /*
     * Allows the following HTTP status codes.
     * SOAP 1.1/HTTP - 200, 202, 500
     * SOAP 1.2/HTTP - 200, 202, 400, 500
     * XML/HTTP - all
     *
     * For all other status codes, it throws an exception
     */
    private void checkStatusCode(InputStream in, OepHttpClientTransport con) throws IOException {
        int statusCode = con.statusCode;
        String statusMessage = con.statusMessage;
        // SOAP1.1 and SOAP1.2 differ here
        if (binding instanceof SOAPBinding) {
            if (binding.getSOAPVersion() == SOAPVersion.SOAP_12) {
                //In SOAP 1.2, Fault messages can be sent with 4xx and 5xx error codes
                if (statusCode == HttpURLConnection.HTTP_OK || statusCode == HttpURLConnection.HTTP_ACCEPTED || isErrorCode(statusCode)) {
                    // acceptable status codes for SOAP 1.2
                    if (isErrorCode(statusCode) && in == null) {
                        // No envelope for the error, so throw an exception with http error details
                        throw new ClientTransportException(ClientMessages.localizableHTTP_STATUS_CODE(statusCode, statusMessage));
                    }
                    return;
                }
            } else {
                // SOAP 1.1
                if (statusCode == HttpURLConnection.HTTP_OK || statusCode == HttpURLConnection.HTTP_ACCEPTED || statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    // acceptable status codes for SOAP 1.1
                    if (statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR && in == null) {
                        // No envelope for the error, so throw an exception with http error details
                        throw new ClientTransportException(ClientMessages.localizableHTTP_STATUS_CODE(statusCode, statusMessage));
                    }
                    return;
                }
            }
            if (in != null) {
                in.close();
            }
            throw new ClientTransportException(ClientMessages.localizableHTTP_STATUS_CODE(statusCode, statusMessage));
        }
        // Every status code is OK for XML/HTTP
    }

    private boolean isErrorCode(int code) {
        //if(code/100 == 5/*Server-side error*/ || code/100 == 4 /*client error*/ ) {
        return code == 500 || code == 400;
    }

    private void addCookies(Packet context, Map<String, List<String>> reqHeaders) throws IOException {
        Boolean shouldMaintainSessionProperty =
                (Boolean) context.invocationProperties.get(BindingProvider.SESSION_MAINTAIN_PROPERTY);
        if (shouldMaintainSessionProperty != null && !shouldMaintainSessionProperty) {
            return;         // explicitly turned off
        }
        if (sticky || (shouldMaintainSessionProperty != null && shouldMaintainSessionProperty)) {
            Map<String, List<String>> cookies = cookieJar.get(context.endpointAddress.getURI(),reqHeaders);
            List<String> cookieList = cookies.get("Cookie");
            if (cookieList != null && !cookieList.isEmpty()) {
                reqHeaders.put("Cookie", cookieList);
            }
            cookieList = cookies.get("Cookie2");
            if (cookieList != null && !cookieList.isEmpty()) {
                reqHeaders.put("Cookie2", cookieList);
            }
        }
    }

    private void recordCookies(Packet context, OepHttpClientTransport con) throws IOException {
        Boolean shouldMaintainSessionProperty =
                (Boolean) context.invocationProperties.get(BindingProvider.SESSION_MAINTAIN_PROPERTY);
        if (shouldMaintainSessionProperty != null && !shouldMaintainSessionProperty) {
            return;         // explicitly turned off
        }
        if (sticky || (shouldMaintainSessionProperty != null && shouldMaintainSessionProperty)) {
            cookieJar.put(context.endpointAddress.getURI(), con.getHeaders());
        }
    }

    private void addBasicAuth(Packet context, Map<String, List<String>> reqHeaders) {
        String user = (String) context.invocationProperties.get(BindingProvider.USERNAME_PROPERTY);
        if (user != null) {
            String pw = (String) context.invocationProperties.get(BindingProvider.PASSWORD_PROPERTY);
            if (pw != null) {
                StringBuffer buf = new StringBuffer(user);
                buf.append(":");
                buf.append(pw);
                String creds = DatatypeConverter.printBase64Binary(buf.toString().getBytes());
                reqHeaders.put("Authorization", Collections.singletonList("Basic "+creds));
            }
        }
    }

    /*
     * write SOAPAction header if the soapAction parameter is non-null or BindingProvider properties set.
     * BindingProvider properties take precedence.
     */
    private void writeSOAPAction(Map<String, List<String>> reqHeaders, String soapAction) {
        //dont write SOAPAction HTTP header for SOAP 1.2 messages.
        if(SOAPVersion.SOAP_12.equals(binding.getSOAPVersion()))
            return;
        if (soapAction != null)
            reqHeaders.put("SOAPAction", Collections.singletonList(soapAction));
        else
            reqHeaders.put("SOAPAction", Collections.singletonList("\"\""));
    }

    public void preDestroy() {
        // nothing to do. Intentionally left empty.
    }

    public OepHttpTransportPipe copy(TubeCloner cloner) {
        return new OepHttpTransportPipe(this,cloner);
    }


    private void dump(ByteArrayBuffer buf, boolean isRequest, Map<String, List<String>> headers, String marker) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos, true);
        //pw.println("!!!---!!!["+caption +"]!!!---!!!");
        for (Entry<String,List<String>> header : headers.entrySet()) {
            if(header.getValue().isEmpty()) {
                // I don't think this is legal, but let's just dump it,
                // as the point of the dump is to uncover problems.
                pw.println(header.getValue());
            } else {
                for (String value : header.getValue()) {
                    pw.println(header.getKey()+": "+value);
                }
            }
        }

        buf.writeTo(baos);
        //pw.println("!!!--------!!!------------!!!");

        String msg = baos.toString();
        LogCustomizer.getLogger().log( marker, msg, isRequest, true);
    }
}