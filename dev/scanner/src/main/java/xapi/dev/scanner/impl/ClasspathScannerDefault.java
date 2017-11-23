package xapi.dev.scanner.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.dev.resource.impl.ByteCodeResource;
import xapi.dev.resource.impl.FileBackedResource;
import xapi.dev.resource.impl.JarBackedResource;
import xapi.dev.resource.impl.SourceCodeResource;
import xapi.dev.resource.impl.StringDataResource;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.except.ThreadsafeUncaughtExceptionHandler;
import xapi.fu.Do;
import xapi.util.X_Debug;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_Util;
import xapi.util.api.ProvidesValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

@InstanceDefault(implFor = ClasspathScanner.class)
public class ClasspathScannerDefault implements ClasspathScanner {

  final Set<String> pkgs;
  final Set<Class<? extends Annotation>> annotations;
  final Set<Pattern> resourceMatchers;
  final Set<Pattern> bytecodeMatchers;
  final Set<Pattern> sourceMatchers;
  final Set<String> activeJars;
  private boolean scanSystemJars;

  public ClasspathScannerDefault() {
    pkgs = new HashSet<String>();
    annotations = new HashSet<Class<? extends Annotation>>();
    resourceMatchers = new HashSet<Pattern>();
    bytecodeMatchers = new HashSet<Pattern>();
    sourceMatchers = new HashSet<Pattern>();
    activeJars = new HashSet<String>();
  }

  protected class ScanRunner implements Runnable {

    private final URL classpath;
    private final ClasspathResourceMap map;
    private final int priority;
    private final Iterable<String> pathRoot;
    private Thread creatorThread;

    public ScanRunner(final URL classpath, final Iterable<String> pkgs,
      final ClasspathResourceMap map, final int priority) {
      this.classpath = classpath;
      this.map = map;
      this.priority = priority;
      this.pathRoot = pkgs;
      creatorThread = Thread.currentThread();
    }

    @Override
    public void run() {
      Thread.currentThread().setUncaughtExceptionHandler(new ThreadsafeUncaughtExceptionHandler(creatorThread));
      creatorThread = null;

      // determine if we should run in jar mode or file mode
      File file;
      String path = classpath.toExternalForm();
      final boolean jarUrl = path.startsWith("jar:");
      if (jarUrl) {
        path = path.substring("jar:".length());
      }
      final boolean fileUrl = path.startsWith("file:");
      if (fileUrl) {
        path = path.substring("file:".length());
      }
      boolean jarFile = path.contains(".jar!");
      if (jarFile) {
        path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
      } else {
        jarFile = path.endsWith(".jar");
      }
      if (!(file = new java.io.File(path)).exists()) {
        path = new File(path).toURI().toString();
        if ((file = new java.io.File(path)).exists()) {
          // should be impossible since we get these urls from classloader
          throw X_Util.rethrow(new FileNotFoundException());
        }
      }
      try {
        if (classpath.getProtocol().equals("jar")) {
          scan(((JarURLConnection)classpath.openConnection()).getJarFile());
          return;
        }
        assert classpath.getProtocol().equals("file") : "ScanRunner only handles url and file protocols";

        if (jarFile) {
          scan(new JarFile(file));
        } else {
          // For files, we need to strip everything up to the package we are
          // scanning
          String fileRoot = file.getCanonicalPath().replace('\\', '/');
          int delta = 0;
          if (!fileRoot.endsWith("/")) {
            delta = -1;
            fileRoot += "/";
          }
          for (final String pkg : pathRoot) {
            if (fileRoot.replace('/', '.').endsWith(pkg.endsWith(".")?pkg:pkg+".")) {
              scan(file, fileRoot.substring(0, fileRoot.length() - pkg.length() + delta));
              break;
            }
          }
        }
      } catch (final Exception e) {
        final Thread t = Thread.currentThread();
        t.getUncaughtExceptionHandler().uncaughtException(t, e);
      }
    }

    private final void scan(final File file, final String pathRoot) throws IOException {
      if (file.isDirectory()) {
        scan(file.listFiles(), pathRoot);
      } else {
        addFile(file, pathRoot);
      }
    }

