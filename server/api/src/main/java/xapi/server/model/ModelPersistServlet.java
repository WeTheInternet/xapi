/**
 *
 */
package xapi.server.model;

import xapi.fu.In1Out1;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.service.ModelService;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.util.X_Properties;
import xapi.string.X_String;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelPersistServlet extends HttpServlet implements ModelCrudMixin {

  private static final long serialVersionUID = -8873779568305155795L;
  protected ServletContext context;

  /**
   * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
   */
  @Override
  public void init(final ServletConfig config) {
    context = config.getServletContext();
    final String modules = X_Properties.getProperty("gwt.modules");
    if (modules != null) {
      for (String module : modules.split("\\s+")) {
        module = module.trim();
        if (!module.isEmpty()) {
          final String moduleName = module;
          ModelModuleLoader.get().preloadModule(asFinder(context), moduleName);
        }
      }
    }
  }

  private static In1Out1Unsafe<String,InputStream> asFinder(ServletContext context) {
    return moduleName -> context.getResourceAsStream("/WEB-INF/deploy/"+moduleName+"/XapiModelLinker/xapi.rpc");
  }

  @Override
  public In1Out1<String, InputStream> findManifest(String moduleName) {
    return asFinder(context);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

    final ModelService service = getService();
    final String moduleName = req.getHeader("X-Gwt-Module");
    final String encoding = X_String.firstNotEmpty(req.getCharacterEncoding(), "UTF-8");
    final String uri = URLDecoder.decode(req.getRequestURI(), encoding);
    final String[] keySections = uri.split("/");
    final String requestType = keySections[keySections.length-4];
    if ("query".equals(requestType)) {
      String kind = keySections[keySections.length-2];
      final PrimitiveSerializer primitives = service.primitiveSerializer();
      kind = primitives.deserializeString(new StringCharIterator(kind));
      final CharIterator ident = new StringCharIterator(keySections[keySections.length-1]);

      performQuery(service, primitives, kind, ident, (query, result)->{
        final String serialized = result.serialize(service, primitives);
        X_IO.drain(resp.getOutputStream(), X_IO.toStream(serialized, encoding));
      }, failure->{
        X_Log.error(ModelPersistServlet.class, "Failed to query", uri, failure);
        resp.sendError(500, "Unable to query " + uri + ": " + failure);
      });

      return;
    }
    final String type = req.getHeader("X-Model-Type");

    performGet(moduleName, uri, type, (manifest, model) -> {
      final String serialized = X_Model.serialize(manifest, model);
      final OutputStream out = resp.getOutputStream();
      X_IO.drain(out, X_IO.toStream(serialized, encoding));
    }, failure -> {
      X_Log.error(ModelPersistServlet.class, "Failed to read", uri, failure);
      resp.sendError(500, "Unable to read " + uri + ": " + failure);
    });

  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    final String encoding = X_String.firstNotEmpty(req.getCharacterEncoding(), "UTF-8");
    Out1<String> loader = In2Out1.unsafe(X_IO::toStringEncoded).supply1(req.getInputStream()).supply(encoding).lazy();
    final String type = req.getHeader("X-Model-Type");
    final String moduleName = req.getHeader("X-Gwt-Module");

    ModelCrudMixin.super.performPost(moduleName, type, loader, (manifest, model)->{
      final String serialized = X_Model.serialize(manifest, model);
      X_IO.drain(resp.getOutputStream(), X_IO.toStreamUtf8(serialized));
    }, failure -> {
      X_Log.error(ModelPersistServlet.class, "Failed to save", loader.out1(), failure);
      resp.sendError(500, "Unable to save " + loader.out1() + failure);
    });
  }
}
