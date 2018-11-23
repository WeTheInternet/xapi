package xapi.server;

import xapi.fu.In1.In1Unsafe;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.server.auth.AuthService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

public final class X_Server {

  private X_Server() {}

  /**
   * Using provider for X_Inject will allow us to use a threadlocal provider.
   */
  @SuppressWarnings("unchecked") // just adding generics to class literals
  private static final Out1<AuthService<HttpServletRequest>> authProvider
    = X_Inject.singletonLazy(AuthService.class);

  public static AuthService<HttpServletRequest> getAuthService() {
    return authProvider.out1();
  }

  /**
   * To be gain protection against race conditions
   * (a worthy divine protection for any program),
   * you should bind to this port number within this callback.
   *
   * If you store the port and use it later,
   * someone else may have received it.
   */
  public static int usePort(In1Unsafe<Integer> onlyAvailableSafelyInCallback) {
    int port;
    synchronized (authProvider) {
      port = getUnusedPort();
      onlyAvailableSafelyInCallback.in(port);
    }
    return port;
  }

  public static int getUnusedPort() {
    try (
      final ServerSocket socket = new ServerSocket(0);
    ) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