    private void scan(final File[] listFiles, final String pathRoot) throws IOException {
      for (final File file : listFiles) {
        scan(file, pathRoot);
      }
    }

    private final void scan(final JarFile jarFile) {
      if (activeJars.add(jarFile.getName())) {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          final JarEntry next = entries.nextElement();
          addEntry(jarFile, next);
        }
      }
    }

    protected void addFile(final File file, final String pathRoot) throws IOException {
      String name = file.getCanonicalPath().substring(pathRoot.length());
      if (name.startsWith(File.separator)) {
        name = name.substring(1);
      }
      if (name.endsWith(".class")) {
        if (map.includeBytecode(name)) {
          map.addBytecode(name, new ByteCodeResource(
              new FileBackedResource(name, file, priority)));
        }
      } else if (name.endsWith(".java")) {
        if (map.includeSourcecode(name)) {
          map.addSourcecode(name, new SourceCodeResource(
              new FileBackedResource(name, file, priority)));
        }
      } else {
        if (map.includeResource(name)) {
          map.addResource(name, new StringDataResource(
              new FileBackedResource(name, file, priority)));
        }
      }
    }

    protected void addEntry(final JarFile file, final JarEntry entry) {
      final String name = entry.getName();
      for (final String pkg : pkgs) {
        if (name.startsWith(pkg)) {
          if (name.endsWith(".class")) {
            if (map.includeBytecode(name)) {
              map.addBytecode(name, new ByteCodeResource(
                  new JarBackedResource(file, entry, priority)));
            }
          } else if (name.endsWith(".java")) {
            if (map.includeSourcecode(name)) {
              map.addSourcecode(name, new SourceCodeResource(
                  new JarBackedResource(file, entry, priority)));
            }
          } else {
            if (map.includeResource(name)) {
              map.addResource(name, new StringDataResource(
                  new JarBackedResource(file, entry, priority)));
            }
          }
          return;
        }
      }
    }

  }

  @Override
  public ClasspathScanner scanAnnotation(final Class<? extends Annotation> annotation) {
    annotations.add(annotation);
    return this;
  }

