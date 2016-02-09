package xapi.components.impl;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.DivElement;
import xapi.components.api.OnWebComponentAttributeChanged;
import xapi.fu.In1;
import xapi.util.X_String;

import static xapi.components.impl.JsFunctionSupport.wrapConsumerOfThis;
import static xapi.components.impl.JsFunctionSupport.wrapRunnable;
import static xapi.components.impl.JsFunctionSupport.wrapWebComponentChangeHandler;
import static xapi.components.impl.JsSupport.copy;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class WebComponentBuilder {

  @JsType
  static interface WebComponentPrototype {
    @JsProperty
    void setAttachedCallback(JavaScriptObject callback);

    @JsProperty
    JavaScriptObject getAttachedCallback();

    @JsProperty
    void setCreatedCallback(JavaScriptObject callback);

    @JsProperty
    JavaScriptObject getCreatedCallback();

    @JsProperty
    void setDetachedCallback(JavaScriptObject callback);

    @JsProperty
    JavaScriptObject getDetachedCallback();

    @JsProperty
    void setAttributeChangedCallback(JavaScriptObject callback);

    @JsProperty
    JavaScriptObject getAttributeChangedCallback();
  }

  public static WebComponentBuilder create() {
    return new WebComponentBuilder(htmlElementPrototype());
  }

  public static WebComponentBuilder create(final JavaScriptObject proto) {
    return new WebComponentBuilder(proto);
  }

  public static native JavaScriptObject htmlElementPrototype()
  /*-{
     return HTMLElement.prototype;
   }-*/;

  private final WebComponentPrototype prototype;
  private String superTag;

  public WebComponentBuilder(JavaScriptObject prototype) {
    if (prototype == null) {
      prototype = htmlElementPrototype();
    }
    this.prototype = (WebComponentPrototype) prototype;
  }

  public WebComponentBuilder attachedCallback(final Runnable function) {
    return attachedCallback(wrapRunnable(function));
  }

  public <E extends Element> WebComponentBuilder attachedCallback(
    final Consumer<E> function) {
    return attachedCallback(wrapConsumerOfThis(function));
  }

  public WebComponentBuilder attachedCallback(final JavaScriptObject function) {
    if (prototype.getAttachedCallback() == null) {
      prototype.setAttachedCallback(function);
    } else {
      // append the functions together
      prototype.setAttachedCallback(JsFunctionSupport.merge(prototype
        .getAttachedCallback(), function));
    }
    return this;
  }

  public <E extends Element> WebComponentBuilder attributeChangedCallback(
    final OnWebComponentAttributeChanged function) {
    return attachedCallback(wrapWebComponentChangeHandler(function));
  }

  public WebComponentBuilder attributeChangedCallback(final JavaScriptObject function) {
    if (prototype.getAttributeChangedCallback() == null) {
      prototype.setAttributeChangedCallback(function);
    } else {
      // append the functions together
      prototype.setAttributeChangedCallback(JsFunctionSupport.merge(prototype
        .getAttributeChangedCallback(), function));
    }
    return this;
  }

  public WebComponentBuilder createdCallback(final Runnable function) {
    return createdCallback(wrapRunnable(function));
  }

  private static final DivElement templateHost = Browser.getDocument().createDivElement();

  public WebComponentBuilder addShadowRoot(String html) {
    return addShadowRoot(html, In1.noop());
  }
  public WebComponentBuilder addShadowRoot(String html, In1<Element> initRoot) {
    if (html.contains("<template")) {
      templateHost.setInnerHTML(html);
      Element template = templateHost.getFirstElementChild();
      String id = template.getId();
      if (X_String.isEmpty(id)) {
        id = JsSupport.newId();
        template.setId(id);
      }
      templateHost.setInnerHTML("");
      return createdCallback(element->{
        final Element root = setShadowRootTemplate(element, template);
        initRoot.in(root);
      });
    } else {
      return createdCallback(element-> {
        final Element root = setShadowRoot(element, html);
        initRoot.in(root);
      });
    }
  }

  private native Element setShadowRootTemplate(Element element, Element template)
  /*-{
    var root = element.createShadowRoot();
    var clone = document.importNode(template.content, true);
    root.appendChild(clone);
    return root;
  }-*/;

  private native Element setShadowRoot(Element element, String html)
  /*-{
    var root = element.createShadowRoot();
    root.innerHTML = html;
    return root;
  }-*/;

  public <E extends Element> WebComponentBuilder createdCallback(
    final Consumer<E> function) {
    return createdCallback(wrapConsumerOfThis(function));
  }

  public WebComponentBuilder createdCallback(JavaScriptObject function) {
    function = reapplyThis(function);
    if (prototype.getCreatedCallback() == null) {
      prototype.setCreatedCallback(function);
    } else {
      // append the functions together
      prototype.setCreatedCallback(JsFunctionSupport.merge(
        function,
        prototype.getCreatedCallback()
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

  public WebComponentBuilder detachedCallback(final Runnable function) {
    return detachedCallback(wrapRunnable(function));
  }

  public <E extends Element> WebComponentBuilder detachedCallback(
    final Consumer<E> function) {
    return detachedCallback(wrapConsumerOfThis(function));
  }

  public WebComponentBuilder detachedCallback(final JavaScriptObject function) {
    if (prototype.getDetachedCallback() == null) {
      prototype.setDetachedCallback(function);
    } else {
      // append the functions together
      prototype.setDetachedCallback(JsFunctionSupport.merge(prototype
        .getDetachedCallback(), function));
    }
    return this;
  }

  public native JavaScriptObject build()
  /*-{
  	var p = {
  		prototype : this.@xapi.components.impl.WebComponentBuilder::prototype
  	};
  	if (this.@xapi.components.impl.WebComponentBuilder::superTag != null) {
  	  p['extends'] = this.@xapi.components.impl.WebComponentBuilder::superTag;
  	}
  	return p;
  }-*/;

  public WebComponentBuilder extend() {
    return new WebComponentBuilder(copy(prototype));
  }

  public WebComponentBuilder setExtends(final String tagName) {

    this.superTag = tagName;
    return this;
  }

  public WebComponentBuilder addValue(final String name, final JavaScriptObject value) {
    return addValue(name, value, false, true, true);
  }

  public WebComponentBuilder addValueReadOnly(final String name,
    final JavaScriptObject value) {
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

  public <T> WebComponentBuilder addProperty(final String name, final Supplier<T> get,
    final Consumer<T> set) {
    return addProperty(name, get, set, true, false);
  }

  public <T> WebComponentBuilder addPropertyReadOnly(final String name,
    final Supplier<T> get) {
    return addProperty(name, get, null, true, false);
  }

  public <T> WebComponentBuilder addPropertyWriteOnly(final String name,
    final Consumer<T> set) {
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

  public WebComponentBuilder addPropertyInt(final String name, final IntSupplier get,
    final IntConsumer set) {
    return addPropertyInt(name, get, set, true, false);
  }

  public WebComponentBuilder addPropertyIntReadOnly(final String name, final IntSupplier get) {
    return addPropertyInt(name, get, null, true, false);
  }

  public WebComponentBuilder addPropertyIntWriteOnly(final String name,
    final IntConsumer set) {
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

  public WebComponentBuilder addPropertyLong(final String name, final LongSupplier get,
    final LongConsumer set) {
    return addPropertyLong(name, get, set, true, false);
  }

  public WebComponentBuilder addPropertyLongReadOnly(final String name,
    final LongSupplier get) {
    return addPropertyLong(name, get, null, true, false);
  }

  public WebComponentBuilder addPropertyLongWriteOnly(final String name,
    final LongConsumer set) {
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

  public WebComponentBuilder addPropertyLongNativeUnbox(final String name,
    final LongSupplier get, final LongConsumer set) {
    return addPropertyLongNativeUnbox(name, get, set, true, false);
  }

  public WebComponentBuilder addPropertyLongNativeUnboxReadOnly(final String name,
    final LongSupplier get) {
    return addPropertyLongNativeUnbox(name, get, null, true, false);
  }

  public WebComponentBuilder addPropertyLongNativeUnboxWriteOnly(final String name,
    final LongConsumer set) {
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
