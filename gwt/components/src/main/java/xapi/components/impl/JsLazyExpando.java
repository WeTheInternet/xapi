package xapi.components.impl;

import xapi.components.api.JsObjectDescriptor;
import xapi.components.api.Symbol;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out1;

import static xapi.components.impl.JsFunctionSupport.reapplyThis;
import static xapi.components.impl.JsFunctionSupport.wrapOut1;
import static xapi.components.impl.JsSupport.object;

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

  /**
   * An non-enumerable, read-only property factory.
   * If you wish to create an enumerable, read-only factory,
   * use the constructor which takes a String as an argument.
   */
  public JsLazyExpando(Symbol name) {
    assert name != null;
    this.name = name;
  }

  public I setValue(I on, O val) {
    final JsObjectDescriptor props = JsSupport.newDescriptor();
    props.setValue(val);
    props.setConfigurable(false);
//    props.set(false);
    // strings will be enumerable; symbols won't
    props.setEnumerable(name instanceof String);

    object().defineProperty(on, name, props);
    return on;
  }

  public boolean isDefined(I on) {
    if (name instanceof String) {
      return JsSupport.exists(on, (String) name);
    } else {
      return JsSupport.exists(on, (Symbol) name);
    }
  }

  public O getValue(I on) {
    O value;
    if (name instanceof String) {
      value = (O) JsSupport.getObject(on, (String) name);
    } else {
      value = (O) JsSupport.getObject(on, (Symbol) name);
    }
    return value;
  }

  private boolean reentrant;

  @Override
  public O io(I el, In1Out1<I, O> io) {
    if (!reentrant && isDefined(el)) {
      reentrant = true;
      try {
        return getValue(el);
      } finally {
        reentrant = false;
      }
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
      final JsObjectDescriptor opts = JsSupport.newDescriptor();
      opts.setEnumerable(name instanceof String);
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
          return io(getCaller(), factory);
        }
      };
      // apply our Out1 as the getter on the prototype, ensuring this is passed through to our Out1
      opts.get(reapplyThis(wrapOut1(task)));

      // apply the descriptor; this js
      object().defineProperty(prototype, name, opts);
  }
}
