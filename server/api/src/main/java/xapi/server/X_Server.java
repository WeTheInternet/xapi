package xapi.server;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import xapi.inject.X_Inject;
import xapi.server.auth.AuthService;

public final class X_Server {

  private X_Server() {}

  /**
   * Using provider for X_Inject will allow us to use a threadlocal provider.
   */
  @SuppressWarnings("unchecked") // just adding generics to class literals
  private static final Provider<AuthService<HttpServletRequest>> authProvider
    = X_Inject.singletonLazy(Class.class.cast(AuthService.class));

  public static AuthService<HttpServletRequest> getAuthService() {
    return authProvider.get();
  }


}
