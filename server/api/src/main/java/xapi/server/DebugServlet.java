package xapi.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import xapi.log.X_Log;
import xapi.server.annotation.XapiServlet;

@XapiServlet(prefix="debug")
public class DebugServlet extends HttpServlet{

  private static final long serialVersionUID = 4666969975652260150L;

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    X_Log.trace("doDelete", req, resp);
    super.doDelete(req, resp);
  }
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    X_Log.trace("doGet", req.getRemoteAddr(), resp);
    resp.addHeader("meow", "wow");
    resp.getWriter().println("GET");
    resp.setStatus(HttpServletResponse.SC_OK);
  }
  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    X_Log.trace("doHead", req, resp);
    super.doHead(req, resp);
  }

  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    X_Log.trace("doOptions", req, resp);
    super.doOptions(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    X_Log.trace("doPost", req, resp);
    super.doPost(req, resp);
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    X_Log.trace("doPut", req, resp);
    super.doPut(req, resp);
  }

  @Override
  protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    X_Log.trace("doTrace", req, resp);
    super.doTrace(req, resp);
  }



}
