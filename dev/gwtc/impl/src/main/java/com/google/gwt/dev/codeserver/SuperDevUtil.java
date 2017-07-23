package com.google.gwt.dev.codeserver;

import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.MinimalRebuildCacheManager;
import com.google.gwt.dev.javac.UnitCache;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.JJSOptionsImpl;

public class SuperDevUtil {

  private static final ConcurrentHashMap<String, RecompileController> compilers
    = new ConcurrentHashMap<String, RecompileController>();

  public static RecompileController getOrMakeController(TreeLogger logger, GwtManifest manifest) {
    String module = manifest.getModuleName();
    RecompileController ret = compilers.get(module);
    if (ret != null)  {
      ret.cleanup();
      return ret;
    }
    AppSpace app;
    try {
      File tmp = File.createTempFile("recompile", "log").getParentFile();
      tmp.deleteOnExit();
      // We've overridden AppSpace so we can use more deterministic names for our compile folders,
      // but if the user does not order the jars correctly, our overridden method will be missing.
      try {
        // So, to be safe, we'll try with reflection first, and, on failure, use the existing method.
//        System.out.println(SuperDevUtil.class.getClassLoader());
//        System.out.println(Thread.currentThread().getContextClassLoader());
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class<?> cls = cl.loadClass(AppSpace.class.getName());
        Method method = cls.getDeclaredMethod("create", File.class, String.class);
        method.setAccessible(true);
        app = (AppSpace) method.invoke(null, tmp , "Gwtc"+module);
      } catch (Exception e) {
        e.printStackTrace();
        app = AppSpace.create(tmp);
      }

    } catch (IOException e1) {
      throw new Error("Unable to initialize gwt recompiler ",e1);
    }

    final OutboxDir outbox;
    final LauncherDir launcher;
    final Options opts;
    try {
      Set<File> sourcePath = new LinkedHashSet<>();
      List<String> args = new ArrayList<>();
      for (String src : manifest.getSources().forEach()){
        //TODO: sanitize this somehow?
        if (".".equals(src)) {
          src = new File("").getAbsolutePath();
        }
        if (src.startsWith("file:")) {
          src = src.substring(5);
        }
        File dir = new File(src);
        if (!dir.exists()){
          X_Log.error(SuperDevUtil.class,"Gwt source directory "+dir+" does not exist");
        } else {
          X_Log.trace(SuperDevUtil.class, "Adding to source: "+dir);
        }
        sourcePath.add(dir);
      }
      for (String src : manifest.getDependencies()){
        if (".".equals(src)) {
          src = new File("").getAbsolutePath();
        }
        if (src.startsWith("file:")) {
          src = src.substring(5);
        }
        File dir = new File(src);
        if (!dir.isAbsolute()) {
          dir = new File(manifest.getRelativeRoot() + File.separator + dir.getPath());
        }
          if (dir.exists() ) {
              if (dir.isDirectory()) {
                  sourcePath.add(dir);
                  X_Log.trace(SuperDevUtil.class, "Adding to source path (will hot recompile): " + dir);
              }
          } else {
            final String error = "Gwt dependency directory " + dir + " does not exist";
            X_Log.error(SuperDevUtil.class, error);
            if (manifest.isStrict()) {
              throw new IllegalStateException(error);
            }
          }
      }
      final File warDir = new File(manifest.getWarDir());
      if (!warDir.isDirectory()) {
        boolean result = warDir.mkdirs();
        if (!result) {
          X_Log.warn(SuperDevUtil.class, "Unable to create war dir", manifest.getWarDir(), "expect more errors...");
        }
      }
      outbox = OutboxDir.create(new File(manifest.getWarDir()), logger);
      opts = new Options();

      args.add("-allowMissingSrc");

      for (File file : sourcePath) {
        args.add("-src");
        args.add(file.getCanonicalPath());
      }

      opts.addTags();

      if (manifest.getPort() != 0) {
        args.add("-port");
        args.add(Integer.toString(manifest.getPort()));
      }

      if (manifest.getLogLevel() != null) {
        args.add("-logLevel");
        args.add(manifest.getLogLevel().getLabel());
      }

      if (manifest.getObfuscationLevel() != null) {
        args.add("-style");
        args.add(manifest.getObfuscationLevel().name());
      }

      if (manifest.getMethodNameMode() != null) {
        args.add("-XmethodNameDisplayMode");
        args.add(manifest.getMethodNameMode().name());
      }

      if (manifest.isIncremental()) {
        args.add("-incremental");
      } else {
        args.add("-noincremental");
      }

      args.add(module);

      opts.parseArgs(args.toArray(new String[args.size()]));
      launcher = LauncherDir.maybeCreate(opts);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    final File cacheFolder = manifest.getUnitCacheDir() == null ? null : new File(manifest.getUnitCacheDir());
    final JJSOptionsImpl options = new JJSOptionsImpl();
    // The following two options are the only ones used by UnitCacheSingleton;
    // we should put back / in support for these...
//    options.setGenerateJsInteropExports(false);
//    options.getJsInteropExportFilter().add("...");
    final UnitCache cache = UnitCacheSingleton.get(logger, cacheFolder, options);
    final MinimalRebuildCacheManager rebinds = new MinimalRebuildCacheManager(logger, cacheFolder, new HashMap<>());

    JobEventTable eventTable = new JobEventTable();
    JobRunner runner = new JobRunner(eventTable, rebinds);

    Recompiler compiler = new Recompiler(outbox, launcher, module.split("/")[0], opts,
        cache, rebinds
//        sourcePath, "127.0.0.1:"+port,logger
    );
      try{
        RecompileController recompiler = new RecompileController(compiler, runner);
        compilers.put(module, recompiler);
        return recompiler;
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
  }



}
