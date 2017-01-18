package xapi.dev.gwtc.impl;

import org.junit.Test;
import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Dependency.DependencyType;
import xapi.annotation.compile.ResourceBuilder;
import xapi.annotation.inject.InstanceDefault;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.gwtc.impl.GwtcContext.Dep;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.file.X_File;
import xapi.fu.*;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.fu.iterate.EmptyIterator;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.IsRecompiler;
import xapi.gwtc.api.ServerRecompiler;
import xapi.io.api.SimpleLineReader;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellSession;
import xapi.test.junit.JUnit4Runner;
import xapi.test.junit.JUnitUi;
import xapi.time.X_Time;
import xapi.util.X_Debug;
import xapi.util.X_Properties;
import xapi.util.X_Util;

import static xapi.fu.iterate.SingletonIterator.singleItem;
import static xapi.process.X_Process.runFinally;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.GwtCompiler;
import com.google.gwt.dev.codeserver.RecompileController;
import com.google.gwt.dev.codeserver.SuperDevUtil;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.reflect.shared.GwtReflect;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Gwtc(propertiesLaunch=@GwtcProperties)
@InstanceDefault(implFor=GwtcService.class)
public class GwtcServiceImpl extends GwtcServiceAbstract {

  private String binDir;

  Pattern SPECIAL_DIRS = Pattern.compile(
      "("+Pattern.quote(Dependency.DIR_BIN)+")|"+
      "("+Pattern.quote(Dependency.DIR_GEN)+")|"+
      "("+Pattern.quote(Dependency.DIR_TEMP)+")|"
  );

  private MethodBuffer junitLoader;

  public GwtcServiceImpl() {
    this(Thread.currentThread().getContextClassLoader());
  }

  public GwtcServiceImpl(ClassLoader resourceLoader) {
    super(resourceLoader);
  }

  @Override
  public void addAsyncBlock(Class<? extends RunAsyncCallback> asSubclass) {

  }

  @Override
  public void addGwtTestCase(Class<? extends GWTTestCase> subclass) {

  }

  @Override
  public void addGwtTestSuite(Class<? extends GWTTestSuite> asSubclass) {

  }

  @Override
  public void addMethod(Method method) {
    addMethod(method, false);
  }
  @Override
  public void addMethod(Method method, boolean onNewInstance) {
    if (Modifier.isStatic(method.getModifiers())){
      // print a call to a static method
      out.println(out.formatStaticCall(method));
    } else {
      // print a call to an instance method; creating an instance if necessary.
      out.println(out.formatInstanceCall(method, onNewInstance));
    }
  }


  @Override
  public void addPackage(Package pkg, boolean recursive) {
    if (!finished.add(pkg.getName())) {
      return;
    }
    Gwtc gwtc = pkg.getAnnotation(Gwtc.class);
    context.addPackages(pkg, this, recursive);
    if (gwtc != null) {
      context.addGwtcPackage(gwtc, pkg, recursive);
    }
  }

  @Override
  public void compile(GwtManifest manifest, int timeout, TimeUnit unit, In1<Integer> callback) {
    final In2Out1<Integer, TimeUnit, Integer> compilation = doCompile(manifest);
    X_Time.runLater(()->{
      final Integer result = compilation.io(timeout, unit);
      callback.in(result);
    });
  }

