package xapi.dev.scanner;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import xapi.bytecode.ClassFile;
import xapi.collect.impl.MultithreadedStringTrie;
import xapi.util.api.MatchesValue;
import xapi.util.api.Pointer;

public class ClasspathResourceMap {

  private final ResourceTrie<ByteCodeResource> bytecode;
  private final ResourceTrie<SourceCodeResource> sources;
  private final ResourceTrie<StringDataResource> resources;
  private final Set<Class<? extends Annotation>> annotations;
  private final Set<Pattern> bytecodeMatchers;
  private final ExecutorService executor;
  private final Set<Pattern> resourceMatchers;
  private final Set<Pattern> sourceMatchers;
  private final Queue<Future<?>> pending;

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
    this.pending = new ConcurrentLinkedQueue<Future<?>>();
  }

  public void addBytecode(final String name, final ByteCodeResource bytecode) {
    // if annotations are specified, we must peek at the bytecode before adding
    if (annotations.size() == 0) {
      this.bytecode.put(name, bytecode);
    } else {
      final Pointer<Future<?>> future = new Pointer<Future<?>>();
      future.set(
      executor.submit(new Runnable() {
        @Override
        public void run() {
          try {
            accept(name, bytecode, annotations);
          }finally {
            pending.remove(future.remove());
          }
        }
      }));
      pending.add(future.get());
    }
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
    flush();
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
  
  public final Iterable<ClassFile> findClassesImplementing(
      final Class<?> ... interfaces) {
    // Make sure all pending tasks are done before a read succeeds
    flush();
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

  public final Iterable<ClassFile> findClassAnnotatedWith(
     @SuppressWarnings("unchecked")
     final Class<? extends Annotation> ... annotations) {
    // Make sure all pending tasks are done before a read succeeds
    flush();
    // Local class to capture the final method parameter
    class Itr implements Iterator<ClassFile> {
      ClassFile cls;
      Iterator<ByteCodeResource> iter = bytecode.findPrefixed("").iterator();
      @Override
      public boolean hasNext() {
        while(iter.hasNext()) {
          cls = iter.next().getClassData();
          for (Class<?> annotation : annotations) {
            //TODO lookup the annotation's target and check fields / methods as well
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

  private void flush() {
    if (pending.isEmpty())return;
    long deadline = System.currentTimeMillis()+10000;
    while (deadline > System.currentTimeMillis()&&!pending.isEmpty()) {
      Iterator<Future<?>> iter = pending.iterator();
      while (iter.hasNext()) {
        if (iter.next().isDone())
          iter.remove();
      }
      try {
        Thread.sleep(0, 100);
      }catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
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

