package xapi.ui.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.util.X_Debug;
import xapi.util.X_String;
import xapi.util.api.ReceivesValue;
import xapi.util.impl.DeferredCharSequence;

import static xapi.collect.X_Collect.newStringMap;

import javax.inject.Provider;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class NodeBuilder<E> implements Widget<E> {

  protected static final String EMPTY = "";

  private CharSequence buffer;
  protected NodeBuilder<E> children;
  protected NodeBuilder<E> siblings;
  protected E el;
  protected Provider<E> elementProvider;
  @SuppressWarnings("rawtypes")
  protected NodeBuilder childTarget = this;

  protected final IntTo<ReceivesValue<E>> createdCallbacks;
  protected final boolean searchableChildren;

  protected Function<String, String> bodySanitizer;
  protected BiFunction<String, Boolean, NodeBuilder<E>> newNode;

  protected NodeBuilder() {
    this(false);
  }

  public NodeBuilder(boolean searchableChildren) {
    this.searchableChildren = searchableChildren;
    createdCallbacks = X_Collect.newList(ReceivesValue.class);
  }

  public boolean hasSiblings() {
    return siblings != null;
  }

  public class ClassnameBuilder extends AttributeBuilder {

    public ClassnameBuilder() {
      super("class", true);
    }

    @Override
    public <C extends NodeBuilder<String>> C addChild(C child) {
      return addToSet(child, " "); // classname is a space-separated list of classname tokens
      // TODO: also send a validator for the classnames
    }

  }

  protected AttributeBuilder newAttributeBuilder(CharSequence attr) {
    return new AttributeBuilder(attr);
  }

  protected static class AttributeBuilder extends NodeBuilder<String> {

    private final StringTo<String> existing;

    protected AttributeBuilder(CharSequence value) {
      this(value, false);
    }

    protected AttributeBuilder(CharSequence value, boolean searchableChildren) {
      super(searchableChildren);
      existing = newStringMap(String.class);
      append(value);
    }


    @Override
    public void append(Widget<String> child) {
      append(child.getElement());
    }

    @Override
    protected String create(CharSequence node) {
      return node.toString();
    }

    @Override
    protected NodeBuilder<String> wrapChars(CharSequence body) {
      return newAttributeBuilder(body);
    }

    @Override
    protected BiFunction<String, Boolean, NodeBuilder<String>> getCreator() {
      Function<CharSequence, AttributeBuilder> ctor = AttributeBuilder::new;
      // ignore the searchable attribute for now.
      return (str, searchable)->ctor.apply(str);
    }

    @Override
    public <C extends NodeBuilder<String>> C addChild(C child) {
      String value = child.getElement();
      final NodeBuilder<String> wrapped = wrapChars(value);
      super.addChild(wrapped);
      return child;
    }

    public <C extends NodeBuilder<String>> C addToSet(C child,
                                                      String separator
    ) {
      return addToSet(child,
          (c, txt)->(separator+txt+separator).split(separator), // take the list apart
          ()->separator // put it back together.
          );
    }

    public <C extends NodeBuilder<String>> C addToSet(C child,
                                                      BiFunction<C, CharSequence, String[]> splitter,
                                                      Supplier<CharSequence> joinChars
    ) {
      // The api of the bifunction demands that we figure out the string value from the element type we are working with.
      String value = child.getElement();
      // Since we are a string type, this seems pretty convoluted,
      // but when working on live elements and nodes, this lets it see the object and the to string'd version at once.
      for (String part : splitter.apply(child, value)) {
        if (part.length() > 0) {
          if (existing.put(part, part) == null) {
            if (existing.size() > 1) {
              part = part + joinChars.get();
            }
            super.addChild(wrapChars(part));
          }
        }
      }
      return child;
    }


  }

  public <C extends NodeBuilder<E>> C addChild(C child) {
    return addChild(child, false);
  }

  @SuppressWarnings("unchecked")
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
    if (el == null) {
      // MAYBE: double checked lock for jvms?
      if (elementProvider != null) {
        el = elementProvider.get();
      }
      if (el == null) {
        initialize();
      } else {
        elementProvider = null;
        onInitialize(el);
      }
    }
    return el;
  }

  public NodeBuilder<E> onCreated(ReceivesValue<E> callback) {
    if (el == null) {
      assert searchableChildren : "Cannot handle created callbacks without searchableChildren in "+this;
      createdCallbacks.add(callback);
      if (elementProvider != null) {
        getElement(); // will trigger our callback
      }
    } else {
      callback.set(el);
    }
    return this;
  }

  private final E initialize() {
    StringBuilder b = new StringBuilder();
    toHtml(b);
    final String html = b.toString();
    if (X_String.isEmpty(html)) {
      return null;
    }
    el = create(html);
    startInitialize(el);
    try {
        if (!searchableChildren) {
          return el;
        }
        boolean initChildren = false;
        if (children != null) {
          initChildren = children.resolveChildren(el);
        }
        onInitialize(el);
        if (initChildren) {
          children.onInitialize(children.getElement());
        }
    } finally {
      finishInitialize(el);
    }
    return el;
  }

  protected void finishInitialize(E el) {}

  protected void startInitialize(E el) {}

  protected void onInitialize(E el) {
    if (!createdCallbacks.isEmpty()) {
      final ReceivesValue<E>[] callbacks = createdCallbacks.toArray();
      createdCallbacks.clear();
      for (ReceivesValue<E> callback : callbacks) {
        callback.set(el);
      }
      assert createdCallbacks.isEmpty() :
          "You are adding more created callbacks during onInitialize.";
      // TODO consider draining the callbacks for a few iterations
    }
  }

  private boolean resolveChildren(E root) {
    boolean needsCallback = false;
    if (children != null) {
      if (children.resolveChildren(root)) {
        onCreated(
            new ReceivesValue<E>() {
              @Override
              public void set(E value) {
                children.onInitialize(children.getElement());
              }
            }
        );
        needsCallback = true;
      }
    }
    if (siblings != null) {
      if (siblings.resolveChildren(root)) {
        onCreated(
            new ReceivesValue<E>() {
              @Override
              public void set(E value) {
                siblings.onInitialize(siblings.getElement());
              }
            }
        );
        needsCallback = true;
      }
    }
    resolve(root);
    return needsCallback || !createdCallbacks.isEmpty();
  }

  protected void resolve(final E root) {
    if (this.el == null) {
      // We need to resolve our element!
      elementProvider = new Provider<E>() {
        @Override
        public E get() {
          return findSelf(root);
        }
      };
    }
  }

  protected E findSelf(E root) {
    throw new UnsupportedOperationException("NodeBuilder " + getClass() + " does not support nested .getElement()");
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

  protected boolean isTagEmpty() {
    return isChildrenEmpty();
  }

  public void cleanup() {
    if (children != null) {
      children.cleanup();
      children = null;
    }
    if (siblings != null) {
      siblings.cleanup();
      siblings = null;
    }
    this.buffer = "";
    this.el = null;
    this.elementProvider = null;
    this.childTarget = null;
  }

  public NodeBuilder<E> createChild(String value) {
    return wrapChars(value);
  }

  public final void setNodeFactory(BiFunction<String, Boolean, NodeBuilder<E>> factory) {
    setNodeFactory(factory, false);
  }
  public void setBodySanitizer(Function<String, String> bodySanitizer) {
    this.bodySanitizer = bodySanitizer;
  }

  public void setNodeFactory(BiFunction<String, Boolean, NodeBuilder<E>> factory, boolean shareFactories) {
    if (shareFactories) {
      this.newNode = factory.andThen(this::shareFactories);
    } else {
      this.newNode = factory;
    }
  }

  protected NodeBuilder<E> shareFactories(NodeBuilder<E> with) {
    // Make sure we pass along our factories to our children!
    // We expose this method so that it is reusable and optional,
    // so custom behaviors can choose whether to opt in to factory sharing or not.
    // Scenarios where nodes set different factories preclude sharing, thus, it is an optional operation.
    with.setBodySanitizer(bodySanitizer);
    with.setNodeFactory(newNode);
    return with;
  }

  protected void setDefaultFactories() {
    setBodySanitizer(html -> html.replaceAll("\n", "<br/>"));
    setNodeFactory(getCreator(), true);
  }

  protected abstract BiFunction<String, Boolean, NodeBuilder<E>> getCreator();


  public boolean isEmpty() {
    return this.children.isEmpty() && this.siblings.isEmpty();
  }
}
