package xapi.dev.scanner.impl;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import xapi.bytecode.ClassFile;
import xapi.collect.fifo.Fifo;
import xapi.collect.fifo.SimpleFifo;
import xapi.collect.prefixed.HasPrefixed;
import xapi.fu.Filter.Filter1;

/**
 * Finds all classes that have any annotation.
 *
 * If you want more fine-grained search capabilities,
 * override {@link #filter1(ClassFile)}.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
class AnnotatedClassIterator implements Iterable<ClassFile>, Filter1<ClassFile> {

  final Iterator<ClassFile> allClasses;
  Fifo<ClassFile> results = new SimpleFifo<ClassFile>();
  boolean working = true, waiting = false;

  public AnnotatedClassIterator(final ExecutorService executor, final HasPrefixed<ByteCodeResource> bytecode) {
    allClasses = new ClassFileIterator(this, bytecode).iterator();
    executor.submit(new Runnable() {
      @Override
      public void run() {
        try {
          while (allClasses.hasNext()) {
            final ClassFile next = allClasses.next();
            results.give(next);
            if (waiting) {
              synchronized(allClasses) {
                allClasses.notifyAll();
              }
              waiting = false;
            }
          }
        } finally {
          working = false;
        }

        if (waiting) {
          synchronized(allClasses) {
            allClasses.notifyAll();
          }
        }

      }
    });
  }

  class Itr implements Iterator<ClassFile> {

    Iterator<ClassFile> itr = results.forEach().iterator();

    @Override
    public boolean hasNext() {
      while (working) {
        if (itr.hasNext()) {
          return true;
        }
        try {
          synchronized (allClasses) {
            waiting = true;
            allClasses.wait(0, 25000);
          }
        } catch (final InterruptedException e) {
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
  public Boolean io(final ClassFile value) {
    return value.getAnnotations().length > 0;
  }
}
