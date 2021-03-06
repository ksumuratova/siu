/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2014, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gws.stubs;

import ru.codeinside.gws.api.ClientLog;
import ru.codeinside.gws.api.ClientRequest;
import ru.codeinside.gws.api.ClientResponse;
import ru.codeinside.gws.api.LogService;
import ru.codeinside.gws.api.ServerLog;
import ru.codeinside.gws.api.ServerRequest;
import ru.codeinside.gws.api.ServerResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DebugLogService implements LogService {

  boolean serverLogEnabled = true;
  Set<String> enabledServers = new HashSet<String>();
  private boolean logErrors;
  private String status;

  @Override
  public ClientLog createClientLog(long bid, String componentName, String processInstanceId,
                                   boolean isLogEnabled, boolean logErrors, String status, Set<String> remote) {
    return new DebugClientLog(bid, componentName, processInstanceId);
  }

  @Override
  public ServerLog createServerLog(String componentName, String remoteAddr) {
    return serverLogEnabled ? new DebugServerLog(componentName) : null;
  }

  @Override
  public void setServerLogEnabled(boolean enabled) {
    this.serverLogEnabled = enabled;
  }

  @Override
  public void setServerLogErrorsEnabled(boolean enabled) {
    this.logErrors = enabled;
  }

  @Override
  public void setServerLogStatus(String status) {
    this.status = status;
  }

  @Override
  public boolean isServerLogEnabled() {
    return serverLogEnabled;
  }

  @Override
  public boolean isServerLogErrorsEnabled() {
    return logErrors;
  }

  @Override
  public String getServerLogStatus() {
    return status;
  }

  @Override
  public void setServerLogEnabled(String componentName, boolean enabled) {
    if (enabled) {
      enabledServers.add(componentName);
    } else {
      enabledServers.remove(componentName);
    }
  }

  @Override
  public boolean isServerLogEnabled(String componentName) {
    return enabledServers.contains(componentName);
  }

  @Override
  public String getPathInfo() {
    return null;
  }

  @Override
  public void setIgnoreSet(Set<String> ip) {

  }

  final public static class DebugClientLog implements ClientLog {
    private final Logger logger;

    public DebugClientLog(long bid, String componentName, String processInstanceId) {
      logger = Logger.getLogger("ru.codeinside.gws.api.client." + componentName);
      logger.info("create for bid: " + bid + ", processInstanceId: " + processInstanceId);
    }

    @Override
    public void log(Throwable e) {
      logger.log(Level.INFO, "failure", e);
    }

    @Override
    public OutputStream getHttpOutStream() {
      // TODO: блочный вывод
      return new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          logger.log(Level.INFO, "out: " + ((char) b));
        }
      };
    }

    @Override
    public OutputStream getHttpInStream() {
      // TODO: блочный вывод
      return new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          logger.log(Level.INFO, "in: " + ((char) b));
        }
      };
    }

    @Override
    public void logRequest(ClientRequest request) {
      logger.log(Level.INFO, "request: " + request);
    }

    @Override
    public void logResponse(ClientResponse response) {
      logger.log(Level.INFO, "response: " + response);
    }

    @Override
    public void close() {

    }
  }

  final public static class DebugServerLog implements ServerLog {
    private final Logger logger;

    public DebugServerLog(String componentName) {
      logger = Logger.getLogger("ru.codeinside.gws.api.client." + componentName);
    }

    @Override
    public void log(Throwable e) {
      logger.log(Level.INFO, "filure", e);
    }

    @Override
    public OutputStream getHttpOutStream() {
      return new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          logger.log(Level.INFO, "out: " + ((char) b));
        }
      };
    }

    @Override
    public OutputStream getHttpInStream() {
      return new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          logger.log(Level.INFO, "in: " + ((char) b));
        }
      };
    }

    @Override
    public void logRequest(ServerRequest request) {
      logger.log(Level.INFO, "request: " + request);
    }

    @Override
    public void logResponse(ServerResponse response) {
      logger.log(Level.INFO, "response: " + response);
    }

    @Override
    public void close() {

    }
  }

}
