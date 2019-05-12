package xapi.dev.scanner.impl;

import xapi.bytecode.*;
import xapi.collect.api.Fifo;
import xapi.collect.impl.MultithreadedStringTrie;
import xapi.collect.impl.SimpleFifo;
import xapi.dev.resource.api.ClasspathResource;
import xapi.dev.resource.impl.ByteCodeResource;
import xapi.dev.resource.impl.SourceCodeResource;
import xapi.dev.resource.impl.StringDataResource;
import xapi.fu.Filter.Filter1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.itr.MappedIterable;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.util.X_Debug;
import xapi.util.api.ProvidesValue;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public class ClasspathResourceMap {

  private final ResourceTrie<ByteCodeResource> bytecode;
  private final ResourceTrie<SourceCodeResource> sources;
  private final ResourceTrie<StringDataResource> resources;
  private final Set<Class<? extends Annotation>> annotations;
  private final Set<Pattern> bytecodeMatchers;
  private final Set<Pattern> resourceMatchers;
  private final Set<Pattern> sourceMatchers;
  private final Fifo<ByteCodeResource> pending;
  private final Out1<ExecutorService> refreshExecutor;
  private Lazy<ExecutorService> executor;
  private AnnotatedClassIterator allAnnos;
  private AnnotatedMethodIterator allMethodsWithAnnos;
  private ClassPool pool;
  private ArrayList<URL> classpath;
  private boolean running;

  public ClasspathResourceMap(final ProvidesValue<ExecutorService> executor, final Set<Class<? extends Annotation>> annotations,
    final Set<Pattern> bytecodeMatchers, final Set<Pattern> resourceMatchers, final Set<Pattern> sourceMatchers) {
    this.annotations = annotations;
    this.bytecodeMatchers = bytecodeMatchers;
    refreshExecutor = executor::get;
    this.executor = Lazy.deferred1(refreshExecutor);
    this.resourceMatchers = resourceMatchers;
    this.sourceMatchers = sourceMatchers;
    this.bytecode = new ResourceTrie<ByteCodeResource>();
    this.sources = new ResourceTrie<SourceCodeResource>();
    this.resources = new ResourceTrie<StringDataResource>();
    this.pending = new SimpleFifo<ByteCodeResource>();
    running = true;
  }

  public void stop() {
    running = false;
    if (executor.isResolved()) {
      executor.out1().shutdownNow();
    }
    executor = Lazy.deferred1(refreshExecutor);
  }

  public void addBytecode(final String name, final ByteCodeResource bytecode) {
    this.bytecode.put(X_Source.stripClassExtension(name.replace(File.separatorChar, '.')), bytecode);
      if (!preloadClasses()) {
        return;
      }
      if (pending.isEmpty()) {
        synchronized (pending) {
          // double-checked lock
          if (pending.isEmpty()) {
            getExecutor().submit(new Runnable() {
              // We use one thread to iterate and preload class files
              @Override
              public void run() {
                while (!pending.isEmpty()) {
                  final Iterator<ByteCodeResource> iter = pending.iterator();
                  while (iter.hasNext()) {
                    // Preload classes
                    addSubclasses(iter.next().getClassData());
                    iter.remove();
                    if (!running) {
                      return;
                    }
                  }
                }
              }
            });
          }
          pending.give(bytecode);
        } // end synchro
      } else {
        pending.give(bytecode);
      }
  }

  protected void addSubclasses(final ClassFile classData) {
    // TODO reenable or delete this
//    if (classData.getEnclosedName().indexOf('.')>-1) {
//      bytecode.put(classData.getQualifiedName(), classData);
//    }
  }

  private boolean preloadClasses() {
    return !annotations.isEmpty() || !bytecodeMatchers.isEmpty();
  }

  protected void accept(final String name, final ByteCodeResource bytecode, final Iterable<Class<? extends Annotation>> classAnnotations) {
    final ClassFile classFile = bytecode.getClassData();
    for (final Class<? extends Annotation> annoClass : classAnnotations) {
      maybeAccept(name, bytecode, classFile, annoClass);
    }
  }

  protected void maybeAccept(final String name, final ByteCodeResource bytecode,
      final ClassFile classFile, final Class<? extends Annotation> annoClass) {
    final xapi.bytecode.annotation.Annotation anno = classFile.getAnnotation(annoClass.getName());
    if (anno != null) {
      this.bytecode.put(name, bytecode);
      return;
    }
    // check the target retention of these annotations, and scan methods or fields
    try {
      final Target target = annoClass.getAnnotation(Target.class);
      ElementType[] targets;
      if (target == null) {
        targets = getDefaultAnnotationTargets();
      } else {
        targets = target.value();
      }
      for (final ElementType type : targets) {
        switch (type) {
          case METHOD:
            for (final MethodInfo method : classFile.getMethods()) {
              if (accepts(method, annoClass)) {
                this.bytecode.put(name, bytecode);
                return;
              }
            }
            break;
          case FIELD:
            for (final FieldInfo field : classFile.getFields()) {
              if (accepts(field, annoClass)) {
                this.bytecode.put(name, bytecode);
                return;
              }
            }
            break;
          case CONSTRUCTOR:
            for (final MethodInfo method : classFile.getMethods()) {
              if (method.getName().contains("<init>") && accepts(method, annoClass)) {
                this.bytecode.put(name, bytecode);
                return;
              }
            }
            break;
          default:
            break;
        }
      }
    } catch (final Throwable e) {
      throw X_Debug.rethrow(e);
    }

  }

  private boolean accepts(final MemberInfo method, final Class<? extends Annotation> annoClass) {
      return method.getAnnotation(annoClass.getName()) != null;
  }

  protected ElementType[] getDefaultAnnotationTargets() {
    return
        shouldScanMethods() ?
        new ElementType[]{ElementType.METHOD} :
          new ElementType[0];
  }

  protected boolean shouldScanMethods() {
    return allMethodsWithAnnos != null;
  }

  public void addSourcecode(final String name, final SourceCodeResource sourcecode) {
    this.sources.put(name, sourcecode);
  }

  public void addResource(final String name, final StringDataResource resource) {
    this.resources.put(name, resource);
  }

  public boolean includeResource(final String name) {
    for (final Pattern p : resourceMatchers) {
      if (p.matcher(name).matches()) {
        return true;
      }
      if (p.matcher(name.substring(name.lastIndexOf('/')+1)).matches()) {
        return true;
      }
    }
    return false;
  }

  public boolean includeSourcecode(final String name) {
    for (final Pattern p : sourceMatchers) {
      if (p.matcher(name).matches()) {
        return true;
      }
    }
    return false;
  }

  public boolean includeBytecode(final String name) {
    for (final Pattern p : bytecodeMatchers) {
      if (p.matcher(name).matches()) {
        return true;
      }
    }
    return false;
  }

  public final SourceCodeResource findSource(final String name) {
    return sources.get(name);
  }

  public final MappedIterable<SourceCodeResource> findSources(final String prefix, final Pattern ... patterns) {
    if (patterns.length == 0) {
      return sources.findPrefixed(prefix);
    }
    class Itr implements Iterator<SourceCodeResource> {
      SourceCodeResource cls;
      Iterator<SourceCodeResource> iter = sources.findPrefixed(prefix).iterator();
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next();
          for (final Pattern pattern : patterns) {
            if (pattern.matcher(cls.getResourceName()).matches()) {
              return true;
            }
          }
        }
        return false;
      }

      @Override
      public SourceCodeResource next() {
        return cls;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
    return ()->new Itr();
  }

  public final StringDataResource findResource(final String name) {
    return resources.get(name);
  }

  public final MappedIterable<StringDataResource> findResources(final String prefix, final Pattern ... patterns) {
    if (patterns.length == 0) {
      return resources.findPrefixed(prefix);
    }
    class Itr implements Iterator<StringDataResource> {
      StringDataResource cls;
      Iterator<StringDataResource> iter = resources.findPrefixed(prefix).iterator();
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next();
          for (final Pattern pattern : patterns) {
            if (pattern.matcher(cls.getResourceName()).matches()) {
              return true;
            }
          }
        }
        return false;
      }

      @Override
      public StringDataResource next() {
        return cls;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
    return ()->new Itr();
  }

  public final ClassFile findClass(String clsName) {
    clsName = clsName.replace('/', '.');
    final ByteCodeResource resource = bytecode.get(clsName);
    return resource == null ? null : resource.getClassData();
  }

  @SuppressWarnings("unchecked")
  public final Iterable<ClassFile> getAllClasses(){
    return new ClassFileIterator(Filter1.TRUE, bytecode);
  }

  public final MappedIterable<StringDataResource> getAllResources(){
    return resources.findPrefixed("");
  }

  public Iterable<ClassFile> findClassesInPackage(final String name) {
    return new ClassFileIterator(value -> !"package-info".equals(value.getEnclosedName()) && value.getPackage().equals(name), bytecode);
  }

  public Iterable<ClassFile> findClassesBelowPackage(final String name) {
    return new ClassFileIterator(value -> !"package-info".equals(value.getEnclosedName()) && value.getPackage().startsWith(name), bytecode);
  }

  public Iterable<ClassFile> findPackagesBelowPackage(final String name) {
    return new ClassFileIterator(value -> "package-info".equals(value.getEnclosedName()) && value.getPackage().startsWith(name+"."), bytecode);
  }

  /**
   * Finds all classes that are direct subclasses of one of the supplied types.
   *
   * This does not check interfaces, only the direct supertype.
   * It will _not_ match types equal to the supplied types.
   *
   * This is primarily used for types that cannot have more than one subclass,
   * like Enum or Annotation.
   *
   * @param superClasses
   * @return
   */
  public final Iterable<ClassFile> findDirectSubclasses(
    final Class<?> ... superClasses) {
    return findDirectSubclasses(X_Source.toStringBinary(superClasses));
  }
  public final Iterable<ClassFile> findDirectSubclasses(
      final String ... superClasses) {
    // Local class to capture the final method parameter
    class Itr implements Iterator<ClassFile> {
      ClassFile cls;
      Iterator<ByteCodeResource> iter = bytecode.findPrefixed("").iterator();
      final MatchesDirectSubclasses matcher = new MatchesDirectSubclasses(superClasses);
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next().getClassData();
          if (matcher.filter1(cls)) {
            return true;
          }
        }
        return false;
      }

      @Override
      public ClassFile next() {
        return cls;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
    return new Iterable<ClassFile>() {
      @Override
      public Iterator<ClassFile> iterator() {
        return new Itr();
      }
    };
  }

  public final MappedIterable<ClassFile> findImplementationOf(
    final Class<?> ... superClasses) {
    return findImplementationOf(X_Source.toStringBinary(superClasses));
  }
  public final MappedIterable<ClassFile> findImplementationOf(
      final String ... superClasses) {
    // Local class to capture the final method parameter
    class Itr implements Iterator<ClassFile> {
      ClassFile cls;
      Iterator<ByteCodeResource> iter = bytecode.findPrefixed("").iterator();
      final MatchesImplementationsOf matcher = new MatchesImplementationsOf(bytecode, superClasses);
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next().getClassData();
          if (matcher.filter1(cls)) {
            return true;
          }
        }

        return false;
      }

      @Override
      public ClassFile next() {
        return cls;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
    return ()->new Itr();
  }

  public final MappedIterable<ClassFile> findClassAnnotatedWith(
     @SuppressWarnings("unchecked")
     final Class<? extends Annotation> ... annotations) {
    if (allAnnos == null) {
      allAnnos = new AnnotatedClassIterator(getExecutor(), bytecode);
    }
    // Local class to capture the final method parameter
    class Itr implements Iterator<ClassFile> {
      ClassFile cls;
      Iterator<ClassFile> iter = allAnnos.iterator();
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next();
          for (final Class<?> annotation : annotations) {
            if (cls.getAnnotation(annotation.getName())!=null) {
              return true;
            }
          }
        }
        return false;
      }

      @Override
      public ClassFile next() {
        return cls;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }

    return ()->new Itr();
  }


