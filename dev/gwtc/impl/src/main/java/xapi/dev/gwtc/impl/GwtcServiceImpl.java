package xapi.dev.gwtc.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Dependency.DependencyType;
import xapi.annotation.inject.InstanceDefault;
import xapi.dev.gwtc.api.*;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage;
import xapi.dev.impl.ExtensibleClassLoader;
import xapi.dev.impl.ReflectiveMavenLoader;
import xapi.dev.source.ClassBuffer;
import xapi.file.X_File;
import xapi.fu.*;
import xapi.fu.itr.*;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.GwtcProperties;
import xapi.io.X_IO;
import xapi.jre.inject.RuntimeInjector;
import xapi.jre.process.ConcurrencyServiceJre;
import xapi.log.X_Log;
import xapi.mvn.api.MvnDependency;
import xapi.reflect.X_Reflect;
import xapi.test.junit.JUnit4Runner;
import xapi.test.junit.JUnitUi;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import static xapi.fu.itr.ArrayIterable.iterate;
import static xapi.fu.itr.SingletonIterator.singleItem;
import static xapi.source.X_Source.removeClassDirs;
import static xapi.time.X_Time.diff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dev.Compiler;

/**
 * This class is the ball of brains behind hosting multiple gwt compiles, concurrently,
 * behind isolated classloader or processes, and using dynamic module generation / assembly.
 *
 * It is useful for both unit tests, development servers and in your build glue.
 *
 *
 */
@Gwtc(propertiesLaunch=@GwtcProperties)
@InstanceDefault(implFor=GwtcService.class)
public class GwtcServiceImpl extends GwtcServiceAbstract {

  static {
    if (System.getProperty("javax.xml.parsers.DocumentBuilderFactory") == null) {
      System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }
    if (System.getProperty("javax.xml.parsers.SAXParserFactory") == null) {
      System.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    }
  }

  Pattern SPECIAL_DIRS = Pattern.compile(
      "(" + Pattern.quote(Dependency.DIR_BIN) + ")|" +
          "(" + Pattern.quote(Dependency.DIR_GEN) + ")|" +
          "(" + Pattern.quote(Dependency.DIR_TEMP) + ")|"
  );

  private final GwtcJobManagerImpl manager;
  private String binDir;

  public GwtcServiceImpl() {
    manager = new GwtcJobManagerImpl(this);
  }

  @Override
  public GwtcJobManagerAbstract getJobManager() {
    return manager;
  }

  @Override
  public void doCompile(
      boolean avoidWork, GwtManifest manifest, long timeout, TimeUnit unit, In2<CompiledDirectory, Throwable> callback
  ) {
    // TODO: consider supporting a TreeLogger argument, and being able to get the job to feed results back to it.
    // (collIDE needs this; it was a feature we removed when gutting the Gwtc prototype).
    if (avoidWork && manager.getStatus(manifest.getModuleName()) == CompileMessage.Success) {
      CompiledDirectory dir = manifest.getCompileDirectory();
      if (dir != null) {
        callback.in(dir, null);
        return;
      }
    }
    final Moment start = X_Time.now();
    try {
      manager.compileIfNecessary(manifest, callback);
    } catch (Exception e) {
      X_Log.error(GwtcServiceImpl.class, "Attempt to compile had unexpected error", e, " Killing all jobs in GwtcService", this);
      getJobManager().runningJobs.values().forEach(this::destroy);
      callback.in(null, e);
      return;
    }
    X_Log.trace(GwtcServiceImpl.class, "maybeCompile took", diff(start));
    if (unit != null) {
      try {
        manager.blockFor(manifest.getModuleName(), timeout, unit);
      } catch (TimeoutException e) {
        callback.in(null, e);
      }
    }
  }

  @Override
  public URLClassLoader resolveClasspath(GwtManifest manifest) {
    X_Log.info(GwtcServiceImpl.class, "Starting gwt compile", manifest.getModuleName());
    X_Log.debug(getClass(), manifest);

    final String[] classpath = manifest.toClasspathFullCompile();
    X_Log.debug(GwtcServiceImpl.class, "Requested Classpath\n", classpath);

    In1<Integer> cleanup = prepareCleanup(manifest);

    URL[] urls = fromClasspath(classpath);
    final String cacheDir = RuntimeInjector.getInjectorCacheDir();
    if (X_String.isNotEmptyTrimmed(cacheDir)) {
      try {
        urls = X_Fu.push(urls, new URL("file:" + cacheDir));
      } catch (MalformedURLException e) {
        X_Log.warn(GwtcServiceImpl.class, "Bad cache dir computed", cacheDir, " is not a valid URL");
      }
    }
    // TODO: also add the runtime injection path to this set of urls.
    // Ensure our list of urls contain all requested urls
    URLClassLoader loader = manifestBackedClassloader(urls, manifest);

    return loader;
  }

