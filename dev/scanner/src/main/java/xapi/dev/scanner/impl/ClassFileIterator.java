package xapi.dev.scanner.impl;

import java.util.Iterator;

import xapi.bytecode.ClassFile;
import xapi.collect.api.HasPrefixed;
import xapi.dev.resource.impl.ByteCodeResource;
import xapi.util.api.MatchesValue;

class ClassFileIterator implements Iterable<ClassFile> {

  private final MatchesValue<ClassFile> matcher;
  private final HasPrefixed<ByteCodeResource> bytecode;
  
  private final class Itr implements Iterator<ClassFile> {

    private Iterator<ByteCodeResource> iter = bytecode.findPrefixed("").iterator();
    private ClassFile cls;
    
    @Override
    public boolean hasNext() {
      while(iter.hasNext()) {
        cls = iter.next().getClassData();
        if (matcher.matches(cls)) {
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
  
  ClassFileIterator(MatchesValue<ClassFile> matcher, HasPrefixed<ByteCodeResource> bytecode) {
    assert matcher != null;
    this.matcher = matcher;
    this.bytecode = bytecode;
  }
  

  @Override
  public Iterator<ClassFile> iterator() {
    return new Itr();
  }
}