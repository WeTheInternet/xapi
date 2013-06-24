package xapi.source.impl;

import java.util.Iterator;

import xapi.source.api.IsMember;
import xapi.source.api.IsType;

public class DeclaredMemberFilter <Member extends IsMember> implements Iterable<Member>  {

  private final Iterable<Member> iter;
  private final IsType owner;

  private final class Itr implements Iterator<Member> {
    Iterator<Member> i = iter.iterator();
    Member next;
    @Override
    public boolean hasNext() {
      if (next != null)
        return true;
      while (i.hasNext()) {
        next = i.next();
        if (next.getEnclosingType().getQualifiedName().equals(owner.getQualifiedName()))
          return true;
      }
      next = null;
      return false;
    }
    @Override
    public Member next() {
      try {
        return next;
      } finally {
        next = null;
      }
    }
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  
  public DeclaredMemberFilter(Iterable<Member> iter, IsType owner) {
    this.iter = iter;
    this.owner = owner;
  }
  
  @Override
  public Iterator<Member> iterator() {
    return new Itr();
  }

}
