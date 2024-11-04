package xapi.gwt.collect;

import java.util.Iterator;

import xapi.collect.fifo.Fifo;

import com.google.gwt.core.client.JavaScriptObject;

public class JsFifo<E> extends JavaScriptObject implements Fifo<E> {

  protected JsFifo() {
  }

  public native static <E> Fifo<E> newFifo()
  /*-{
    return [];
  }-*/;

  @Override
  public final native void clear()
  /*-{
    this.length = 0;
  }-*/;

  @Override
  public final boolean contains(E item) {
    for (E e : forEach()) {
      if (e.equals(item))
        return true;
    }
    return false;
  }

  @Override
  public final native Fifo<E> give(E item)
  /*-{
    if (item !== null)
      this.push(item);
    return this;
  }-*/;
  
  @Override
  @SuppressWarnings("unchecked")
  public final native Fifo<E> giveAll(E... elements)
  /*-{
    this.splice(this.length, elements);
    return this;
  }-*/;

  @Override
  public final Fifo<E> giveAll(Iterable<E> elements) {
    return null;
  }

  @Override
  public final native boolean isEmpty()
  /*-{
    return this.length==0;
  }-*/;

  @Override
  public final Iterator<E> iterator() {
    return new JsArrayIterator<E>(this);
  }

  @Override
  public final Iterable<E> forEach() {
    class Itr implements Iterable<E> {
      @Override
      public Iterator<E> iterator() {
        return JsFifo.this.iterator();
      }
    }
    return new Itr();
  }

  @Override
  public final boolean remove(E item) {
    boolean removed = false;
    for (int i = size(); i-- > 0;) {
      if (equal(item, i)) {
        remove(i);
        removed = true;
      }
    }
    return removed;
  }

  @Override
  public final native int size()
  /*-{
    return this.length;
  }-*/;

  @Override
  public final native E take()
  /*-{
    return this.length==0?null:this.shift();
  }-*/;

  private final native void remove(int i)
  /*-{
    this.splice(i, 1);
  }-*/;

  private final native boolean equal(E item, int i)
  /*-{
    return this[i] === item;
  }-*/;

  public final native String join(String delim)
  /*-{
    return this.join(delim);
  }-*/;

}
