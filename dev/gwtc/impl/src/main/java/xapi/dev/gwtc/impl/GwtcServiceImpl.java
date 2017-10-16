package xapi.dev.gwtc.impl;

import xapi.annotation.compile.Dependency;
import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.ExtensibleClassLoader;
import xapi.dev.gwtc.api.AnnotatedDependency;
import xapi.dev.gwtc.api.GwtcJobManager;
import xapi.dev.gwtc.api.GwtcProjectGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.file.X_File;
import xapi.fu.*;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.fu.iterate.EmptyIterator;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.ServerRecompiler;
import xapi.io.api.SimpleLineReader;
import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.reflect.X_Reflect;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellSession;
import xapi.util.X_Debug;
import xapi.util.X_Properties;
import xapi.util.X_Util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xapi.fu.iterate.SingletonIterator.singleItem;

import com.google.gwt.dev.GwtCompiler;

@Gwtc(propertiesLaunch=@GwtcProperties)
@InstanceDefault(implFor=GwtcService.class)
public class GwtcServiceImpl extends GwtcServiceAbstract {

  private String binDir;

  Pattern SPECIAL_DIRS = Pattern.compile(
      "(" + Pattern.quote(Dependency.DIR_BIN) + ")|" +
          "(" + Pattern.quote(Dependency.DIR_GEN) + ")|" +
          "(" + Pattern.quote(Dependency.DIR_TEMP) + ")|"
  );

  private final StringTo<GwtcJobStateImpl> runningCompiles;
  private final StringTo<GwtcJobManager> jobs;

  public GwtcServiceImpl() {
    this(Thread.currentThread().getContextClassLoader());
  }

  public GwtcServiceImpl(ClassLoader resourceLoader) {
    super(resourceLoader);
    runningCompiles = X_Collect.newStringMap(GwtcJobStateImpl.class);
    jobs = X_Collect.newStringMap(GwtcJobManager.class);
  }



  @Override
  public void doCompile(
      GwtManifest manifest, long timeout, TimeUnit unit, In2<CompiledDirectory, Throwable> callback
  ) {

    final GwtcJobManager manager = jobs.getOrCreateFrom(manifest.getModuleName(), GwtcJobManagerImpl::new, this);
    manager.compileIfNecessary(manifest, callback);

  }

  @Override
  public void compile(GwtManifest manifest, long timeout, TimeUnit unit, In2<Integer, Throwable> callback) {
    final In2Out1<Long, TimeUnit, Integer> compilation = doCompile(manifest);
    Lazy<Integer> doCompile = Lazy.deferred1(()->{
      boolean called = false;
      try {
        final Integer result = compilation.io(timeout, unit);
        called = true;
        callback.in(result, null);
        return result;
      } catch (Throwable t) {
        if (!called) {
          callback.in(-1, t);
        }
        throw t;
      }

    });
    X_Process.runWhenReady(doCompile, result->{
      callback.in(result, null);
    });
  }

  @Override
  public GwtcJobStateImpl recompile(
      GwtManifest manifest,
      Long millisToWait,
      In2<ServerRecompiler, Throwable> callback
  ) {

    manifest.setRecompile(true);
    String id = manifest.getModuleName();
    GwtcJobStateImpl job = runningCompiles.getOrCreate(id, module ->
      new GwtcJobStateImpl(manifest, manifest.getModuleName(), manifest.getModuleShortName(), GwtcServiceImpl.this));

    job.startCompile(callback);

    return job;
  }

  @Override
  public URLClassLoader resolveClasspath(GwtManifest manifest, String compileHome) {
    X_Log.info(GwtcServiceImpl.class, "Starting gwt compile", manifest.getModuleName());
    X_Log.debug(getClass(), manifest);

    final String[] classpath = manifest.toClasspathFullCompile(compileHome);
    X_Log.debug(getClass(), "Requested Classpath\n", classpath);

    In1<Integer> cleanup = prepareCleanup(manifest);

    URL[] urls = fromClasspath(classpath);
    URLClassLoader loader = manifestBackedClassloader(urls, manifest);
    return loader;
  }

