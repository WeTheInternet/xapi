package xapi.components.impl;

import static xapi.components.impl.JsFunctionSupport.wrapConsumerOfThis;
import static xapi.components.impl.JsFunctionSupport.wrapRunnable;
import static xapi.components.impl.JsFunctionSupport.wrapWebComponentChangeHandler;
import static xapi.components.impl.JsSupport.copy;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;

import xapi.components.api.OnWebComponentAttributeChanged;

public class WebComponentBuilder {

  @JsType
  static interface WebComponentPrototype {
    @JsProperty
    void attachedCallback(JavaScriptObject callback);

    @JsProperty
    JavaScriptObject attachedCallback();

    @JsProperty
    void createdCallback(JavaScriptObject callback);

    @JsProperty
    JavaScriptObject createdCallback();

    @JsProperty
    void detachedCallback(JavaScriptObject callback);

    @JsProperty
    JavaScriptObject detachedCallback();

    @JsProperty
    void attributeChangedCallback(JavaScriptObject callback);

    @JsProperty
    JavaScriptObject attributeChangedCallback();
  }

  public static WebComponentBuilder create() {
    return new WebComponentBuilder(htmlElementPrototype());
  }

  public static WebComponentBuilder create(JavaScriptObject proto) {
    return new WebComponentBuilder(proto);
  }

  public static native JavaScriptObject htmlElementPrototype()
  /*-{
     return HTMLElement.prototype;
   }-*/;

  private final WebComponentPrototype prototype;

  public WebComponentBuilder(JavaScriptObject prototype) {
    if (prototype == null) {
      prototype = htmlElementPrototype();
    }
    this.prototype = (WebComponentPrototype) prototype;
  }

  public WebComponentBuilder attachedCallback(Runnable function) {
    return attachedCallback(wrapRunnable(function));
  }

  public <E extends Element> WebComponentBuilder attachedCallback(
      Consumer<E> function) {
    return attachedCallback(wrapConsumerOfThis(function));
  }

  public WebComponentBuilder attachedCallback(JavaScriptObject function) {
    if (prototype.attachedCallback() == null) {
      prototype.attachedCallback(function);
    } else {
      // append the functions together
      prototype.attachedCallback(JsFunctionSupport.merge(prototype
        .attachedCallback(), function));
    }
    return this;
  }

  public <E extends Element> WebComponentBuilder attributeChangedCallback(
      OnWebComponentAttributeChanged function) {
    return attachedCallback(wrapWebComponentChangeHandler(function));
  }

  public WebComponentBuilder attributeChangedCallback(JavaScriptObject function) {
    if (prototype.attributeChangedCallback() == null) {
      prototype.attributeChangedCallback(function);
    } else {
      // append the functions together
      prototype.attributeChangedCallback(JsFunctionSupport.merge(prototype
        .attributeChangedCallback(), function));
    }
    return this;
  }

  public WebComponentBuilder createdCallback(Runnable function) {
    return createdCallback(wrapRunnable(function));
  }

  public <E extends Element> WebComponentBuilder createdCallback(
      Consumer<E> function) {
    return createdCallback(wrapConsumerOfThis(function));
  }

  public WebComponentBuilder createdCallback(JavaScriptObject function) {
    function = reapplyThis(function);
    if (prototype.createdCallback() == null) {
      prototype.createdCallback(function);
    } else {
      // append the functions together
      prototype.createdCallback(JsFunctionSupport.merge(
        function,
        prototype.createdCallback()
      ));
    }
    return this;
  }

  private native JavaScriptObject reapplyThis(JavaScriptObject f)
  /*-{
    return function() {
      return f.apply(this, [this].concat(Array.prototype.slice.apply(arguments)));
    };
  }-*/;

  public WebComponentBuilder detachedCallback(Runnable function) {
    return detachedCallback(wrapRunnable(function));
  }

  public <E extends Element> WebComponentBuilder detachedCallback(
      Consumer<E> function) {
    return detachedCallback(wrapConsumerOfThis(function));
  }

  public WebComponentBuilder detachedCallback(JavaScriptObject function) {
    if (prototype.detachedCallback() == null) {
      prototype.detachedCallback(function);
    } else {
      // append the functions together
      prototype.detachedCallback(JsFunctionSupport.merge(prototype
        .detachedCallback(), function));
    }
    return this;
  }

  public native JavaScriptObject build()
  /*-{
		return {
			prototype : this.@xapi.components.impl.WebComponentBuilder::prototype
		};
  }-*/;

  public WebComponentBuilder extend() {
    return new WebComponentBuilder(copy(prototype));
  }

  public WebComponentBuilder addValue(String name, JavaScriptObject value) {
    return addValue(name, value, false, true, true);
  }

  public WebComponentBuilder addValueReadOnly(String name,
      JavaScriptObject value) {
    return addValue(name, value, false, true, false);
  }

  public native WebComponentBuilder addValue(String name,
      JavaScriptObject value, boolean enumerable, boolean configurable,
      boolean writeable)
  /*-{
		Object
				.defineProperty(
						this.@xapi.components.impl.WebComponentBuilder::prototype,
						name, {
							value : value,
							enumerable : enumerable,
							configurable : configurable,
							writeable : writeable
						});
		return this;
  }-*/;

