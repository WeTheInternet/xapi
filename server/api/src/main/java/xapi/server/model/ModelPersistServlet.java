/**
 *
 */
package xapi.server.model;

import xapi.io.X_IO;
import xapi.jre.model.ModelServiceJre;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelModule;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQueryResult;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.service.ModelService;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.time.X_Time;
import xapi.util.X_Properties;
import xapi.util.X_String;
import xapi.util.api.Pointer;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelPersistServlet extends HttpServlet {

  private static final long serialVersionUID = -8873779568305155795L;
  protected ServletContext context;

  /**
   * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
   */
  @Override
  public void init(final ServletConfig config) throws ServletException {
    context = config.getServletContext();
    final String modules = X_Properties.getProperty("gwt.modules");
    if (modules != null) {
      for (String module : modules.split("\\s+")) {
        module = module.trim();
        if (!module.isEmpty()) {
          final String moduleName = module;
          ModelModuleLoader.get().preloadModule(context, moduleName);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    final String moduleName = req.getHeader("X-Gwt-Module");
    final ModelModule module = ModelModuleLoader.get().loadModule(context, moduleName);
    final RemovalHandler handler = ModelServiceJre.registerModule(module);
    try {
      final String encoding = X_String.firstNotEmpty(req.getCharacterEncoding(), "UTF-8");
      final String[] keySections = URLDecoder.decode(req.getRequestURI(), encoding).split("/");
      final String requestType = keySections[keySections.length-4];
      final PrimitiveSerializer primitives = X_Model.getService().primitiveSerializer();
      String namespace = keySections[keySections.length-3];
      namespace = primitives.deserializeString(new StringCharIterator(namespace));
      String kind = keySections[keySections.length-2];
      final CharIterator ident = new StringCharIterator(keySections[keySections.length-1]);
      if (requestType.equals("query")) {
        kind = primitives.deserializeString(new StringCharIterator(kind));
        runQuery(resp, module, primitives, namespace, kind, ident, encoding);
        return;
      }
      final String type = req.getHeader("X-Model-Type");
      final ModelManifest manifest = module.getManifest(type);
      final int keyType = primitives.deserializeInt(ident);
      final String id = ident.consumeAll().toString();

      final ModelKey key = X_Model.newKey(namespace, kind, id).setKeyType(keyType);
      final Pointer<Boolean> wait = new Pointer<>(true);
      final Class<Model> modelType = (Class<Model>) manifest.getModelType();
      X_Model.load(modelType, key,
          new SuccessHandler<Model>() {
        @Override
        public void onSuccess(final Model m) {
          final String serialized = X_Model.serialize(manifest, m);
          try {
            X_IO.drain(resp.getOutputStream(), X_IO.toStream(serialized, encoding));
            wait.set(false);
          } catch (final Exception e) {
            X_Log.error(getClass(), "Error saving model",e);
          }
        }
      });
      blockUntilTrue(wait, key);
    } finally {
      handler.remove();
    }
  }

  private void blockUntilTrue(final Pointer<Boolean> wait, final Object debugInfo) {
    final long deadline = System.currentTimeMillis() + 5000;
    while (wait.get()) {
      X_Time.trySleep(30, 0);
      if (X_Time.isPast(deadline)) {
        X_Log.error(getClass(), "Timeout while loading model(s)",debugInfo);
        return;
      }
    }
  }

  protected void runQuery(final HttpServletResponse resp, final ModelModule module, final PrimitiveSerializer primitives, final String namespace, final String kind,
      final CharIterator queryString, final String encoding) {
    final ModelService service = X_Model.getService();
    final ModelQuery query = ModelQuery.deserialize(service, primitives, queryString);
    final Pointer<Boolean> wait = new Pointer<>(true);
    final SuccessHandler callback = new SuccessHandler<ModelQueryResult>() {
      @Override
      public void onSuccess(final ModelQueryResult t) {
        try {
          final String serialized = t.serialize(service, primitives);
          X_IO.drain(resp.getOutputStream(), X_IO.toStream(serialized, encoding));
          wait.set(false);
        } catch (final Exception e) {
          X_Log.error(getClass(), "Error saving model",e);
        }
      }
    };

    if ("".equals(kind)) {
      service.query(query, callback);
    } else {
      final Class<? extends Model> modelClass = service.typeToClass(kind);
      service.query(modelClass, query, callback);
    }

    blockUntilTrue(wait, query);
  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    final String asString = X_IO.toStringUtf8(req.getInputStream());
    final String type = req.getHeader("X-Model-Type");
    final String moduleName = req.getHeader("X-Gwt-Module");
    final ModelModule module = ModelModuleLoader.get().loadModule(context, moduleName);
    final RemovalHandler handler = ModelServiceJre.registerModule(module);
    try {
      final ModelManifest manifest = module.getManifest(type);
      final Model model;
      try {
        model = X_Model.deserialize(manifest, asString);
      } catch (final Throwable e) {
        String moduleText, manifestText;
        try {
          moduleText = ModelModule.serialize(module);
        } catch (final Throwable e1) {
          moduleText = String.valueOf(module);
        }
        try {
          manifestText = ModelManifest.serialize(manifest);
        } catch (final Throwable e1) {
          manifestText = String.valueOf(manifest);
        }
        X_Log.error(getClass(), "Error trying to deserialize model; ",e,"source: ","|"+asString+"|"
            , "\nManifest: ","|"+manifestText+"|"
            , "\nModule: ","|"+moduleText+"|");
        throw new ServletException(e);
      }
      final Pointer<Boolean> wait = new Pointer<>(true);
      X_Model.persist(model,
          new SuccessHandler<Model>() {
        @Override
        public void onSuccess(final Model m) {
          final String serialized = X_Model.serialize(manifest, m);
          try {
            X_IO.drain(resp.getOutputStream(), X_IO.toStreamUtf8(serialized));
            wait.set(false);
          } catch (final Exception e) {
            X_Log.error(getClass(), "Error saving model",e);
          }
        }
      });

      final long deadline = System.currentTimeMillis() + 5000;
      while (wait.get()) {
        X_Time.trySleep(30, 0);
        if (X_Time.isPast(deadline)) {
          X_Log.error(ModelPersistServlet.class, "Timeout while saving model",model);
          return;
        }
      }
    } finally {
      handler.remove();
    }
  }
}
