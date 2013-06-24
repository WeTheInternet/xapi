package xapi.dev.scanner;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import xapi.bytecode.ClassFile;
import xapi.collect.api.Fifo;
import xapi.collect.impl.MultithreadedStringTrie;
import xapi.collect.impl.SimpleFifo;
import xapi.log.X_Log;
import xapi.source.X_Source;
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
                    iter.next().getClassData();
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

  private boolean preloadClasses() {
    return !annotations.isEmpty() || !bytecodeMatchers.isEmpty();
  }

  protected void accept(String name, ByteCodeResource bytecode, Set<Class<? extends Annotation>> annotations) {
    ClassFile classFile = bytecode.getClassData();
    for (Class<?> cls : annotations) {
      // TODO: check the target of these annotations, and scan methods or fields
      if (classFile.getAnnotation(cls.getName()) != null) {
        this.bytecode.put(name, bytecode);
        return;
      }
    }
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

  public final Iterable<StringDataResource> findResources(final String prefix, final Pattern ... patterns) {
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

  class ClassFileIterator implements Iterator<ClassFile>, Iterable<ClassFile> {

    private MatchesValue<ClassFile> matcher;
    
    ClassFileIterator(MatchesValue<ClassFile> matcher) {
      assert matcher != null;
      this.matcher = matcher;
    }
    
    private ClassFile cls;
    private Iterator<ByteCodeResource> iter;
    
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

    @Override
    public Iterator<ClassFile> iterator() {
      ClassFileIterator live = new ClassFileIterator(matcher);
      live.iter = bytecode.findPrefixed("").iterator();
      return live;
    }
  }
  
  class AnnotatedClassIterator implements Iterable<ClassFile>, MatchesValue<ClassFile> {
    
    Iterator<ClassFile> allClasses = new ClassFileIterator(this).iterator();
    Fifo<ClassFile> results = new SimpleFifo<ClassFile>();
    boolean working = true;
    {
      executor.submit(new Runnable() {
        @Override
        public void run() {
          while (allClasses.hasNext())
            results.give(allClasses.next());
          working = false;
        }
      });
    }
    class Itr implements Iterator<ClassFile> {

      Iterator<ClassFile> itr = results.forEach().iterator();
      
      @Override
      public boolean hasNext() {
        while (working) {
          if (itr.hasNext())
            return true;
          try {
            Thread.sleep(0, 500);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
        return itr.hasNext();
      }

      @Override
      public ClassFile next() {
        return itr.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
    
    @Override
    public Iterator<ClassFile> iterator() {
      return new Itr();
    }

    @Override
    public boolean matches(ClassFile value) {
      return value.getAnnotations().length > 0;
    }
  }
  
  public final ClassFile findClass(String clsName) {
    ByteCodeResource resource = bytecode.get(clsName);
    return resource == null ? null : resource.getClassData();
  }
  
  @SuppressWarnings("unchecked")
  public final Iterable<ClassFile> getAllClasses(){
    return new ClassFileIterator(MatchesValue.ANY);
  }
  
  public final Iterable<ClassFile> findClassesImplementing(
      final Class<?> ... interfaces) {
    // Local class to capture the final method parameter
    class Itr implements Iterator<ClassFile> {
      ClassFile cls;
      Iterator<ByteCodeResource> iter = bytecode.findPrefixed("").iterator();
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next().getClassData();
          for (Class<?> iface : interfaces) {
            //TODO lookup the annotation's target and check fields / methods as well
            for (String clsIface : cls.getInterfaces()){
              if (iface.getCanonicalName().equals(clsIface))
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
    return new Iterable<ClassFile>() {
      @Override
      public Iterator<ClassFile> iterator() {
        return new Itr();
      }
    };
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
    // Local class to capture the final method parameter
    class Itr implements Iterator<ClassFile> {
      ClassFile cls;
      Iterator<ByteCodeResource> iter = bytecode.findPrefixed("").iterator();
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next().getClassData();
          for (Class<?> iface : superClasses) {
            if (cls.getSuperclass().equals(iface.getCanonicalName())) {
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
      allAnnos = new AnnotatedClassIterator();
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
    return new Iterable<ClassFile>() {
      @Override
      public Iterator<ClassFile> iterator() {
        return new Itr();
      }
    };
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

