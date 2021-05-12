package xapi.dev.scanner.impl;

import xapi.bytecode.ClassFile;
import xapi.collect.prefixed.HasPrefixed;
import xapi.fu.Filter.Filter1;
import xapi.fu.itr.MappedIterable;

import java.util.Iterator;

class ClassFileIterator implements MappedIterable<ClassFile> {

  private final Filter1<ClassFile> matcher;
  private final HasPrefixed<ByteCodeResource> bytecode;

  private final class Itr implements Iterator<ClassFile> {

    private Iterator<ByteCodeResource> iter = bytecode.findPrefixed("").iterator();
    private ClassFile cls;

    @Override
    public boolean hasNext() {
      while(iter.hasNext()) {
        final ByteCodeResource next = iter.next();
        if (consider(next)) {
          cls = next.getClassData();
          if (matcher.filter1(cls)) {
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

  protected boolean consider(ByteCodeResource next) {
    return !"module-info.class".equals(next.getResourceName());
  }

  ClassFileIterator(Filter1<ClassFile> matcher, HasPrefixed<ByteCodeResource> bytecode) {
    assert matcher != null;
    this.matcher = matcher;
    this.bytecode = bytecode;
  }


  @Override
  public Iterator<ClassFile> iterator() {
    return new Itr();
  }
}
