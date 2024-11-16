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

  // We want to lock on something that is jvm-wide, not just classloader-wide.
  // So, we'll grab a class loaded by the system classloader, and pull in an object that we can be pretty sure nobody else is locking on
  private static final Object portLock;
  static {
    // make sure to take turns getting the protection domain. It uses null-check-or-create semantics.
    // locking on System.class here is fine, b/c we know it's already loaded, and we'll be in-and-out quickly.
    synchronized (System.class) {
      portLock = System.class.getProtectionDomain();
    }
  }

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
    // lock on our portLock object, which is an unlikely-to-be-synchorized-upon object from the system classloader.
    // This ensures that even if OSGI-style classloader-hell apps which use this code can still ensure atomic access to new port numbers.
    synchronized (portLock) {
      port = getUnusedPort();
      onlyAvailableSafelyInCallback.in(port);
    }
    return port;
  }

  private static int getUnusedPort() {
    try (
      final ServerSocket socket = new ServerSocket(0)
    ) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
