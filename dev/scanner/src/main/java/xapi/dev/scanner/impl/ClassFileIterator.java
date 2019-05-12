package xapi.dev.scanner.impl;

import xapi.bytecode.ClassFile;
import xapi.collect.api.HasPrefixed;
import xapi.dev.resource.impl.ByteCodeResource;
import xapi.fu.Filter.Filter1;

import java.util.Iterator;

class ClassFileIterator implements Iterable<ClassFile> {

  private final Filter1<ClassFile> matcher;
  private final HasPrefixed<ByteCodeResource> bytecode;

  private final class Itr implements Iterator<ClassFile> {

    private Iterator<ByteCodeResource> iter = bytecode.findPrefixed("").iterator();
    private ClassFile cls;

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
