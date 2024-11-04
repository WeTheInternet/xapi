package xapi.gwt.api;

import jsinterop.base.Js;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.log.X_Log;

/**
 * This class is used to enforce set-once semantics on properties of a javascript object.
 *
 * It's initial use case is for setting read-only properties on an element;
 * you can use this object manually to detect, set and get a named property on a js object,
 * or it is also an {@link In2Out1} functional object,
 * which takes the object to work on as the first parameter
 * and a *default factory* to create the value when the property is not defined as second parameter.
 *
 * You do not have to worry about defining the property yourself in the factory,
 * unless you want to override the default read-only nature of these properties.
 *
 * Note also, sending a String as the name key will, be default, make the property enumerable
 * (visible in a `for _ in ...` statement),
 * while sending a {@link Symbol} as your name key makes an immutable, non-enumerable property.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/6/17.
 */
public class JsLazyExpando<I, O> implements In2Out1<I, In1Out1<I, O>, O> {

  private final SymbolOrString name;

  /**
   * An enumerable, read-only property factory.
   * If you wish to create a non-enumerable, read-only factory,
   * use the constructor which takes a {@link Symbol} as an argument.
   */
  public JsLazyExpando(String name) {
    assert name != null;
    this.name = SymbolOrString.fromString(name);
  }

  /**
   * An non-enumerable, read-only property factory.
   * If you wish to create an enumerable, read-only factory,
   * use the constructor which takes a String as an argument.
   */
  public JsLazyExpando(Symbol name) {
    assert name != null;
    this.name = SymbolOrString.fromSymbol(name);
  }

  public I setValue(I on, O val) {
    final JsObjectDescriptor props = JsObjectDescriptor.create();
    props.setValue(val);
    props.setConfigurable(false);
    // strings will be enumerable; symbols won't
    props.setEnumerable(name.isString());

    Jso.defineProperty(on, name, props);
    return on;
  }

  public boolean isDefined(I on) {
    return Js.isTruthy(on) &&
        name.isDefinedOn(on);
  }

  public O getValue(I on) {
    O value = Js.uncheckedCast(name.getFrom(on));
    return value;
  }


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
      final JsObjectDescriptor opts = JsObjectDescriptor.create();
      opts.setEnumerable(name.isString());
      // We want the prototype to be configurable so we are allowed to overwrite it later.
      opts.setConfigurable(true);
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
          X_Log.debug(JsLazyExpando.class, "Applying lazy expando; ", this, self);
          final O result = io(self, factory);
          if (makeImmutable && !isDefined(self)) {
            opts.setConfigurable(false);
            opts.get(()->result);
            Jso.defineProperty(self, name, opts);
          }
          return result;
        }
      };
      // apply our Out1 as the getter on the prototype, ensuring this is passed through to our Out1
      opts.get(Js.cast(reapplyThis(wrapOut1(task))));

      // apply the descriptor; this attaches to the prototype so each new instance can call our getter
      // which will, if set to be immutable, cement the value onto that instance, making it unwritable and unconfigurable
      Jso.defineProperty(prototype, name, opts);
  }


  public static native <T> Jso wrapOut1(Out1<T> task)
	/*-{
          return @JsLazyExpando::maybeEnter(*)(function(){
            task.__caller__ = this;
            return task.@Out1::out1()();
          });
        }-*/;

  public static native Jso reapplyThis(Jso f)
        /*-{
          return function() {
            return f.apply(this, [this].concat(Array.prototype.slice.apply(arguments)));
          };
        }-*/;



  public static native Jso maybeEnter(Jso func)
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


}
