/**
 *
 */
package xapi.server.model;

import java.io.InputStream;

import javax.inject.Provider;
import javax.servlet.ServletContext;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.io.X_IO;
import xapi.model.X_Model;
import xapi.model.api.ModelModule;
import xapi.model.service.ModelService;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.time.X_Time;
import xapi.util.X_Debug;
import xapi.util.impl.LazyProvider;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelModuleLoader {

  private static class ModuleLoader extends LazyProvider<ModelModule> implements Runnable {

    public ModuleLoader(final ServletContext context, final String moduleName) {
      super(new Provider<ModelModule>() {
        @Override
        public ModelModule get() {
          try (
              InputStream stream = context.getResourceAsStream("/WEB-INF/deploy/"+moduleName+"/XapiModelLinker/xapi.rpc");
          ){
            final CharIterator policy = new StringCharIterator(X_IO.toStringUtf8(stream));
            final ModelService modelService = X_Model.getService();
            return  ModelModule.deserialize(policy, modelService.primitiveSerializer());
          } catch (final Throwable e) {
            throw X_Debug.rethrow(e);
          }
        }
      });
    }

    @Override
    public void run() {
      get();
    }

  }
  private static final ModelModuleLoader LOADER = new ModelModuleLoader();

  private final StringTo<ModuleLoader> loaders;

  public static ModelModuleLoader get() {
    return LOADER;
  }

  private ModelModuleLoader() {
    loaders = X_Collect.newStringMap(ModuleLoader.class);
  }

  public void preloadModule(final ServletContext context, final String moduleName) {
    if (!loaders.containsKey(moduleName)) {
      X_Time.runLater(getOrMakeLoader(context, moduleName));
    }
  }

  public ModelModule loadModule(final ServletContext context, final String moduleName) {
    return getOrMakeLoader(context, moduleName).get();
  }

  private synchronized ModuleLoader getOrMakeLoader(final ServletContext context, final String moduleName) {
    if (loaders.containsKey(moduleName)) {
      return loaders.get(moduleName);
    }
    final ModuleLoader loader = new ModuleLoader(context, moduleName);
    loaders.put(moduleName, loader);
    return loader;
  }
}
