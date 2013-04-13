package xapi.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import xapi.model.user.ModelUser;
import xapi.server.auth.AuthService;

public class AuthedServlet extends HttpServlet{

  private static final long serialVersionUID = -2466990550904790207L;
  public AuthedServlet() {
  }

  protected AuthService<HttpServletRequest> getAuthService() {
    // Allow subclasses to override our static service
    return X_Server.getAuthService();
  }

  protected String getUserId(HttpServletRequest req) {
    HttpSession session = req.getSession(false);
    if (session == null)
      return AuthService.NOT_LOGGED_IN;
    return getAuthService().getUuid(req);
  }

  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    String ident = getUserId(req);
    if (ident == AuthService.NOT_LOGGED_IN) {
      // do unauthorized head
    } else {
      if (isAllowed(ident, req)) {
        super.doHead(req, resp);
      } else {
        resp.setContentLength(0);
        resp.getOutputStream().close();
      }
    }
  }

  protected boolean isAllowed(String ident, HttpServletRequest req) {
    return true; // Subclasses should provide this.
  }
  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    super.doDelete(req, resp);
  }
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doGet(req, resp);
  }
  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    super.doOptions(req, resp);
  }
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    super.doPost(req, resp);
  }
  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doPut(req, resp);
  }
  @Override
  protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    super.doTrace(req, resp);
  }

}
