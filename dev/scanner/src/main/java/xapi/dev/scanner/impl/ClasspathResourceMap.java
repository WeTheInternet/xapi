package xapi.dev.scanner.impl;

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

import xapi.bytecode.ClassFile;
import xapi.bytecode.ClassPool;
import xapi.bytecode.FieldInfo;
import xapi.bytecode.MemberInfo;
import xapi.bytecode.MethodInfo;
import xapi.bytecode.NotFoundException;
import xapi.collect.api.Fifo;
import xapi.collect.impl.MultithreadedStringTrie;
import xapi.collect.impl.SimpleFifo;
import xapi.dev.resource.api.ClasspathResource;
import xapi.dev.resource.impl.ByteCodeResource;
import xapi.dev.resource.impl.SourceCodeResource;
import xapi.dev.resource.impl.StringDataResource;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.util.X_Debug;
import xapi.util.api.MatchesValue;

public class ClasspathResourceMap {

  private final ResourceTrie<ByteCodeResource> bytecode;
  private final ResourceTrie<SourceCodeResource> sources;
  private final ResourceTrie<StringDataResource> resources;
  private final Set<Class<? extends Annotation>> annotations;
  private final Set<Pattern> bytecodeMatchers;
  private final ExecutorService executor;
  private final Set<Pattern> resourceMatchers;
  private final Set<Pattern> sourceMatchers;
  private final Fifo<ByteCodeResource> pending;
  private AnnotatedClassIterator allAnnos;
  private AnnotatedMethodIterator allMethodsWithAnnos;
  private ClassPool pool;
  private ArrayList<URL> classpath;

  public ClasspathResourceMap(ExecutorService executor, Set<Class<? extends Annotation>> annotations,
    Set<Pattern> bytecodeMatchers, Set<Pattern> resourceMatchers, Set<Pattern> sourceMatchers) {
    this.annotations = annotations;
    this.bytecodeMatchers = bytecodeMatchers;
    this.executor = executor;
    this.resourceMatchers = resourceMatchers;
    this.sourceMatchers = sourceMatchers;
    this.bytecode = new ResourceTrie<ByteCodeResource>();
    this.sources = new ResourceTrie<SourceCodeResource>();
    this.resources = new ResourceTrie<StringDataResource>();
    this.pending = new SimpleFifo<ByteCodeResource>();
  }

