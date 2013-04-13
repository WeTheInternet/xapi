package xapi.source.impl;

import xapi.source.api.IsType;

public class Varargs {

  private IsType[] types;

  public Varargs(IsType ... types) {
    this.types = types;
  }

  public void addType(IsType ... newtypes) {
    IsType[] grow = new IsType[types.length+newtypes.length];
    System.arraycopy(types, 0, grow, 0, types.length);
    System.arraycopy(newtypes, 0, grow, types.length, newtypes.length);
    types = grow;
    grow = null;
  }

  public IsType[] getTypes() {
    return types;
  }

  @Override
  public String toString() {
    if (types.length==0)return "";
    StringBuilder b = new StringBuilder();
    b.append(format(types[0]));
    for (int i = 1;i < types.length; i++) {
      b.append(separator());
      b.append(format(types[i]));
    }
    return b.toString();
  }

  protected String format(IsType isType) {
    return String.valueOf(isType);
  }

  private String separator() {
    return ", ";
  }

}
