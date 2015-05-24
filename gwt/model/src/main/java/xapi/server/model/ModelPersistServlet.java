/**
 *
 */
package xapi.server.model;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelModule;
import xapi.model.api.PrimitiveSerializer;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.time.X_Time;
import xapi.util.X_Properties;
import xapi.util.api.Pointer;

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

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    final String type = req.getHeader("X-Model-Type");
    final String moduleName = req.getHeader("X-Gwt-Module");
    final ModelModule module = ModelModuleLoader.get().loadModule(context, moduleName);
    final ModelManifest manifest = module.getManifest(type);
    String encoding = req.getCharacterEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }
    final String[] keySections = URLDecoder.decode(req.getRequestURI(), encoding).split("/");
    final PrimitiveSerializer primitives = X_Model.getService().primitiveSerializer();
    String namespace = keySections[keySections.length-3];
    namespace = primitives.deserializeString(new StringCharIterator(namespace));
    final String kind = keySections[keySections.length-2];
    final CharIterator ident = new StringCharIterator(keySections[keySections.length-1]);
    final int keyType = primitives.deserializeInt(ident);
    final String id = ident.consumeAll().toString();

    final ModelKey key = X_Model.newKey(namespace, kind, id).setKeyType(keyType);
    final Pointer<Boolean> wait = new Pointer<>(true);
    X_Model.load(manifest.getModelType(), key, m->{
      final String serialized = X_Model.serialize(manifest, m);
      try {
        X_IO.drain(resp.getOutputStream(), X_IO.toStreamUtf8(serialized));
        wait.set(false);
      } catch (final Exception e) {
        X_Log.error(getClass(), "Error saving model",e);
      }
    });

    final long deadline = System.currentTimeMillis() + 5000;
    while (wait.get()) {
      X_Time.trySleep(30, 0);
      if (X_Time.isPast(deadline)) {
        X_Log.error(getClass(), "Timeout while loading model",key);
        return;
      }
    }
  }

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    final String asString = X_IO.toStringUtf8(req.getInputStream());
    final String type = req.getHeader("X-Model-Type");
    final String moduleName = req.getHeader("X-Gwt-Module");
    final ModelModule module = ModelModuleLoader.get().loadModule(context, moduleName);
    final ModelManifest manifest = module.getManifest(type);
    final Model model = X_Model.deserialize(manifest, asString);
    final Pointer<Boolean> wait = new Pointer<>(true);
    X_Model.persist(model, m->{
      final String serialized = X_Model.serialize(manifest, m);
      try {
        X_IO.drain(resp.getOutputStream(), X_IO.toStreamUtf8(serialized));
        wait.set(false);
      } catch (final Exception e) {
        X_Log.error(getClass(), "Error saving model",e);
      }
    });

    final long deadline = System.currentTimeMillis() + 5000;
    while (wait.get()) {
      X_Time.trySleep(30, 0);
      if (X_Time.isPast(deadline)) {
        X_Log.error(getClass(), "Timeout while saving model",model);
        return;
      }
    }
  }
}