  public void addBytecode(final String name, final ByteCodeResource bytecode) {
    this.bytecode.put(X_Source.stripClassExtension(name.replace(File.separatorChar, '.')), bytecode);
      if (!preloadClasses())
        return;
      if (pending.isEmpty()) {
        synchronized (pending) {
          // double-checked lock
          if (pending.isEmpty()) {
            executor.submit(new Runnable() {
              // We use one thread to iterate and preload class files
              @Override
              public void run() {
                while (!pending.isEmpty()) {
                  Iterator<ByteCodeResource> iter = pending.iterator();
                  while (iter.hasNext()) {
                    // Preload classes
                    addSubclasses(iter.next().getClassData());
                    iter.remove();
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

  protected void addSubclasses(ClassFile classData) {
    // TODO reenable or delete this
//    if (classData.getEnclosedName().indexOf('.')>-1) {
//      bytecode.put(classData.getQualifiedName(), classData);
//    }
  }

  private boolean preloadClasses() {
    return !annotations.isEmpty() || !bytecodeMatchers.isEmpty();
  }

  protected void accept(String name, ByteCodeResource bytecode, Iterable<Class<? extends Annotation>> classAnnotations) {
    ClassFile classFile = bytecode.getClassData();
    for (Class<? extends Annotation> annoClass : classAnnotations) {
      maybeAccept(name, bytecode, classFile, annoClass);
    }
  }

  protected void maybeAccept(String name, ByteCodeResource bytecode,
      ClassFile classFile, Class<? extends Annotation> annoClass) {
    xapi.bytecode.annotation.Annotation anno = classFile.getAnnotation(annoClass.getName());
    if (anno != null) {
      this.bytecode.put(name, bytecode);
      return;
    }
    // check the target retention of these annotations, and scan methods or fields
    try {
      Target target = annoClass.getAnnotation(Target.class);
      ElementType[] targets;
      if (target == null) {
        targets = getDefaultAnnotationTargets();
      } else {
        targets = target.value();
      }
      for (ElementType type : targets) {
        switch (type) {
          case METHOD:
            for (MethodInfo method : classFile.getMethods()) {
              if (accepts(method, annoClass)) {
                this.bytecode.put(name, bytecode);
                return;
              }
            }
            break;
          case FIELD:
            for (FieldInfo field : classFile.getFields()) {
              if (accepts(field, annoClass)) {
                this.bytecode.put(name, bytecode);
                return;
              }
            }
            break;
          case CONSTRUCTOR:
            for (MethodInfo method : classFile.getMethods()) {
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
    } catch (Throwable e) {
      throw X_Debug.rethrow(e);
    }

  }

  private boolean accepts(MemberInfo method, Class<? extends Annotation> annoClass) {
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

  public void addSourcecode(String name, SourceCodeResource sourcecode) {
    this.sources.put(name, sourcecode);
  }

  public void addResource(String name, StringDataResource resource) {
    this.resources.put(name, resource);
  }

  public boolean includeResource(String name) {
    for (Pattern p : resourceMatchers) {
      if (p.matcher(name).matches())
        return true;
      if (p.matcher(name.substring(name.lastIndexOf('/')+1)).matches())
        return true;
    }
    return false;
  }

  public boolean includeSourcecode(String name) {
    for (Pattern p : sourceMatchers) {
      if (p.matcher(name).matches())
        return true;
    }
    return false;
  }

  public boolean includeBytecode(String name) {
    for (Pattern p : bytecodeMatchers) {
      if (p.matcher(name).matches())
        return true;
    }
    return false;
  }

  public final SourceCodeResource findSource(final String name) {
    return sources.get(name);
  }

  public final Iterable<SourceCodeResource> findSources(final String prefix, final Pattern ... patterns) {
    if (patterns.length == 0)
      return sources.findPrefixed(prefix);
    class Itr implements Iterator<SourceCodeResource> {
      SourceCodeResource cls;
      Iterator<SourceCodeResource> iter = sources.findPrefixed(prefix).iterator();
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next();
          for (Pattern pattern : patterns) {
            if (pattern.matcher(cls.getResourceName()).matches())
              return true;
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
    return new Iterable<SourceCodeResource>() {
      @Override
      public Iterator<SourceCodeResource> iterator() {
        return new Itr();
      }
    };
  }

  public final StringDataResource findResource(final String name) {
    return resources.get(name);
  }

  public final Iterable<StringDataResource> findResources(final String prefix, final Pattern ... patterns) {
    if (patterns.length == 0)
      return resources.findPrefixed(prefix);
    class Itr implements Iterator<StringDataResource> {
      StringDataResource cls;
      Iterator<StringDataResource> iter = resources.findPrefixed(prefix).iterator();
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next();
          for (Pattern pattern : patterns) {
            if (pattern.matcher(cls.getResourceName()).matches())
              return true;
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
    return new Iterable<StringDataResource>() {
      @Override
      public Iterator<StringDataResource> iterator() {
        return new Itr();
      }
    };
  }

  public final ClassFile findClass(String clsName) {
    clsName = clsName.replace('/', '.');
    ByteCodeResource resource = bytecode.get(clsName);
    return resource == null ? null : resource.getClassData();
  }

  @SuppressWarnings("unchecked")
  public final Iterable<ClassFile> getAllClasses(){
    return new ClassFileIterator(MatchesValue.ANY, bytecode);
  }

  public Iterable<ClassFile> findClassesInPackage(final String name) {
    return new ClassFileIterator(new MatchesValue<ClassFile>() {
      @Override
      public boolean matches(ClassFile value) {
        return !"package-info".equals(value.getEnclosedName()) && value.getPackage().equals(name);
      }
    }, bytecode);
  }

  public Iterable<ClassFile> findClassesBelowPackage(final String name) {
    return new ClassFileIterator(new MatchesValue<ClassFile>() {
      @Override
      public boolean matches(ClassFile value) {
        return !"package-info".equals(value.getEnclosedName()) && value.getPackage().startsWith(name);
      }
    }, bytecode);
  }

  public Iterable<ClassFile> findPackagesBelowPackage(final String name) {
    return new ClassFileIterator(new MatchesValue<ClassFile>() {
      @Override
      public boolean matches(ClassFile value) {
        return "package-info".equals(value.getEnclosedName()) && value.getPackage().startsWith(name+".");
      }
    }, bytecode);
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
          if (matcher.matches(cls))
            return true;
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

  public final Iterable<ClassFile> findImplementationOf(
    final Class<?> ... superClasses) {
    return findImplementationOf(X_Source.toStringBinary(superClasses));
  }
  public final Iterable<ClassFile> findImplementationOf(
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
          if (matcher.matches(cls))
            return true;
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

  public final Iterable<ClassFile> findClassAnnotatedWith(
     @SuppressWarnings("unchecked")
     final Class<? extends Annotation> ... annotations) {
    if (allAnnos == null) {
      allAnnos = new AnnotatedClassIterator(executor, bytecode);
    }
    // Local class to capture the final method parameter
    class Itr implements Iterator<ClassFile> {
      ClassFile cls;
      Iterator<ClassFile> iter = allAnnos.iterator();
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next();
          for (Class<?> annotation : annotations) {
            if (cls.getAnnotation(annotation.getName())!=null)
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


public final Iterable<ClassFile> findClassWithAnnotatedMethods(
    @SuppressWarnings("unchecked")
    final Class<? extends Annotation> ... annotations) {
  if (allMethodsWithAnnos == null) {
    allMethodsWithAnnos = new AnnotatedMethodIterator(executor, bytecode);
  }
  // Local class to capture the final method parameter
  class Itr implements Iterator<ClassFile> {
    ClassFile cls;
    Iterator<ClassFile> iter = allMethodsWithAnnos.iterator();
    @Override
    public boolean hasNext() {
      while(iter.hasNext()) {
        cls = iter.next();
        for (MethodInfo method : cls.getMethods()) {
          if (allMethodsWithAnnos.scanMethod(method)) {
            for (Class<?> annotation : annotations) {
              if (method.getAnnotation(annotation.getName()) != null)
                return true;
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
  return new Iterable<ClassFile>() {
    @Override
    public Iterator<ClassFile> iterator() {
      preloadClasses();
      return new Itr();
    }
  };
}

  public ClassPool getClassPool() {
    if (pool == null) {
      pool = new ClassPool();
      for (URL cp : classpath) {
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
        int index = asString.indexOf("jar!");
        if (index != -1) {
          asString=asString.substring(0, index+3);
        }
        try {
          pool.appendClassPath(asString);
        } catch (NotFoundException e) {
          X_Log.warn(getClass(), "Could not find resource "+cp, e);
        }
      }
    }
    return pool;
  }

  public void setClasspath(Set<URL> keySet) {
    this.classpath = new ArrayList<URL>(keySet);
  }

}


class ResourceTrie <ResourceType extends ClasspathResource>
extends MultithreadedStringTrie<ResourceType> {


  protected class PrioritizedEdge extends Edge {
    private static final long serialVersionUID = 7917481802519184433L;

    public PrioritizedEdge() {
    }

    public PrioritizedEdge(char[] key, int index, int end, ResourceType value) {
      super(key, index, end);
      this.value = value;
    }

    @Override
    protected void setValue(ResourceType value) {
      if (this.value != null) {
        if (this.value.priority() > value.priority())
          return;
      }
      super.setValue(value);
    }
  }

  @Override
  protected Edge newEdge() {
    return new PrioritizedEdge();
  }

  @Override
  protected Edge newEdge(char[] key, int index, int end, ResourceType value) {
    return new PrioritizedEdge(key, index, end, value);
  }
}