public final MappedIterable<ClassFile> findClassWithAnnotatedMethods(
    @SuppressWarnings("unchecked")
    final Class<? extends Annotation> ... annotations) {
  if (allMethodsWithAnnos == null) {
    allMethodsWithAnnos = new AnnotatedMethodIterator(getExecutor(), bytecode);
  }
  // Local class to capture the final method parameter
  class Itr implements Iterator<ClassFile> {
    ClassFile cls;
    Iterator<ClassFile> iter = allMethodsWithAnnos.iterator();
    @Override
    public boolean hasNext() {
      while(iter.hasNext()) {
        cls = iter.next();
        for (final MethodInfo method : cls.getMethods()) {
          if (allMethodsWithAnnos.scanMethod(method)) {
            for (final Class<?> annotation : annotations) {
              if (method.getAnnotation(annotation.getName()) != null) {
                return true;
              }
            }
          }
        }
      }
      return false;
    }

    @Override
    public ClassFile next() {
      return cls;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  return ()->new Itr();
}

  protected ExecutorService getExecutor() {
    return executor.out1();
  }

  public ClassPool getClassPool() {
    if (pool == null) {
      pool = new ClassPool();
      for (final URL cp : classpath) {
        String asString = cp.toString();
        if (asString.endsWith("/")) {
          asString = asString.substring(0, asString.length()-1);
        }
        if (asString.startsWith("jar:")) {
          asString = asString.substring(4);
        }
        if (asString.startsWith("file:/")) {
          asString = asString.substring(6);
        }
        final int index = asString.indexOf("jar!");
        if (index != -1) {
          asString=asString.substring(0, index+3);
        }
        try {
          pool.appendClassPath(asString);
        } catch (final NotFoundException e) {
          X_Log.warn(getClass(), "Could not find resource "+cp, e);
        }
      }
    }
    return pool;
  }

  public void setClasspath(final Set<URL> keySet) {
    this.classpath = new ArrayList<URL>(keySet);
  }

}


class ResourceTrie <ResourceType extends ClasspathResource>
extends MultithreadedStringTrie<ResourceType> {


  protected class PrioritizedEdge extends Edge {
    private static final long serialVersionUID = 7917481802519184433L;

    public PrioritizedEdge() {
    }

    public PrioritizedEdge(final char[] key, final int index, final int end, final ResourceType value) {
      super(key, index, end);
      this.value = value;
    }

    @Override
    protected void setValue(final ResourceType value) {
      if (this.value != null) {
        if (this.value.priority() > value.priority()) {
          return;
        }
      }
      super.setValue(value);
    }
  }

  @Override
  protected Edge newEdge() {
    return new PrioritizedEdge();
  }

  @Override
  protected Edge newEdge(final char[] key, final int index, final int end, final ResourceType value) {
    return new PrioritizedEdge(key, index, end, value);
  }

}