@Override
  public ClasspathScanner scanAnnotations(@SuppressWarnings("unchecked")
  final
    Class<? extends Annotation> ... annotations) {
    for (final Class<? extends Annotation> annotation : annotations) {
      this.annotations.add(annotation);
    }
    return this;
  }

  @Override
  public ClasspathResourceMap scan(final ClassLoader loaders) {
    final ExecutorService executor = newExecutor();
    try {
      final ClasspathResourceMap map = scan(loaders, executor).call();
      synchronized (map) {
        return map;
      }
    } catch (final Exception e) {
      throw X_Debug.rethrow(e);
    }
  }

  private Do onShutdown = Do.NOTHING;
  @Override
  public ExecutorService newExecutor() {
    final int threads = Runtime.getRuntime().availableProcessors()*3/2;
    final ExecutorService pool = Executors.newFixedThreadPool(threads);
    final Do after = onShutdown;
    // we run new callbacks sooner, as they are more likely to have
    // actual work still running to shutdown, and so that we can
    // re-assign onShutdown as we go, such that each callback
    // will erase itself before running, leaving onShutdown == Do.NOTHING;
    onShutdown = after.doBefore(()-> {
      // unroll shutdown loop as we process, to avoid double-calling
      onShutdown = after;
      pool.shutdownNow();
    });
    return pool;
  }

  @Override
  public void shutdown() {
    onShutdown.done();
  }

  @Override
  public synchronized Callable<ClasspathResourceMap> scan(final ClassLoader loadFrom, final ExecutorService executor) {
    // perform the actual scan
    final Map<URL,Fifo<String>> classPaths = new LinkedHashMap<URL,Fifo<String>>();
    if (pkgs.size() == 0 || (pkgs.size() == 1 && "".equals(pkgs.iterator().next()))) {

      for (final String pkg : X_Properties.getProperty(X_Namespace.PROPERTY_RUNTIME_SCANPATH,
        ",META-INF,com,org,net,xapi,java").split(",\\s*")) {
        pkgs.add(pkg);
      }
    }
    for (final String pkg : pkgs) {

      final Enumeration<URL> resources;
      try {
        resources = loadFrom.getResources(
          pkg.equals(".*")//||pkg.equals("")
            ?"":pkg.replace('.', '/')
          );
      } catch (final Exception e) {
        e.printStackTrace();
        continue;
      }
      while (resources.hasMoreElements()) {
        final URL resource = resources.nextElement();
        final String file = resource.toExternalForm();
        if (file.contains("gwt-dev.jar")) {
          continue;
        }
        Fifo<String> fifo = classPaths.get(resource);
        if (fifo == null) {
          fifo = new SimpleFifo<String>();
          fifo.give(pkg);
          classPaths.put(resource, fifo);
        } else {
          fifo.remove(pkg);
          fifo.give(pkg);
        }
      }
    }
    final int pos = 0;
    final ProvidesValue<ExecutorService> exe = new ProvidesValue<ExecutorService>() {
      ExecutorService exe = executor;
      @Override
      public ExecutorService get() {
        if (exe.isShutdown()) {
          exe = newExecutor();
        }
        return exe;
      }
    };
    final ClasspathResourceMap map = new ClasspathResourceMap(exe,
      annotations, bytecodeMatchers, resourceMatchers, sourceMatchers);
    map.setClasspath(classPaths.keySet());
    final Fifo<Future<?>> jobs = new SimpleFifo<Future<?>>();
    class Finisher implements Callable<ClasspathResourceMap>{
      @Override
      public ClasspathResourceMap call() throws Exception {
        while (!jobs.isEmpty()) {
          // drain the work pool
          final Iterator<Future<?>> iter = jobs.forEach().iterator();
          while (iter.hasNext()) {
            if (iter.next().isDone()) {
              iter.remove();
            }
          }
          LockSupport.parkNanos(250_000);
        }
        map.stop();
        return map;
      }
    }
    for (final URL url : classPaths.keySet()) {
      final Fifo<String> packages = classPaths.get(url);
      if (shouldScanUrl(url)) {
        final ScanRunner scanner = newScanRunner(url, map, executor, packages.forEach(), pos);
        jobs.give(executor.submit(scanner));
      }
    }
    return new Finisher();
  }

  protected boolean shouldScanUrl(URL url) {
    String external = url.toExternalForm();
    if (!scanSystemJars) {
      if (external.contains(
          System.getProperty("java.home", "--n0t f0und---")
      )) {
        return false;
      }
    }

    return !external.contains("idea_rt.jar");
  }

  private ScanRunner newScanRunner(final URL classPath, final ClasspathResourceMap map, final ExecutorService executor,
    final Iterable<String> pkgs, final int priority) {
    return new ScanRunner(classPath, pkgs, map, priority);
  }

  @Override
  public ClasspathScanner scanPackage(final String pkg) {
    if (pkg != null) {
      pkgs.add(pkg);
    }
    return this;
  }

  @Override
  public ClasspathScanner scanPackages(final String ... pkgs) {
    for (final String pkg : pkgs) {
      this.pkgs.add(pkg);
    }
    return this;
  }

  @Override
  public ClasspathScanner matchClassFile(final String regex) {
    bytecodeMatchers.add(Pattern.compile(regex));
    return this;
  }

  @Override
  public ClasspathScanner skipSystemJars(boolean skipSystemJars) {
    this.scanSystemJars = !skipSystemJars;
    return this;
  }

  @Override
  public ClasspathScanner matchClassFiles(final String ... regexes) {
    for (final String regex : regexes) {
      bytecodeMatchers.add(Pattern.compile(regex));
    }
    return this;
  }
  @Override
  public ClasspathScanner matchResource(final String regex) {
    resourceMatchers.add(Pattern.compile(regex));
    return this;
  }
  @Override
  public ClasspathScanner matchResources(final String ... regexes) {
    for (final String regex : regexes) {
      resourceMatchers.add(Pattern.compile(regex));
    }
    return this;
  }
  @Override
  public ClasspathScanner matchSourceFile(final String regex) {
    sourceMatchers.add(Pattern.compile(regex));
    return this;
  }
  @Override
  public ClasspathScanner matchSourceFiles(final String ... regexes) {
    for (final String regex : regexes) {
      sourceMatchers.add(Pattern.compile(regex));
    }
    return this;
  }

}