  @Override
  public In2Out1<Integer, TimeUnit, Integer> recompile(GwtManifest manifest, In2<ServerRecompiler, Throwable> callback) {
    String gwtHome = generateCompile(manifest);
    // Logging
    X_Log.info(getClass(), "Starting gwt compile", manifest.getModuleName());
    X_Log.trace(getClass(), manifest);
    manifest.setRecompile(true);
    final String[] programArgs = manifest.toProgramArgArray();
    final String[] jvmArgs = manifest.toJvmArgArray();
    X_Log.trace(getClass(), "Args: java ", jvmArgs, programArgs);
    final String[] classpath = manifest.toClasspathFullCompile(getTempDir().getAbsolutePath(), gwtHome);
    X_Log.debug(getClass(), "Requested Classpath\n", classpath);

    X_Log.info("Entry point: "+new File(manifest.getWarDir(), context.getGenName()+".html"));
    In1<Integer> cleanup = prepareCleanup(manifest);

    assert runtimeContainsClasspath(manifest.getGenDir(), classpath);
    URL[] urls = fromClasspath(classpath);
    URLClassLoader loader = manifestBackedClassloader(urls, manifest);

    Mutable<Integer> code = new Mutable<>();
    Thread t = new Thread(()->{
      try {
        final PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
        logger.setMaxDetail(manifest.getLogLevel());
        final RecompileController compiler = SuperDevUtil.getOrMakeController(
            logger,
            manifest,
            manifest.getPort()
        );

        final CompiledDirectory result = compiler.recompile();

        manifest.setCompileDirectory(result);

        code.in(0);
        cleanup.in(code.out1());

        Mutable<In1<IsRecompiler>> userRequest = new Mutable<>();

        final ServerRecompiler useCompiler = getComp ->
          userRequest.mutex(()->{
            if (userRequest.isNonNull()) {
                final In1<IsRecompiler> current = userRequest.out1();
                final In1<IsRecompiler> toAdd = getComp.onlyOnce();
                // newest requests get serviced first,
                // in the event of a page reload, an old request may have disconnected,
                // so we want to process the callbacks from newest to oldest.
                userRequest.in(current.useBeforeMe(toAdd));
            } else {
              userRequest.in(getComp.onlyOnce());
            }
          });
          synchronized (result) {
            result.notify();
          }
        callback.in(useCompiler, null);
        manifest.setOnline(true);
        while (manifest.isOnline()) {
          synchronized (result) {
            try {
              result.wait(1_000);
            } catch (InterruptedException e) {
              X_Log.info(getClass(), "Recompiler interrupted; shutting down");
              return; // kill the thread
            }
          }
          userRequest.useThenSet(pending->{
            if (pending != null) {
              // exit quickly to avoid race conditions!
              runFinally(pending.provide(compiler));
            }
          }, null);
        }
      } catch (Throwable fail) {
        manifest.setOnline(false);
        callback.in(null, fail);
        throw fail;
      }
    });
    t.setContextClassLoader(loader);
    t.start();
    In2Unsafe<Integer, TimeUnit> blocker = (sec, unit) ->{
      try {
        t.join(unit.toMillis(sec));
      } catch (InterruptedException e) {
        throw X_Debug.rethrow(e);
      }
    };
    return blocker.supply1AfterRead(code);
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

  private In1<Integer> prepareCleanup(GwtManifest manifest) {

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

  protected In2Out1<Integer, TimeUnit, Integer> doCompile(GwtManifest manifest) {
    String gwtHome = generateCompile(manifest);
    // Logging
    X_Log.info(getClass(), "Starting gwt compile", manifest.getModuleName());
    X_Log.trace(manifest);
    manifest.setRecompile(false);
    final String[] programArgs = manifest.toProgramArgArray();
    final String[] jvmArgs = manifest.toJvmArgArray();
    X_Log.trace("Args: java ", jvmArgs, programArgs);
    final String[] classpath = manifest.toClasspathFullCompile(getTempDir().getAbsolutePath(), gwtHome);
    X_Log.debug("Requested Classpath\n", classpath);

    In1<Integer> cleanup = prepareCleanup(manifest);

    if (manifest.isUseCurrentJvm()) {
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
      In2Unsafe<Integer, TimeUnit> blocker= (sec, unit) ->{
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
    return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader()) {
      @Override
      public URL getResource(String name) {
        URL url = super.getResource(name);
        if (url == null) {
          // Egregious hack; when running in the same jvm, the classloader is not able to use
          // the bootstrap classpath to load our newly generated resources out of our generated folder,
          // so, we resort to manually looking up any failed resource loads here, in the classloader.
          try {
            File f = new File(inGeneratedDirectory(manifest, name));
            if (f.exists()) {
              return f.toURI().toURL();
            }
          } catch (IOException e) {
            X_Log.warn(getClass(), "Failure to load resources for " + name);
          }
        }
        return url;
      }
    };
  }

  @Override
  public int compile(GwtManifest manifest) {
    return doCompile(manifest)
      .io(120, TimeUnit.SECONDS);
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
        if (url.toExternalForm().endsWith(s)) {
          continue searching;
        }
      }
      return false;
    }

    return true;
  }

  public String generateCompile(GwtManifest manifest) {
    assert tempDir.exists() : "No usable directory "+tempDir.getAbsolutePath();
    X_Log.info(getClass(), "Generated entry point", "\n"+getEntryPoint());
    X_Log.info(getClass(), "Generated module", "\n"+getGwtXml(manifest));
    if (manifest.getModuleName() == null) {
      manifest.setModuleName(genName);
      manifestName = genName;
    } else {
      manifestName = manifest.getModuleName();
      context.setRenameTo(manifest.getModuleName());
    }
    saveGwtXmlFile(context.getGwtXml(manifest), manifest.getModuleName(), manifest);
    manifest.getModules().forEach(mod->{
      saveGwtXmlFile(mod.getBuffer(), mod.getInheritName(), manifest);
    });
    String entryPointLocation = inGeneratedDirectory(manifest, entryPoint.getQualifiedName().replace('.', '/')+".java");
    saveTempFile(entryPoint.toString(), new File(entryPointLocation));
    files.forBoth((path, body)->
      saveTempFile(body.out1(), new File(inGeneratedDirectory(manifest, path)))
    );
    return prepareCompile(manifest);
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
            replacements.add(new Replacement(matcher.start(), matcher.end(), tempDir.getAbsolutePath()));
            break;
          case Dependency.DIR_SOURCE:
            replacements.add(new Replacement(matcher.start(), matcher.end(), tempDir.getAbsolutePath()));
            break;
        }
      }
      for (int i = replacements.size(); i-->0;) {
        Replacement replacement = replacements.get(i);
        value = value.substring(0, replacement.start)
            + replacement.newValue+value.substring(replacement.end);
      }
      return singleItem(value);
    };
  }

  private Out1<? extends Iterable<String>> resolveDependency(GwtManifest manifest, Dep dep) {
    final Dependency dependency = dep.getDependency();
    switch (dependency.dependencyType()) {
      case ABSOLUTE:
        return replaceLocationVars(manifest, dependency.value(), dep.getSource());
      case RELATIVE:
        final Out1<MappedIterable<String>> i = replaceLocationVars(manifest, dependency.value(), dep.getSource())
            .map(itr ->
                {
                  final MappedIterable<Iterable<String>> m = itr.map(item -> relativize(item, manifest, dep));
                  final MappedIterable<String> n = m.flatten(In1Out1.identity());
                  return n;
                }
            );

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

  private Iterable<String> relativize(String item, GwtManifest manifest, Dep dep) {
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
      // TODO consider using the manifest for relativization of resources
      fileLoc = new File("").getAbsolutePath();
    }
    Path loc = Paths.get(fileLoc);
    if (item.isEmpty()) {
      // assume the user wants src/main/java and src/main/resources
      return Chain.<String>startChain()
                .add(loc.resolve("src/main/java").toString())
                .add(loc.resolve("src/main/resources").toString())
                .build();
    } else {
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

  protected GwtcProperties getDefaultLaunchProperties() {
    Gwtc myGwtc = getClass().getAnnotation(Gwtc.class);
    if (myGwtc == null) {
      myGwtc = GwtcServiceImpl.class.getAnnotation(Gwtc.class);
    }
    return myGwtc.propertiesLaunch()[0];
  }

  protected String prepareCompile(GwtManifest manifest) {

    String gwtHome = X_Properties.getProperty("gwt.home");
    if (manifest.getCompileDirectory() == null) {

      GwtcProperties defaultProp = getDefaultLaunchProperties();
      Type level = manifest.getLogLevel();
      String warDir = manifest.getWarDir();
      for (GwtcProperties prop : context.getLaunchProperties()) {

        if (prop.obfuscationLevel() != defaultProp.obfuscationLevel()) {
          manifest.setObfuscationLevel(prop.obfuscationLevel());
        }

        if (prop.logLevel() != defaultProp.logLevel()) {
          if (level.isLowerPriorityThan(prop.logLevel())) {
            level = prop.logLevel();
          }
        }

        if (!prop.warDir().equals(GwtcProperties.DEFAULT_WAR)) {
          warDir = prop.warDir();
        }
      }
      if (warDir == null) {
        warDir = GwtcProperties.DEFAULT_WAR;
      }
      manifest.setLogLevel(level);
      manifest.addSystemProp("xapi.log.level="+level.name());
      manifest.setWarDir(warDir);

      if (warDir.contains("/tmp/")) {
        File f = tempDir;
        try {
          String tempCanonical = f.getCanonicalPath();
          if (!warDir.contains(tempCanonical)) {
            warDir = warDir.replaceAll("/tmp/", tempCanonical + File.separator);
          }
          manifest.setWarDir( warDir );
          warDir = new File(warDir).getCanonicalPath();
          manifest.setWarDir( warDir );
          X_Log.info(getClass(), "Manifest WAR: ",manifest.getWarDir());
          final boolean made = new File(warDir).mkdirs();
          if (!made) {
          X_Log.warn(getClass(), "Unable to create temporary war directory for GWT compile",
              "You will likely get an unwanted war folder in the directory you executed this program \ncheck " + warDir+"");
          }
        } catch (IOException e) {
          X_Log.warn(getClass(), "Unable to create temporary war directory for GWT compile",
              "You will likely get an unwanted war folder in the directory you executed this program \ncheck " + warDir+"", e);
          X_Debug.maybeRethrow(e);
        }
      }
      if (manifest.getUnitCacheDir() == null) {
        try {
          File f = X_File.createTempDir("gwtc-"+manifest.getModuleName()+"UnitCache", manifest.isDisableUnitCache());
          if (f != null) {
            manifest.setUnitCacheDir(f.getCanonicalPath());
          }
        } catch (IOException e) {
          X_Log.warn("Unable to create unit cache work directory for GWT compile",
              "You will likely get unwanted gwtUnitcache folders in the directory you executed this program");
        }
      }
      Set<Dependency> dups = new HashSet<>();
      for (Dep dependency : context.getDependencies()) {
        if (dependency.getDependency().dependencyType() == DependencyType.RELATIVE
            || dups.add(dependency.getDependency())) {
          manifest.addDependencies(resolveDependency(manifest, dependency));
        }
      }
      if (gwtHome == null) {
        URL gwtHomeLocation = Compiler.class.getClassLoader().getResource(Compiler.class.getName().replace('.', '/')+".class");
        if (gwtHomeLocation == null) {
          X_Log.warn("Unable to find gwt home from System property gwt.home, "
              , "nor from looking up the gwt compiler class from classloader.  Defaulting to ./lib");
          gwtHome = X_File.getPath(".");
        } else {
          gwtHome = gwtHomeLocation.toExternalForm();
          if (gwtHome.contains("jar!")) {
            gwtHome = gwtHome.split("jar!")[0]+"jar";
          }
          gwtHome = gwtHome.replace("file:", "").replace("jar:", "");
          if (manifest.getGwtVersion().length() == 0) {
            if (gwtHome.contains("gwt-dev.jar")) {
              manifest.setGwtVersion("");
            } else {
              manifest.setGwtVersion(extractGwtVersion(gwtHome));
            }
          }
          int ind = gwtHome.lastIndexOf("gwt-dev");
          gwtHome = gwtHome.substring(0, ind-1);
        }
      }
      X_Properties.setProperty("gwt.home", gwtHome);
    }

    generateWar(manifest);
    return gwtHome;
  }

  @Override
  public boolean addJUnitClass(Class<?> clazz) {
    if (!finished.add(clazz.getName())) {
      X_Log.info(getClass(), "Skipped JUnit 4 class",clazz);
      return false;
    }
    search: {
      for (Method m : clazz.getMethods()) {
        if (m.isAnnotationPresent(Test.class)) {
          break search;
        }
      }
      return false;
    }
    Gwtc gwtc = clazz.getAnnotation(Gwtc.class);
    X_Log.info(getClass(), "generating JUnit class", clazz, "?"+(gwtc != null));
    if (gwtc != null) {
      context.addGwtcClass(gwtc, clazz);
    }
    addGwtModules(clazz);
    X_Log.info(getClass(), "added test class for JUnit 4",clazz);
    ensureReportError();
    inheritGwtXml(clazz, ResourceBuilder.buildResource("org.junit.JUnit4").build());
    inheritGwtXml(clazz, ResourceBuilder.buildResource("com.google.gwt.core.Core").build());
    ClassBuffer cb = classBuffer();
    String simple = cb.addImport(clazz);
    String methodName = "add"+simple+"Tests";
    String gwt = cb.addImport(GWT.class);
    String callback = cb.addImport(RunAsyncCallback.class);
    String magic = cb.addImportStatic(GwtReflect.class, "magicClass");
    cb.createMethod("void "+methodName)
      .println(gwt+".runAsync("+simple+".class,")
      .indent()
      .println("new "+callback+ "() {")
      .indent()
      .println("public void onSuccess() {")
      .indent()
      .println(magic+"("+simple+".class);")
      .startTry()
      .println("junit.addTests("+simple+".class);")
      .startCatch("Throwable", "e")
      .println("junit.print(\"Error adding "+simple+" to unit test\", e);")
      .endTry()
      .outdent()
      .println("}")
      .println()
      .println("public void onFailure(Throwable reason) {")
      .indent()
      .println("junit.print(\"Error loading "+simple+"\", reason);")
      .outdent()
      .println("}")
      .outdent()
      .println("}")
      .outdent()
      .println(");")
      ;

    junitLoader.println(methodName+"();");
    return true;
  }

  @Override
  protected void generateReportError(ClassBuffer classBuffer) {
    super.generateReportError(classBuffer);
    addClass(JUnit4Runner.class);
    junitLoader = classBuffer.createInnerClass("private final class JUnit extends JUnitUi")
      .createMethod("public void loadAllTests()");

    classBuffer.createField(JUnitUi.class, "junit")
      .setModifier(Modifier.FINAL | Modifier.PRIVATE)
      .setInitializer("new JUnit()");

    out.println("junit.onModuleLoad();");
  }

}
