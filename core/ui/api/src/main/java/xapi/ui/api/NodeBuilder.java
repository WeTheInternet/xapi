package xapi.ui.api;

import java.io.IOException;

import xapi.util.X_Debug;
import xapi.util.impl.DeferredCharSequence;

public abstract class NodeBuilder <E> implements Widget <E> {
  protected static final String EMPTY = "";

  private CharSequence buffer;
  protected NodeBuilder<E> children;
  protected NodeBuilder<E> siblings;
  protected E el;
  private NodeBuilder childTarget = this;

  public <C extends NodeBuilder<E>> C addChild(C child) {
    return addChild(child, false);
  }

  @SuppressWarnings("unchecked" )
  public <C extends NodeBuilder<E>> C addChild(C child, boolean makeTarget) {
    if (buffer != null) {
      CharSequence was = buffer;
      buffer = null;
      // Tear off the current set of char sequences as a child node
      addChild0(wrapChars(was));
    }
    childTarget.addChild0(child);
    if (makeTarget) {
      childTarget = child;
    }
    return child;
  }

  private <C extends NodeBuilder<E>> void addChild0(C child) {
    if (children == null) {
      children = child;
    } else {
      assert children != child;
      children.addSibling(child);
    }
  }

  public void clearChildren() {
    buffer = null;
    children = null;
  }

  public void clearAll() {
    clearChildren();
    siblings = null;
    el = null;
  }

  public <C extends NodeBuilder<E>> C addSibling(C sibling) {
    if (siblings == null) {
      siblings = sibling;
    } else {
      siblings.addSibling(sibling);
    }
    return sibling;
  }

  public NodeBuilder<E> append(CharSequence chars) {
    if (children == null) {
      if (buffer == null) {
        buffer = chars;
      } else {
        buffer = join(buffer, chars);
      }
    } else {
      if (chars != null && chars != EMPTY) {
        addChild0(wrapChars(chars));
      }
    }
    return this;
  }

  protected abstract E create(CharSequence node);

  protected CharSequence getCharsAfter(CharSequence self) {
    return EMPTY;
  }

  protected CharSequence getCharsBefore() {
    return EMPTY;
  }

  public E getElement() {
    return el == null ? initialize() : el;
  }

  private final E initialize() {
    StringBuilder b = new StringBuilder();
    toHtml(b);
    el = create(b.toString());
    return el;
  }

  protected CharSequence join(final CharSequence body, final CharSequence chars) {
    return new DeferredCharSequence<E>(body, chars);
  }

  protected void toHtml(Appendable out) {
    CharSequence chars = getCharsBefore();
    boolean printChars = chars != null && chars != EMPTY;
    if (printChars) {
      print(out, chars);
    }
    if (children == null) {
      if (buffer != null && buffer != EMPTY) {
        print(out, buffer);
      }
    } else {
      if (buffer != null && buffer != EMPTY) {
        addChild0(wrapChars(buffer));
      }
      children.toHtml(out);
    }
    if (printChars) {
      CharSequence after = getCharsAfter(chars);
      if (after != null && after != EMPTY) {
        print(out, after);
      }
    }
    if (siblings != null) {
      siblings.toHtml(out);
    }
  }

  protected void print(Appendable out, CharSequence was) {
    try {
      out.append(was);
    } catch (IOException e) {
      throw X_Debug.rethrow(e);
    }
  }

  protected abstract NodeBuilder<E> wrapChars(CharSequence body);

  protected boolean isChildrenEmpty() {
    return children == null && buffer == null;
  }
}