  public <T> WebComponentBuilder addProperty(String name, Supplier<T> get,
      Consumer<T> set) {
    return addProperty(name, get, set, true, false);
  }

  public <T> WebComponentBuilder addPropertyReadOnly(String name,
      Supplier<T> get) {
    return addProperty(name, get, null, true, false);
  }

  public <T> WebComponentBuilder addPropertyWriteOnly(String name,
      Consumer<T> set) {
    return addProperty(name, null, set, true, false);
  }

  public native <T> WebComponentBuilder addProperty(String name,
      Supplier<T> get, Consumer<T> set, boolean enumerable, boolean configurable)
  /*-{
		var proto = {
			enumerable : enumerable,
			configurable : configurable,
		};
		if (get) {
			proto.get = function() {
				get.__caller__ = this;
				return get.@java.util.function.Supplier::get()()
			};
		}
		if (set) {
			proto.set = function(i) {
				set.__caller__ = this;
				set.@java.util.function.Consumer::accept(Ljava/lang/Object;)(i)
			};
		}
		Object
				.defineProperty(
						this.@xapi.components.impl.WebComponentBuilder::prototype,
						name, proto);
		return this;
  }-*/;

  public WebComponentBuilder addPropertyInt(String name, IntSupplier get,
      IntConsumer set) {
    return addPropertyInt(name, get, set, true, false);
  }

  public WebComponentBuilder addPropertyIntReadOnly(String name, IntSupplier get) {
    return addPropertyInt(name, get, null, true, false);
  }

  public WebComponentBuilder addPropertyIntWriteOnly(String name,
      IntConsumer set) {
    return addPropertyInt(name, null, set, true, false);
  }

  public native WebComponentBuilder addPropertyInt(String name,
      IntSupplier get, IntConsumer set, boolean enumerable, boolean configurable)
  /*-{
		var proto = {
			enumerable : enumerable,
			configurable : configurable,
		};
		if (get) {
			proto.get = function() {
				get.__caller__ = this;
				var i = get.@java.util.function.IntSupplier::getAsInt()();
				return @xapi.components.impl.JsSupport::unboxInteger(Lcom/google/gwt/core/client/JavaScriptObject;)(i)
			};
		}
		if (set) {
			proto.set = function(i) {
				set.__caller__ = this;
				var i = @xapi.components.impl.JsSupport::unboxInteger(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
				set.@java.util.function.IntConsumer::accept(I)(i)
			};
		}
		Object
				.defineProperty(
						this.@xapi.components.impl.WebComponentBuilder::prototype,
						name, proto);
		return this;
  }-*/;

  public WebComponentBuilder addPropertyLong(String name, LongSupplier get,
      LongConsumer set) {
    return addPropertyLong(name, get, set, true, false);
  }

  public WebComponentBuilder addPropertyLongReadOnly(String name,
      LongSupplier get) {
    return addPropertyLong(name, get, null, true, false);
  }

  public WebComponentBuilder addPropertyLongWriteOnly(String name,
      LongConsumer set) {
    return addPropertyLong(name, null, set, true, false);
  }

  @UnsafeNativeLong
  public native WebComponentBuilder addPropertyLong(String name,
      LongSupplier get, LongConsumer set, boolean enumerable,
      boolean configurable)
  /*-{
		var proto = {
			enumerable : enumerable,
			configurable : configurable,
		};
		if (get) {
			proto.get = function() {
				get.__caller__ = this;
				var i = get.@java.util.function.LongSupplier::getAsLong()();
				return @xapi.components.impl.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;)(i)
			};
		}
		if (set) {
			proto.set = function(i) {
				set.__caller__ = this;
				var i = @xapi.components.impl.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
				set.@java.util.function.LongConsumer::accept(J)(i)
			};
		}
		Object
				.defineProperty(
						this.@xapi.components.impl.WebComponentBuilder::prototype,
						name, proto);
		return this;
  }-*/;

  public WebComponentBuilder addPropertyLongNativeUnbox(String name,
      LongSupplier get, LongConsumer set) {
    return addPropertyLongNativeUnbox(name, get, set, true, false);
  }

  public WebComponentBuilder addPropertyLongNativeUnboxReadOnly(String name,
      LongSupplier get) {
    return addPropertyLongNativeUnbox(name, get, null, true, false);
  }

  public WebComponentBuilder addPropertyLongNativeUnboxWriteOnly(String name,
      LongConsumer set) {
    return addPropertyLongNativeUnbox(name, null, set, true, false);
  }

  @UnsafeNativeLong
  public native WebComponentBuilder addPropertyLongNativeUnbox(String name,
      LongSupplier get, LongConsumer set, boolean enumerable,
      boolean configurable)
  /*-{
		var proto = {
			enumerable : enumerable,
			configurable : configurable,
		};
		if (get) {
			proto.get = function() {
				get.__caller__ = this;
				var i = get.@java.util.function.LongSupplier::getAsLong()();
				return @xapi.components.impl.JsSupport::unboxLongNative(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
			};
		}
		if (set) {
			proto.set = function(i) {
				set.__caller__ = this;
				var i = @xapi.components.impl.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
				set.@java.util.function.LongConsumer::accept(J)(i)
			};
		}
		Object
				.defineProperty(
						this.@xapi.components.impl.WebComponentBuilder::prototype,
						name, proto);
		return this;
  }-*/;

}