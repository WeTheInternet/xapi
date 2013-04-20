package xapi.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet class designed to expose a generic data type as a web service.
 *
 *  Given a datatype named "Bean",
 *  the default url mappings will be as follows:
 *
 *  /xapi/Bean/new/id -> new bean (never null, returns existing or new)
 *  /xapi/Bean/get/id -> get bean (null if id doesn't exist)
 *  /xapi/Bean/has/id -> check bean != null
 *  /xapi/Bean/del/id -> delete bean
 *  /xapi/Bean/set/id/data -> set bean (where data is your serialized, url encoded object)
 *  /xapi/Bean/patch/id/data -> update bean
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class ObjectServlet <T> extends AuthedServlet{

  private static final long serialVersionUID = 7384312327333846222L;

  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    super.doHead(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doGet(req, resp);
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    super.doDelete(req, resp);
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
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
    IOException {
    super.doOptions(req, resp);
  }



}