  @Override
  public In2Unsafe<Integer, TimeUnit> startTask(Runnable task, URLClassLoader loader) {
    Thread t = new Thread(task);
    try {
      loader.loadClass("com.google.gwt.core.ext.Linker");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    final DoUnsafe canceler = ()->{
      t.interrupt();
      t.join();
    };
    t.setUncaughtExceptionHandler((thread, error)->{
      if (X_Util.unwrap(error) instanceof InterruptedException) {
        // Someone was trying to cancel the job; ignore
      } else {
        // Not sure this is a wise thing to do... the thread should already be ending due to this handler running.
        t.interrupt();
      }
    });
    t.setContextClassLoader(loader);
    t.start();
    final In2Unsafe<Integer, TimeUnit> blocker = (sec, unit) -> {
      try {
        t.join(unit.toMillis(sec));
      } catch (InterruptedException e) {
        throw X_Debug.rethrow(e);
      }
    };

    return blocker;
  }

  private URL[] fromClasspath(String[] classpath) {
    final URL[] urls = new URL[classpath.length];
    for (int i = 0; i < classpath.length; i++) {
      try {
        urls[i] = new URL("file:" + classpath[i]);
      } catch (MalformedURLException e) {
        X_Log.error(getClass(), "Bad url: ", classpath[i]);
        throw X_Util.rethrow(e);
      }
    }
    X_Log.trace(getClass(), "Classpath urls: ", urls);
    return urls;
  }

  private Lazy<In1<Do>> garbageCollector = Lazy.<In1<Do>>deferred1(()->{
    ChainBuilder<Do> ondone = Chain.startChain();

    Runtime.getRuntime().addShutdownHook(
        new Thread(()->ondone.forEach(Do::done))
    );

    return ondone::add;
  });

  @Override
  public In1<Integer> prepareCleanup(GwtManifest manifest) {

    Do delete = ()->{
      X_File.deepDelete(manifest.getWarDir());
      X_File.deepDelete(manifest.getWorkDir());
    };

    switch (manifest.getCleanupMode()) {
      case DELETE_ON_SUCCESSFUL_EXIT:
        return code -> {
          if (code == 0) {
            garbageCollector.out1().in(delete);
          }
        };
      case DELETE_ON_EXIT:
        garbageCollector.out1().in(delete);
        return In1.ignored();
      case DELETE_ON_SUCCESS:
        return code->{
          if (code != 0) {
            delete.done();
          }
        };
      case ALWAYS_DELETE:
        return delete.ignores1();
      case NEVER_DELETE:
        return In1.ignored();
      default:
        throw new IllegalArgumentException("Unhandled case " + manifest.getCleanupMode());
    }
  }

  protected In2Out1<Long, TimeUnit, Integer> doCompile(GwtManifest manifest) {
    String gwtHome = generateCompile(manifest);
    // Logging
    X_Log.info(GwtcServiceImpl.class, "Starting gwt compile", manifest.getModuleName());
    X_Log.trace(manifest);
    manifest.setRecompile(false);
    final String[] programArgs = manifest.toProgramArgArray();
    final String[] jvmArgs = manifest.toJvmArgArray();
    X_Log.trace("Args: java ", jvmArgs, programArgs);
    final String[] classpath = manifest.toClasspathFullCompile(gwtHome);
    X_Log.debug("Requested Classpath\n", classpath);

    In1<Integer> cleanup = prepareCleanup(manifest);

    if (manifest.isUseCurrentJvm()) {
      // this sanity check is expensive...
      assert runtimeContainsClasspath(manifest.getGenDir(), classpath);
      // TODO launch a worker thread for us to block on...
      // Preferably using a brand new URLClassLoader
      URL[] urls = fromClasspath(classpath);

      URLClassLoader loader = manifestBackedClassloader(urls, manifest);

      Mutable<Integer> code = new Mutable<>();
      Thread t = new Thread(()->{
        final boolean success = GwtCompiler.doCompile(programArgs);
        code.in(success ? 0 : 1);
        if (success) {
          manifest.computeCompileDirectory();
        }
        cleanup.in(code.out1());
      });
      t.setContextClassLoader(loader);
      t.start();
      In2Unsafe<Long, TimeUnit> blocker= (sec, unit) ->{
        try {
          t.join(unit.toMillis(sec));
        } catch (InterruptedException e) {
          throw X_Debug.rethrow(e);
        }
      };
      return blocker.supply1AfterRead(code);
    } else {
      ShellSession controller
          = X_Shell.launchJava(GwtCompiler.class,
          classpath,
          jvmArgs,
          programArgs
      );
      controller.stdErr(new SimpleLineReader() {
        @Override
        public void onLine(String errLog) {
          warn("[ERROR] "+errLog);
        }
      });
      controller.stdOut(new SimpleLineReader() {
        @Override
        public void onLine(String logLine) {
          info(logLine);
        }
      });
      controller.onFinished(session->{
        int result = session.join();
        if (result == 0) {
          // successful compile; update compile dir
          manifest.computeCompileDirectory();
        }
        cleanup.in(result);
      });
      return controller::block;
    }
  }

  private URLClassLoader manifestBackedClassloader(URL[] urls, GwtManifest manifest) {
//     return new URLClassLoader(urls, superLoader(manifest)) {
//      @Override
//      public URL getResource(String name) {
//        URL url = super.getResource(name);
//        if (url == null) {
//          // Egregious hack; when running in the same jvm, the classloader is not able to use
//          // the bootstrap classpath to load our newly generated resources out of our generated folder,
//          // so, we resort to manually looking up any failed resource loads here, in the classloader.
//          try {
//            File f = new File(inGeneratedDirectory(manifest, name));
//            if (f.exists()) {
//              return f.toURI().toURL();
//            }
//          } catch (IOException e) {
//            X_Log.warn(GwtcServiceImpl.class, "Failure to load resources for " + name);
//          }
//        }
//        return url;
//      }
//    };
    final ExtensibleClassLoader loader = new ExtensibleClassLoader(urls, superLoader(manifest));
    if (manifest.isIsolateClassLoader()) {
      loader.setCheckMeFirst(true);
    }
    return loader.addResourceFinder(name-> {
          try {
            File f = new File(inGeneratedDirectory(manifest, name));
            if (f.exists()) {
              return f.toURI().toURL();
            }
          } catch (IOException e) {
            X_Log.warn(GwtcServiceImpl.class, "Failure to load resources for " + name);
          }
          return null;
        });
  }

  protected ClassLoader superLoader(GwtManifest manifest) {
    if (manifest.isUseCurrentJvm()) {
      if (manifest.isIsolateClassLoader()) {
          return null;
      } else {
          X_Log.trace(GwtcServiceImpl.class, "Using context classloader for gwt compile", manifest);
          return Thread.currentThread().getContextClassLoader();
      }
    }
    // When not set to reuse jvm, we are currently going to allow the current classloader to be used,
    // but we should likely update this to return null instead
    return Thread.currentThread().getContextClassLoader();
  }

  @Override
  public int compile(GwtManifest manifest) {
    return doCompile(manifest)
      .io(600L, TimeUnit.SECONDS);
  }

  private boolean runtimeContainsClasspath(String genDir, String[] classpath) {
    final URL[] runtimeCp = ((URLClassLoader) getClass().getClassLoader()).getURLs();
    X_Log.debug("Runtime cp", runtimeCp);
    searching:
    for (URL url : runtimeCp) {
      for (String s : classpath) {
        if (genDir.equals(s)) {
          continue searching;
        }
        final String external = url.toExternalForm();
        if (external.endsWith(s) || external.contains("jre/lib")) {
          continue searching;
        }
      }
      return false;
    }

    return true;
  }

  @Override
  public String generateCompile(GwtManifest manifest) {
    final GwtcProjectGenerator project = getProject(manifest.getModuleName(), null);
    return project.generateCompile(manifest);
  }

  @Override
  public String inGeneratedDirectory(GwtManifest manifest, String filename) {
    String genFolder = manifest.getGenDir();
    try {
      return new File(genFolder, filename).getCanonicalPath();
    } catch (IOException e) {
      throw X_Debug.rethrow(e);
    }
  }

  private Out1<MappedIterable<String>> replaceLocationVars(GwtManifest manifest, String val, AnnotatedElement source) {
    return ()->{
      String value = val;
      Matcher matcher = SPECIAL_DIRS.matcher(value);
      List<Replacement> replacements = new ArrayList<>();
      if (matcher.matches()) {
        final GwtcProjectGenerator project = getProject(manifest.getModuleName(), null);
        String type = matcher.group();
        switch (type) {
          case Dependency.DIR_BIN:
            if (binDir == null) {
              binDir = X_Properties.getProperty("java.class.path", "bin");
            }
            replacements.add(new Replacement(matcher.start(), matcher.end(), binDir));
            break;
          case Dependency.DIR_GEN:
            replacements.add(new Replacement(matcher.start(), matcher.end(), manifest.getGenDir()));
            break;
          case Dependency.DIR_TEMP:
            replacements.add(new Replacement(matcher.start(), matcher.end(), project.getTempDir().getAbsolutePath()));
            break;
          case Dependency.DIR_SOURCE:
            replacements.add(new Replacement(matcher.start(), matcher.end(), project.getSourceDir().getAbsolutePath()));
            break;
        }
      }
      for (int i = replacements.size(); i-->0;) {
        Replacement replacement = replacements.get(i);
        value = value.substring(0, replacement.start)
            + replacement.newValue+value.substring(replacement.end);
      }
      if (!new File(value).isAbsolute()) {
        value = manifest.getRelativeRoot() + File.separator + value;
      }
      return singleItem(value);
    };
  }

  @Override
  public Out1<? extends Iterable<String>> resolveDependency(GwtManifest manifest, AnnotatedDependency dep) {
    final Dependency dependency = dep.getDependency();
    switch (dependency.dependencyType()) {
      case ABSOLUTE:
        return replaceLocationVars(manifest, dependency.value(), dep.getSource());
      case RELATIVE:
        return replaceLocationVars(manifest, dependency.value(), dep.getSource())
            .map(itr->
                itr.map(item->relativize(item, manifest, dep))
                    .flatten(MappedIterable::mapped)
            );
      case MAVEN:
        if (!dependency.loadChildren()) {
          // If we don't want to load children, we can just grab the jar from local cache...
          String m2Home = X_Properties.getProperty("maven.home");
          if (m2Home == null) {
            m2Home = X_Properties.getProperty("user.home");
            if (m2Home != null) {
              File f = new File(m2Home,".m2/repository");
              if (f.exists()) {
                m2Home = f.getAbsolutePath();
              }
            }
            if (m2Home == null) {
              X_Log.warn(getClass(), "Cannot resolve maven dependency",dependency
                  ,"as M2_HOME environment variable is not set");
            }
          }
          // TODO: defer to maven service...
          if (m2Home != null) {
            File artifact = new File(m2Home, dependency.groupId().replace('.', File.separatorChar));
            artifact = new File(artifact, dependency.value());
            artifact = new File(artifact, dependency.version());
            if (dependency.classifier().length() > 0) {
              artifact = new File(artifact, dependency.value()+"-"+dependency.version()+"-"+dependency.classifier()+".jar");
            } else {
              artifact = new File(artifact, dependency.value()+"-"+dependency.version()+".jar");
            }
            if (artifact.exists()) {
              X_Log.trace(getClass(), "Using maven artifact ",artifact.getAbsolutePath());
              return Immutable.immutable1(singleItem(artifact.getAbsolutePath()));
            } else {
              X_Log.warn(getClass(),"could not find maven dependency",dependency,"in",artifact);
            }
          }
        }
        return downloadFromMaven(dependency);
      default:
        throw new IllegalArgumentException("Unhandled case " + dependency.dependencyType());
    }
  }

  private Iterable<String> relativize(String item, GwtManifest manifest, AnnotatedDependency dep) {
    if (Files.exists(Paths.get(item))) {
      return singleItem(item);
    }
    final String fileLoc;
    if (dep.getSource() instanceof Class<?>) {
      fileLoc = X_Reflect.getFileLoc((Class<?>) dep.getSource()).replaceAll("/target/(test-)?classes", "");
    } else if (dep.getSource() instanceof Package) {
      String pkgInfo = ((Package) dep.getSource()).getName().replace('.', '/') + "/package-info.class";
      final URL res = Thread.currentThread().getContextClassLoader().getResource(pkgInfo);
      fileLoc = res.toExternalForm().replace("file:", "").replaceAll("/target/(test-)?classes", "")
          .replace(pkgInfo, "");
    } else {
      // use the manifest for relativization of resources
      fileLoc = new File(".").getAbsolutePath();
    }
    Path loc = Paths.get(fileLoc);
    if (item.isEmpty()) {
      // assume the user wants src/main/java and src/main/resources
      final ChainBuilder<String> chain = Chain.<String>startChain()
          .add(loc.resolve("src/main/java").toString())
          .add(loc.resolve("src/main/resources").toString());
      final Path genDir = loc.resolve("src/main/gen");
      if (Files.exists(genDir)) {
        chain.add(genDir.toString());
      }
      return chain.build();
    } else {
      if (!loc.isAbsolute()) {
        loc = Paths.get(manifest.getRelativeRoot()).resolve(loc);
      }
      return singleItem(loc.resolve(item).toString());
    }
  }

  /**
   * This method exists as a stub that can be implemented by a module with a dependency on our maven runner.
   *
   * Because the maven dependency list is long and conflicting, we avoid directly referencing it here.
   */
  protected Out1<Iterable<String>> downloadFromMaven(Dependency dependency) {
    return Immutable.immutable1(EmptyIterator.none());
  }

  @Override
  public GwtcProperties getDefaultLaunchProperties() {
    Gwtc myGwtc = getClass().getAnnotation(Gwtc.class);
    if (myGwtc == null) {
      myGwtc = GwtcServiceImpl.class.getAnnotation(Gwtc.class);
    }
    return myGwtc.propertiesLaunch()[0];
  }

}
