package xapi.collect.impl;

public class ToStringFifo <E> extends SimpleFifo<E>{

  private static final long serialVersionUID = -5287110430519501818L;

  private String sep = ", ";
  
  public void setSeparator(String sep) {
    this.sep = sep;
  }
  
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    String s = "";
    for (E e : forEach()) {
      b.append(s).append(e);
      s = sep;
    }
    return b.toString();
  }
  
}
