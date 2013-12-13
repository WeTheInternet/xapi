package xapi.dev.scanner.impl;

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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

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
import xapi.util.X_Debug;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_Util;

@InstanceDefault(implFor = ClasspathScanner.class)
public class ClasspathScannerDefault implements ClasspathScanner {

  final Set<String> pkgs;
  final Set<Class<? extends Annotation>> annotations;
  final Set<Pattern> resourceMatchers;
  final Set<Pattern> bytecodeMatchers;
  final Set<Pattern> sourceMatchers;
  final Set<String> activeJars;

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

    public ScanRunner(URL classpath, Iterable<String> pkgs,
      ClasspathResourceMap map, int priority) {
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
      boolean jarUrl = path.startsWith("jar:");
      if (jarUrl) path = path.substring("jar:".length());
      boolean fileUrl = path.startsWith("file:");
      if (fileUrl) path = path.substring("file:".length());
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
          for (String pkg : pathRoot) {
            if (fileRoot.replace('/', '.').endsWith(pkg.endsWith(".")?pkg:pkg+".")) {
              scan(file, fileRoot.substring(0, fileRoot.length() - pkg.length() + delta));
              break;
            }
          }
        }
      } catch (Exception e) {
        Thread t = Thread.currentThread();
        t.getUncaughtExceptionHandler().uncaughtException(t, e);
      }
    }

    private final void scan(File file, String pathRoot) throws IOException {
      if (file.isDirectory()) {
        scan(file.listFiles(), pathRoot);
      } else {
        addFile(file, pathRoot);
      }
    }

    private void scan(File[] listFiles, String pathRoot) throws IOException {
      for (File file : listFiles) {
        scan(file, pathRoot);
      }
    }

    private final void scan(JarFile jarFile) {
      if (activeJars.add(jarFile.getName())) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry next = entries.nextElement();
          addEntry(jarFile, next);
        }
      }
    }

    protected void addFile(File file, String pathRoot) throws IOException {
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

    protected void addEntry(JarFile file, JarEntry entry) {
      String name = entry.getName();
      for (String pkg : pkgs) {
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
  public ClasspathScanner scanAnnotation(Class<? extends Annotation> annotation) {
    annotations.add(annotation);
    return this;
  }

@Override
  public ClasspathScanner scanAnnotations(@SuppressWarnings("unchecked")
    Class<? extends Annotation> ... annotations) {
    for (Class<? extends Annotation> annotation : annotations)
      this.annotations.add(annotation);
    return this;
  }

  @Override
  public ClasspathResourceMap scan(ClassLoader loaders) {
    ExecutorService executor = newExecutor();
    try {
      return scan(loaders, executor).call();
    } catch (Exception e) {
      throw X_Debug.rethrow(e);
    }
  }

  @Override
  public ExecutorService newExecutor() {
    return Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()*3/2);
  }
  
  @Override
  public synchronized Callable<ClasspathResourceMap> scan(ClassLoader loadFrom, ExecutorService executor) {
    // perform the actual scan
    Map<URL,Fifo<String>> classPaths = new LinkedHashMap<URL,Fifo<String>>();
    if (pkgs.size() == 0 || (pkgs.size() == 1 && "".equals(pkgs.iterator().next()))) {
      
      for (String pkg : X_Properties.getProperty(X_Namespace.PROPERTY_RUNTIME_SCANPATH,
        ",META-INF,com,org,net,xapi,java").split(",\\s*")) {
        pkgs.add(pkg);
      }
    }
    for (String pkg : pkgs) {

      final Enumeration<URL> resources;
      try {
        resources = loadFrom.getResources(
          pkg.equals(".*")//||pkg.equals("")
            ?"":pkg.replace('.', '/')
          );
      } catch (Exception e) {
        e.printStackTrace();
        continue;
      }
      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        String file = resource.toExternalForm();
        if (file.contains("gwt-dev.jar"))
          continue;
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
    int pos = 0;
    final ClasspathResourceMap map = new ClasspathResourceMap(executor,
      annotations, bytecodeMatchers, resourceMatchers, sourceMatchers);
    final Fifo<Future<?>> jobs = new SimpleFifo<Future<?>>();
    class Finisher implements Callable<ClasspathResourceMap>{
      @Override
      public ClasspathResourceMap call() throws Exception {
        while (!jobs.isEmpty()) {
          // drain the work pool
          Iterator<Future<?>> iter = jobs.forEach().iterator();
          while (iter.hasNext()) {
            if (iter.next().isDone()) {
              iter.remove();
            }
          }
          try {
            Thread.sleep(0, 500);
          } catch (InterruptedException e) {
            throw X_Debug.rethrow(e);
          }
        }
        return map;
      }
    }
    for (URL url : classPaths.keySet()) {
      Fifo<String> packages = classPaths.get(url);
      ScanRunner scanner = newScanRunner(url, map, executor, packages.forEach(), pos);
      jobs.give(executor.submit(scanner));
    }
    return new Finisher();
  }

  private ScanRunner newScanRunner(URL classPath, ClasspathResourceMap map, ExecutorService executor,
    Iterable<String> pkgs, int priority) {
    return new ScanRunner(classPath, pkgs, map, priority);
  }

  @Override
  public ClasspathScanner scanPackage(String pkg) {
    pkgs.add(pkg);
    return this;
  }

  @Override
  public ClasspathScanner scanPackages(String ... pkgs) {
    for (String pkg : pkgs)
      this.pkgs.add(pkg);
    return this;
  }

  @Override
  public ClasspathScanner matchClassFile(String regex) {
    bytecodeMatchers.add(Pattern.compile(regex));
    return this;
  }
  @Override
  public ClasspathScanner matchClassFiles(String ... regexes) {
    for (String regex : regexes) {
      bytecodeMatchers.add(Pattern.compile(regex));
    }
    return this;
  }
  @Override
  public ClasspathScanner matchResource(String regex) {
    resourceMatchers.add(Pattern.compile(regex));
    return this;
  }
  @Override
  public ClasspathScanner matchResources(String ... regexes) {
    for (String regex : regexes)
      resourceMatchers.add(Pattern.compile(regex));
    return this;
  }
  @Override
  public ClasspathScanner matchSourceFile(String regex) {
    sourceMatchers.add(Pattern.compile(regex));
    return this;
  }
  @Override
  public ClasspathScanner matchSourceFiles(String ... regexes) {
    for (String regex : regexes)
      sourceMatchers.add(Pattern.compile(regex));
    return this;
  }

}
