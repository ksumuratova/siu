/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.keys.keyresolver.implementations;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.x509.XMLX509SKI;
import org.apache.xml.security.keys.keyresolver.KeyResolverException;
import org.apache.xml.security.keys.keyresolver.KeyResolverSpi;
import org.apache.xml.security.keys.storage.StorageResolver;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Element;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Iterator;

public class X509SKIResolver extends KeyResolverSpi {

  /**
   * {@link org.apache.commons.logging} logging facility
   */
  private static org.apache.commons.logging.Log log =
    org.apache.commons.logging.LogFactory.getLog(X509SKIResolver.class);


  /**
   * Method engineResolvePublicKey
   *
   * @param element
   * @param baseURI
   * @param storage
   * @return null if no {@link PublicKey} could be obtained
   * @throws KeyResolverException
   */
  public PublicKey engineLookupAndResolvePublicKey(
    Element element, String baseURI, StorageResolver storage
  ) throws KeyResolverException {

    X509Certificate cert =
      this.engineLookupResolveX509Certificate(element, baseURI, storage);

    if (cert != null) {
      return cert.getPublicKey();
    }

    return null;
  }

  /**
   * Method engineResolveX509Certificate
   *
   * @param element
   * @param baseURI
   * @param storage
   * @throws KeyResolverException
   * @inheritDoc
   */
  public X509Certificate engineLookupResolveX509Certificate(
    Element element, String baseURI, StorageResolver storage
  ) throws KeyResolverException {
    if (log.isDebugEnabled()) {
      log.debug("Can I resolve " + element.getTagName() + "?");
    }
    if (!XMLUtils.elementIsInSignatureSpace(element, Constants._TAG_X509DATA)) {
      if (log.isDebugEnabled()) {
        log.debug("I can't");
      }
      return null;
    }
    /** Field _x509childObject[] */
    XMLX509SKI x509childObject[] = null;

    Element x509childNodes[] = null;
    x509childNodes = XMLUtils.selectDsNodes(element.getFirstChild(), Constants._TAG_X509SKI);

    if (!((x509childNodes != null) && (x509childNodes.length > 0))) {
      if (log.isDebugEnabled()) {
        log.debug("I can't");
      }
      return null;
    }
    try {
      if (storage == null) {
        Object exArgs[] = {Constants._TAG_X509SKI};
        KeyResolverException ex =
          new KeyResolverException("KeyResolver.needStorageResolver", exArgs);

        if (log.isDebugEnabled()) {
          log.debug("", ex);
        }

        throw ex;
      }

      x509childObject = new XMLX509SKI[x509childNodes.length];

      for (int i = 0; i < x509childNodes.length; i++) {
        x509childObject[i] = new XMLX509SKI(x509childNodes[i], baseURI);
      }

      Iterator<Certificate> storageIterator = storage.getIterator();
      while (storageIterator.hasNext()) {
        X509Certificate cert = (X509Certificate) storageIterator.next();
        XMLX509SKI certSKI = new XMLX509SKI(element.getOwnerDocument(), cert);

        for (int i = 0; i < x509childObject.length; i++) {
          if (certSKI.equals(x509childObject[i])) {
            if (log.isDebugEnabled()) {
              log.debug("Return PublicKey from " + cert.getSubjectX500Principal().getName());
            }

            return cert;
          }
        }
      }
    } catch (XMLSecurityException ex) {
      throw new KeyResolverException("empty", ex);
    }

    return null;
  }

  /**
   * Method engineResolveSecretKey
   *
   * @param element
   * @param baseURI
   * @param storage
   * @inheritDoc
   */
  public javax.crypto.SecretKey engineLookupAndResolveSecretKey(
    Element element, String baseURI, StorageResolver storage
  ) {
    return null;
  }
}