  @Override
  public ReflectiveMavenLoader getMavenLoader() {
    return this;
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

  private URLClassLoader manifestBackedClassloader(URL[] urls, GwtManifest manifest) {
    String genDir = manifest.getGenDir();
    // TODO: use the bundled xapi manifests to get the full gwtc classpath into here.
    final String monitorImpl = X_Reflect.getFileLoc(GwtcJobMonitorImpl.class);
    try {
      final URL genUrl = new URL("file:" + genDir);
      if (iterate(urls).noneMatch(genUrl::equals)) {
        urls = X_Fu.concat(genUrl, urls);
      }
      final URL monitorApiUrl = new URL("file:" + monitorImpl);
      if (iterate(urls).noneMatch(monitorApiUrl::equals)) {
        // Hm... should really be downloading if non match...
        urls = X_Fu.concat(monitorApiUrl, urls);
      }
    } catch (MalformedURLException e) {
      X_Log.error(GwtcServiceImpl.class, "Cannot create URL from GwtManifest genDir", genDir, e);
    }
    final ExtensibleClassLoader loader = new ExtensibleClassLoader(dedup(urls), superLoader(manifest), "GwtcGroup" + System.identityHashCode(this));

    return ensureMeetsMinimumRequirements(loader);
  }

  private URL[] dedup(URL[] urls) {
    Map<String, URL> dedup = new LinkedHashMap<>();
    for (URL url : urls) {
      final URL was = dedup.put(url.getPath(), url);
      if (was != null) {
        X_Log.debug(GwtcServiceImpl.class, "Removing duplicated classpath url", was);
      }
    }
    return dedup.values().toArray(new URL[dedup.size()]);
  }

  @Override
  public URLClassLoader ensureMeetsMinimumRequirements(URLClassLoader classpath) {
    // A valid classpath (currently) must have at least two available dependencies:
    // Our Gwtc impl infrastructure, and a gwt compiler (of some sort).
    // We are going to leave the selection of which gwt compiler up to subtypes,
    // with the sane default of a standard GWT 2.X compilation.
    boolean needsXapiGwtcImpl = false, needsXapiGwtcApi = false, needsGwtDev = false, needsGwtUser = false, needsJreProcess = false, needsSlf4j = false;

    try {
      classpath.loadClass(GwtcService.class.getName());
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      // No class found.  We'll need to add one
      needsXapiGwtcApi = true;
    }
    try {
      classpath.loadClass(GwtcServiceImpl.class.getName());
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      // No class found.  We'll need to add one
      needsXapiGwtcImpl = true;
    }
    try {
      classpath.loadClass(GWT.class.getName());
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      // No class found.  We'll need to add one
      needsGwtUser = true;
    }
    try {
      classpath.loadClass(Compiler.class.getName());
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      // No class found.  We'll need to add one
      needsGwtDev = true;
    }
    try {
      // To avoid annoying warnings from any dependencies that include slf4j api
      // but client does not have any impl, we'll automatically add simple logger if needed
      classpath.loadClass("org.slf4j.LoggerFactory");
      needsSlf4j = true;
      classpath.loadClass("org.slf4j.impl.StaticLoggerBinder");
      needsSlf4j = false;
    } catch (ClassNotFoundException | NoClassDefFoundError ignored) { }
    try {
      classpath.loadClass(ConcurrencyServiceJre.class.getName());
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      // No class found.  We'll need to add one
      needsJreProcess = true;
    }
    if (needsGwtUser || needsGwtDev || needsXapiGwtcApi || needsXapiGwtcImpl || needsJreProcess) {
      // We'll need to prepare a wrapper around classloader...
      ChainBuilder<Out1<Iterable<String>>> urls = Chain.startChain();
      if (needsGwtDev) {
        final MvnDependency dep = getDependency("net.wetheinter", "gwt-dev", X_Namespace.GWT_VERSION);
        urls.add(downloadDependency(dep));
      }
      if (needsGwtUser) {
        final MvnDependency dep = getDependency("net.wetheinter", "gwt-user", X_Namespace.GWT_VERSION);
        urls.add(downloadDependency(dep));
      }
      if (needsXapiGwtcImpl) {
        final MvnDependency dep = getDependency("xapi-gwtc-impl");
        urls.add(downloadDependency(dep));
      } else {
        if (needsXapiGwtcApi) {
          final MvnDependency dep = getDependency("xapi-gwtc-api");
          urls.add(downloadDependency(dep));
        }
        if (needsJreProcess) {
          final MvnDependency dep = getDependency("xapi-process-jre");
          urls.add(downloadDependency(dep));
        }
      }
      if (needsSlf4j) {
        final MvnDependency dep = getDependency("org.slf4j", "slf4j-simple", "1.7.25");
        urls.add(downloadDependency(dep));
      }

      final URL[] arr = urls
          .map(Out1::out1)
          .flatten(MappedIterable::mapped)
          .map(file->file.startsWith("file:") ? file : "file:" + file)
          .mapUnsafe(URL::new)
          .toArray(URL[]::new);
      // TODO: just add these urls directly to our extensible classloader using a dynamic:classloaderId url,
      // which, when accessed via dynamic:classloaderId/$ls, returns the list of source URLs,
      // and when accessed with any other path, scans the supplied urls w/ ClasspathScanner to satisfy requests.
      if (classpath instanceof ExtensibleClassLoader) {
        ExtensibleClassLoader ex = (ExtensibleClassLoader) classpath;
        ex.addUrls(arr);
        return ex;
      } else {
        @SuppressWarnings("UnnecessaryLocalVariable") // nice for debugging
        ExtensibleClassLoader newLoader = new ExtensibleClassLoader(dedup(arr), classpath, "GwtcGroup" + System.identityHashCode(this));
        return newLoader;
      }
    }
    return classpath;
  }

  protected ClassLoader superLoader(GwtManifest manifest) {
    if (manifest.isUseCurrentJvm()) {
      if (manifest.isIsolateClassLoader()) {
          // TODO perhaps instead create a minimal super-loader that contains gwt-dev, gwt-user, (xapi-gwt || xapi-gwtc-api & xapi-gwtc-impl)
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
  public void generateCompile(GwtManifest manifest) {
    final GwtcProjectGenerator project = getProject(manifest.getModuleName());
    project.generateCompile(manifest);
  }

  public GwtcProjectGenerator getProject(String moduleName, ClassLoader resources) {
    final ClassLoader loader = resources == null ? Thread.currentThread().getContextClassLoader() : resources;
    return projects.getOrCreate(moduleName, mod->
      new GwtcProjectGeneratorDefault(this, loader, mod) {
        @Override
        protected void generateReportError(ClassBuffer classBuffer) {
          super.generateReportError(classBuffer);
          addClass(JUnit4Runner.class);
          // This needs to go in GwtcServiceJUnit
          junitLoader = classBuffer.createInnerClass("private final class JUnit extends JUnitUi")
              .createMethod("public void loadAllTests()");

          classBuffer.createField(JUnitUi.class, "junit")
              .setModifier(Modifier.FINAL | Modifier.PRIVATE)
              .setInitializer("new JUnit()");

          out.println("junit.onModuleLoad();");
        }
      }
    );
  }

  @Override
  public void destroy(GwtcJob existing) {
    // perhaps also delete the project?  we probably don't want that,
    // as that's how the user configured the compilation to begin with.
    getJobManager().destroy(existing);
  }

  //  @Override
  protected void generateReportError(ClassBuffer classBuffer) {
//    super.generateReportError(classBuffer);
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
        final GwtcProjectGenerator project = getProject(manifest.getModuleName());
        String type = matcher.group();
        switch (type) {
          case Dependency.DIR_BIN:
            if (binDir == null) {
              binDir = X_Reflect.getFileLoc(X_Reflect.getMainClass());
              if (binDir == null || new File(binDir).getName().matches("rt[.]+jar")) {
                // TODO: we should be exposing a configurable "class path generator",
                // so users can easily switch between gradle or ...other build tools.
                binDir = "build/classes/java/" + (manifest.isTestMode() ? "test" : "main");
              } else {
                binDir = binDir.replace("file:", "");
              }
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
      if (!new File(val).isAbsolute()) {
        final File f = new File(manifest.getRelativeRoot(), value);
        if (f.exists()) {
          value = f.getAbsolutePath();
        }
      }
      if (!new File(val).exists()) {
        return singleItem(value);
      }
      try {
        return singleItem(new File(value).getCanonicalPath());
      } catch (IOException e) {
        X_Log.warn(GwtcServiceImpl.class, "Failed to canonicalize ", value, e);
        return singleItem(value);
      }
    };
  }

  @Override
  public Out1<? extends Iterable<String>> resolveDependency(GwtManifest manifest, AnnotatedDependency dep) {
    final Dependency dependency = dep.getDependency();
    switch (dependency.dependencyType()) {
      case ABSOLUTE:
        return replaceLocationVars(manifest, dependency.value(), dep.getSource());
      case CLASSPATH_FILE:
      case RELATIVE:
        return replaceLocationVars(manifest, dependency.value(), dep.getSource())
            .map(itr->
                itr.map(item-> {
                  if (dependency.dependencyType() == DependencyType.RELATIVE) {
                    return relativize(item, manifest, dep);
                  } else {
                    return loadClasspathFile(item, manifest, dep);
                  }
                })
                .flatten(In1Out1.identity())
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
        return downloadDependency(getDependency(dependency));
      default:
        throw new IllegalArgumentException("Unhandled case " + dependency.dependencyType());
    }
  }

  private Iterable<String> loadClasspathFile(String item, GwtManifest manifest, AnnotatedDependency dep) {
    if (Files.exists(Paths.get(item))) {
      try (
          FileInputStream in = new FileInputStream(item)
      ) {
          String classpath = X_IO.toStringUtf8(in);
          return iterate(classpath.split("[:;]"));
      } catch (IOException e) {
        X_Log.error(GwtcServiceImpl.class, "Cannot load classpath file", item, e);
      }

    } else {
      final URL res = Thread.currentThread().getContextClassLoader().getResource(item);
      if (res == null) {
        X_Log.warn(GwtcServiceImpl.class, "Classpath file does not exist", item);
      } else {
        try {
          String classpath = X_IO.toStringUtf8(res.openStream());
          return iterate(classpath.split("[:;]"));
        } catch (IOException e) {
          X_Log.error(GwtcServiceImpl.class, "Cannot load ", item, "from classloader url", res, e);
        }
      }
    }
    return EmptyIterator.none();
  }

  private Iterable<String> relativize(String item, GwtManifest manifest, AnnotatedDependency dep) {
    if (Files.exists(Paths.get(item))) {
      return singleItem(item);
    }
    final String fileLoc;
    if (dep.getSource() instanceof Class<?>) {
      final String raw = X_Reflect.getFileLoc((Class<?>) dep.getSource());
      fileLoc = removeClassDirs(raw);
    } else if (dep.getSource() instanceof Package) {
      String pkgInfo = ((Package) dep.getSource()).getName().replace('.', '/') + "/package-info.class";
      final URL res = Thread.currentThread().getContextClassLoader().getResource(pkgInfo);
      final String raw = res.toExternalForm().replace("file:", "");
      fileLoc = removeClassDirs(raw)
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

      String path = loc.getFileName().toString();
      if (path.contains("jar")) {
        path = path.split("jar!")[0] + "jar";
      try (
          JarFile jar = new JarFile(path)
          ) {
        final ZipEntry entry = jar.getEntry("META-INF/xapi/paths.xapi");

        String settings  = X_IO.toStringUtf8(jar.getInputStream(entry));
        // h'ray... xapi settings.
        final UiContainerExpr el = JavaParser.parseUiContainer(
            path + "!META-INF/xapi/paths.xapi",
            settings
        );
        ChainBuilder<String> chain = Chain.startChain();
        final ComposableXapiVisitor<GwtcServiceImpl> vis = ComposableXapiVisitor.onMissingLog(GwtcServiceImpl.class, true);
            vis.withUiAttrExpr((atr, s)->{
                switch(atr.getNameString()) {
                  case "sources":
                  case "resources":
                  case "outputs":
                    atr.getExpression().accept(vis, s);
                }
              return false;
            })
            .withStringLiteralTerminal((str, s) -> {
                  if (new File(str.getValue()).exists()) {
                    chain.add(str.getValue());
                  } else {
                    X_Log.warn(GwtcServiceImpl.class, "Ignoring missing source", str);
                  }
                }
            )
            .withJsonContainerRecurse(In2.ignoreAll())
            .withUiContainerTerminal(In2.ignoreAll())
            .visit(el, this);

            if (chain.isNotEmpty()) {
              return chain;
            }

      } catch (IOException ignored){} catch (ParseException e) {
        X_Log.error(GwtcServiceImpl.class, "Invalid source for ", path, e);
      }

        path = loc.toString()
              .split("build/libs")[0]
              .split("build/classes")[0]
              .split("target/")[0];

          loc = Paths.get(path);
      }
      try {
        return singleItem(loc.resolve(item).toRealPath().toString());
      } catch (IOException e) {
        throw X_Debug.rethrow(e);
      }
    }
  }

  @Override
  public GwtcProperties getDefaultLaunchProperties() {
    Gwtc myGwtc = getDefaultGwtc();
    return myGwtc.propertiesLaunch()[0];
  }

  protected Gwtc getDefaultGwtc() {
    Class<?> c = getClass();
    Gwtc myGwtc;
    do {
      myGwtc = c.getAnnotation(Gwtc.class);
      c = c.getSuperclass();
    }
    while (myGwtc == null && c != null);

    if (myGwtc == null) {
      // help out subclasses by trying their own type first.
      myGwtc = GwtcServiceImpl.class.getAnnotation(Gwtc.class);
    }
    return myGwtc;
  }

}
