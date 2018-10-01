/**
 *
 */
package xapi.server.model;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.io.X_IO;
import xapi.model.X_Model;
import xapi.model.api.ModelModule;
import xapi.model.service.ModelService;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.time.X_Time;
import xapi.util.X_Debug;
import xapi.util.impl.LazyProvider;

import java.io.InputStream;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelModuleLoader {

  private static class ModuleLoader extends LazyProvider<ModelModule> implements Runnable {

    public ModuleLoader(final In1Out1<String, InputStream> manifestFinder, final String moduleName) {
      super(() -> {
          try (
              InputStream stream = manifestFinder.io(moduleName)
          ){
            final CharIterator policy = new StringCharIterator(X_IO.toStringUtf8(512, stream));
            final ModelService modelService = X_Model.getService();
            return  ModelModule.deserialize(policy, modelService.primitiveSerializer());
          } catch (final Throwable e) {
            throw X_Debug.rethrow(e);
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

  public void preloadModule(final In1Out1Unsafe<String, InputStream> manifestFinder, final String moduleName) {
    if (!loaders.containsKey(moduleName)) {
      X_Time.runLater(getOrMakeLoader(manifestFinder, moduleName));
    }
  }

  public ModelModule loadModule(final In1Out1<String, InputStream> manifestFinder, final String moduleName) {
    return getOrMakeLoader(manifestFinder, moduleName).get();
  }

  private synchronized ModuleLoader getOrMakeLoader(final In1Out1<String, InputStream> manifestFinder, final String moduleName) {
    if (loaders.containsKey(moduleName)) {
      return loaders.get(moduleName);
    }
    final ModuleLoader loader = new ModuleLoader(manifestFinder, moduleName);
    loaders.put(moduleName, loader);
    return loader;
  }
}
