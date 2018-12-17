package xapi.util.impl;

import xapi.fu.has.HasId;

public class StringId implements HasId {

  private String id;

  public StringId(String id) {
    assert id != null;
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || id.equals(((HasId)obj).getId());
  }

}
