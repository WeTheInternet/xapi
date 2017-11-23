package xapi.elemental.api;

import elemental2.core.Function;
import elemental2.core.JsObject;
import elemental2.core.ObjectPropertyDescriptor;
import elemental2.core.Reflect;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.HTMLAnchorElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLLabelElement;
import elemental2.dom.Node;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import xapi.annotation.inject.InstanceDefault;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.ui.api.ElementInjector;
import xapi.ui.api.ElementPosition;
import xapi.ui.impl.AbstractUiElement;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@InstanceDefault(implFor = UiElementGwt.class)
public class UiElementGwt<E extends HTMLElement>
    extends AbstractUiElement<Node, E, UiElementGwt<?>> {


  private static final String MEMOIZE_KEY = "xapi-element";
  private static final JsLazyExpando<Element, UiElementGwt> expando = new JsLazyExpando<>(MEMOIZE_KEY);
  protected static Function insertAdjacentElement = Js.uncheckedCast(
      Reflect.get(
          Js.uncheckedCast(htmlElementPrototype()),
          "insertAdjacentElement")
  );

  static class JsLazyExpando<I, O> implements In2Out1<I, In1Out1<I, O>, O> {

    private final Object name;

    /**
     * An enumerable, read-only property factory.
     * If you wish to create a non-enumerable, read-only factory,
     * use the constructor which takes a {@link Symbol} as an argument.
     */
    public JsLazyExpando(String name) {
      assert name != null;
      this.name = name;
    }

//    /**
//     * An non-enumerable, read-only property factory.
//     * If you wish to create an enumerable, read-only factory,
//     * use the constructor which takes a String as an argument.
//     */
//    public JsLazyExpando(Symbol name) {
//      assert name != null;
//      this.name = name;
//    }

    public I setValue(I on, O val) {
      ObjectPropertyDescriptor props = new ObjectPropertyDescriptor();
      props.value = val;
      props.configurable = false;
      //    props.set(false);
      // strings will be enumerable; symbols won't
      props.enumerable = name instanceof String;


//      JsObject.defineProperty(Js.cast(on), name, Js.cast(props));
      return on;
    }

    public boolean isDefined(I on) {
      if (on == null) {
        return false;
      }
//      if (name instanceof String) {
//        return X_Gwt.getUnsafe()Reflect.has(Js.uncheckedCast(on), (String) name);
//      } else {
//        return JsSupport.exists(on, (Symbol) name);
//      }
      return true;
    }

    public O getValue(I on) {
      O value;
      if (name instanceof String) {
//        value = (O) JsSupport.getObject(on, (String) name);
      } else {
//        value = (O) JsSupport.getObject(on, (Symbol) name);
      }
      return null;//value;
    }

    private boolean reentrant;

    @Override
    public O io(I el, In1Out1<I, O> io) {
      //    if (!reentrant && isDefined(el)) {
      //      reentrant = true;
      //      try {
      //        return getValue(el);
      //      } finally {
      //        reentrant = false;
      //      }
      //    }
      if (isDefined(el)) {
        return getValue(el);
      }
      O created = io.io(el);
      if (!isDefined(el)) {
        setValue(el, created);
      }
      return created;
    }

    /**
     * Attaches an object factory to the prototype of a javascript class.
     * The property on the prototype will be immutable (no set() function),
     * but configurable (so we can overwrite it with an immutable, non-configurable property).
     *
     * This allows us to apply run-once, lazy initialization of fields on the js class.
     */
    public void addToPrototype(Object prototype, In1Out1<I, O> factory) {
      addToPrototype(prototype, factory, true);
    }
    public void addToPrototype(Object prototype, In1Out1<I, O> factory, boolean makeImmutable) {
      ObjectPropertyDescriptor opts = new ObjectPropertyDescriptor();
      opts.enumerable = name instanceof String;
      // We want the prototype to be configurable so we are allowed to overwrite it later.
      opts.configurable = true;
      final Out1<O> task = new Out1<O>() {

        private native I getCaller()
        /*-{
          // super-cheating... we set this on the Out1 object so we can extract the original
          // caller of the js method; for example, functions attached to custom element elements.
          var caller = this.__caller__;
          delete this.__caller__; // should only ever need this reference once.
          return caller;
        }-*/;

        @Override
        public O out1() {
          //          if (makeImmutable && result != null) {
          //            // somebody grabbed the raw accessor that we are redefining...
          //            return result;
          //          }
          final I self = getCaller();
          X_Log.info(JsLazyExpando.class, "Applying lazy expando; ", this, self);
          final O result = io(self, factory);
          if (makeImmutable && !isDefined(self)) {
            opts.configurable = false;
            opts.get = ()->result;
            // TODO: fix externs to allow String|Symbol
            JsObject.defineProperty(Js.uncheckedCast(self), (String)name, Js.uncheckedCast(opts));
          }
          return result;
        }
      };
      // apply our Out1 as the getter on the prototype, ensuring this is passed through to our Out1
      opts.get = Js.cast(reapplyThis(wrapOut1(task)));

      // apply the descriptor; this attaches to the prototype so each new instance can call our getter
      // which will, if set to be immutable, cement the value onto that instance, making it unwritable and unconfigurable
      JsObject.defineProperty(Js.uncheckedCast(prototype), (String)name, Js.uncheckedCast(opts));
    }
  }

  public static native <T> JavaScriptObject wrapOut1(Out1<T> task)
	/*-{
          return @UiElementGwt::maybeEnter(*)(function(){
            task.__caller__ = this;
            return task.@Out1::out1()();
          });
        }-*/;

  public static native JavaScriptObject reapplyThis(JavaScriptObject f)
        /*-{
          return function() {
            return f.apply(this, [this].concat(Array.prototype.slice.apply(arguments)));
          };
        }-*/;



  public static native JavaScriptObject maybeEnter(JavaScriptObject func)
    /*-{
      return function() {
        if (@com.google.gwt.core.client.impl.Impl::entryDepth) {
          // we are already inside the gwt call stack, just skip the expensive wrapping.
          return func.apply(this, arguments) || null;
        } else {
          return @com.google.gwt.core.client.impl.Impl::entry0(*)(func, this, arguments) || null;
        }
      };
    }-*/;



  public static native JavaScriptObject htmlElementPrototype()
  /*-{
    return Object.create($wnd.HTMLElement.prototype);
  }-*/;


  public UiElementGwt() {
    super(UiElementGwt.class);
  }


  public static HTMLDivElement newDiv() {
    return Js.uncheckedCast(DomGlobal.document.createElement("div"));
  }

  public static HTMLLabelElement newLabel() {
    return Js.uncheckedCast(DomGlobal.document.createElement("label"));
  }

  public static HTMLAnchorElement newAnchor() {
    return Js.uncheckedCast(DomGlobal.document.createElement("a"));
  }

  public static UiElementGwt<?> fromWeb(Element element) {
    final UiElementGwt result = expando.io(element, e -> {
      final UiElementGwt newEl = newUiElement(e);
      newEl.setElement(element);
      return newEl;
    });
    return result;
  }

  private static UiElementGwt<?> newUiElement(Element e) {
    // TODO: have registered providers per tag name
    return X_Inject.instance(UiElementGwt.class);
  }

  @Override
  public void appendChild(UiElementGwt<?> newChild) {
    newChild.setParent(this);
    final E e = getElement();
    final Element c = newChild.getElement();
    e.appendChild(c);
  }

  @Override
  public void removeChild(UiElementGwt<?> child) {
    assert child.getParent() == this;
    child.setParent(null);
    final E e = getElement();
    final Element c = child.getElement();
    e.appendChild(c);
  }

  @Override
  public String toSource() {
    return (String) JsPropertyMap.of(getElement()).get("outerHTML");
  }

  @Override
  public void insertAdjacent(ElementPosition pos, UiElementGwt<?> child) {
    final E e = getElement();
    final Element c = child.getElement();
    insertAdjacentElement.call(e, pos.position(), c);
    child.setParent(this);
  }

  @Override
  public void insertBefore(UiElementGwt<?> newChild, UiElementGwt<?> refChild) {
    final E e = getElement();
    final Element c = newChild.getElement();
    final Element r = refChild.getElement();
    e.insertBefore(c, r);
    refChild.setParent(this);
  }

  @Override
  public boolean addStyleName(String style) {
    return addClassName(getElement(), style);
  }

  public static boolean addClassName(final Element e, final String cls) {
    if (!hasClassName(e, cls)) {
      e.className = e.className + " " + cls;
      return true;
    }
    return false;
  }


  public static boolean removeClassName(final Element e, final String cls) {
    if (hasClassName(e, cls)) {
      final String clsName = " " + e.className + " ";
      e.className = clsName.replace(" " + cls + " ", " ").trim();
      return true;
    }
    return false;
  }

  public static boolean hasClassName(final Element e, final String cls) {
    return (" " + e.className + " ")
        .contains(" " + cls + " ");
  }

  @Override
  public boolean removeStyleName(String style) {
    return removeClassName(getElement(), style);
  }

  @Override
  public ElementInjector<Node, UiElementGwt<?>> asInjector() {
    return new ElementalInjector(getElement());
  }
}
