package xapi.components.impl;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.DivElement;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.components.api.JsObjectDescriptor;
import xapi.components.api.JsoArray;
import xapi.components.api.OnWebComponentAttributeChanged;
import xapi.components.api.ShadowDomPlugin;
import xapi.components.api.Symbol;
import xapi.fu.*;
import xapi.util.X_String;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import static xapi.components.impl.JsFunctionSupport.*;
import static xapi.components.impl.JsSupport.copy;
import static xapi.components.impl.WebComponentVersion.V0;
import static xapi.components.impl.WebComponentVersion.V1;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;

public class WebComponentBuilder {

  public static final WebComponentVersion DEFAULT_VERSION = "true".equals(
      System.getProperty("web.components.v0", "false")) ? V1 : V0;

  private final WebComponentVersion version;
  private final JavaScriptObject componentClass;

  private MapLike<String, Object> extras;

  public static WebComponentBuilder create() {
    return new WebComponentBuilder(htmlElementPrototype(), DEFAULT_VERSION);
  }

  public static WebComponentBuilder create(final JavaScriptObject proto) {
    return new WebComponentBuilder(proto, DEFAULT_VERSION);
  }

  public static native JavaScriptObject htmlElementPrototype()
  /*-{
     return Object.create($wnd.HTMLElement.prototype);
   }-*/;

  public static native JavaScriptObject htmlElementClass()
  /*-{
     return $wnd.HTMLElement;
   }-*/;

  private final WebComponentPrototype prototype;
  private String superTag;
  private Fifo<ShadowDomPlugin> plugins;

  public WebComponentBuilder(JavaScriptObject prototype) {
    this(prototype, DEFAULT_VERSION);
  }

  public WebComponentBuilder(JavaScriptObject classOrProto, WebComponentVersion version) {
    this.version = version;
    plugins = X_Collect.newFifo(); // uses linked list in jvm, but array in js
    if (version == V0) {
      if (classOrProto == null) {
        classOrProto = htmlElementPrototype();
      }
      this.prototype = (WebComponentPrototype) classOrProto;
      componentClass = classOrProto;
    } else {
      if (classOrProto == null) {
        classOrProto = htmlElementClass();
      }
      componentClass = createJsClass(classOrProto);
      this.prototype = JsSupport.prototypeOf(componentClass);
    }
  }

  public void setClassName(String name) {
    final JsObjectDescriptor props = JsSupport.newDescriptor();
    props.setValue(name);
    props.setConfigurable(false);
    props.setWritable(false);

    JsSupport.object().defineProperty(prototype, Symbol.toStringTag(), props);
  }

  protected native JavaScriptObject createJsClass(JavaScriptObject parent)
  /*-{

    var upgrade = typeof Reflect === 'object' ?
      function () {
        return Reflect.construct(
          parent,
          arguments,
          this.constructor
        );
      } :
      function () {
        return parent.apply(this, arguments) || this;
      };

    // the actual constructor
    function constructor() {
      // delegates to an optional init property that we can build imperatively
      return this.init && this.init.apply(this, arguments);
    }

    function clazz() {
      // Get an extensible instance of the HTMLElement (or subtype)
      var self = upgrade.apply(this, arguments);
      // call the constructor as that instance, returning our
      // supplied instance, or a new instance, if the init method so chooses.
      return constructor.apply(self, arguments) || self;
    }

    clazz.prototype = Object.create(parent.prototype);

    Object.defineProperty(clazz.prototype, "constructor", {
      configurable: true,
      writable: true,
      value: clazz
    });

    return clazz;
  }-*/;

  public WebComponentBuilder attachedCallback(final Runnable function) {
    return attachedCallback(wrapRunnable(function));
  }

  public <E extends Element> WebComponentBuilder attachedCallback(
    final Consumer<E> function) {
    return attachedCallback(wrapConsumerOfThis(function));
  }

  public <E extends Element, T> WebComponentBuilder attachedCallback(
      final In1Out1<E, T> mapper, final In1<T> function) {
    return attachedCallback(wrapInputOfThis(function.map1(mapper)));
  }

  public <E extends Element, T> WebComponentBuilder attachedCallback(
      In1Out1<E, T> mapper, final In2<T, E> function) {
    return attachedCallback(wrapInputOfThis(function.map1(mapper)));
  }

  public WebComponentBuilder attachedCallback(final JavaScriptObject function) {
    // This should compile out one branch because useV1 will be compile-time constant.
    if (useV1()) {

      if (prototype.getConnectedCallback() == null) {
        prototype.setConnectedCallback(function);
      } else {
        // append the functions together
        prototype.setConnectedCallback(JsFunctionSupport.merge(prototype
          .getConnectedCallback(), function));
      }

    } else {

      if (prototype.getAttachedCallback() == null) {
        prototype.setAttachedCallback(function);
      } else {
        // append the functions together
        prototype.setAttachedCallback(JsFunctionSupport.merge(prototype
          .getAttachedCallback(), function));
      }
    }
    return this;
  }

  private boolean useV1() {
    return version == V1;
  }

  public WebComponentBuilder observeAttribute(String named) {
    JsoArray<String> observed = prototype.getObservedAttributes();
    if (observed == null) {
      observed = JsoArray.newArray(named);
      prototype.setObservedAttributes(observed);
    } else if (observed.indexOf(named) == -1) {
      observed.push(named);
    }
    return this;
  }

  public <E extends Element> WebComponentBuilder observeAttribute(String named, OnWebComponentAttributeChanged<E> callback) {
    observeAttribute(named);
    attributeChangedCallback(callback);
    return this;
  }

  public <E extends Element> WebComponentBuilder attributeChangedCallback(
    final OnWebComponentAttributeChanged<E> function) {
    return attributeChangedCallback(wrapWebComponentChangeHandler(function));
  }

  public <E extends Element, T> WebComponentBuilder attributeChangedCallback(
    final In1Out1<E, T> mapper, final OnWebComponentAttributeChanged<T> function) {
    return attributeChangedCallback(JsFunctionSupport.<E>wrapWebComponentChangeHandler((el, name, oldVal, newVal)->{
      final T mapped = mapper.io(el);
      function.onAttributeChanged(mapped, name, oldVal, newVal);
    }));
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

  public <E extends Element> WebComponentBuilder createdCallback(final In1<E> callback) {
    return createdCallback(wrapIn1(callback));
  }

  public <E extends Element, T> WebComponentBuilder createdCallback(In1Out1<E, T> mapper, final In1<T> callback) {
    return createdCallback(wrapIn1(callback.map1(mapper)));
  }

  public WebComponentBuilder createdCallback(final Do function) {
    return createdCallback(wrapDo(function));
  }

  private static final DivElement templateHost = Browser.getDocument().createDivElement();

  public void addShadowDomPlugin(ShadowDomPlugin plugin) {
    plugins.give(plugin);
  }

  public WebComponentBuilder addShadowRoot(String html, ShadowDomPlugin ... plugins) {
    final In1Out1<Element, Element> initializer;
    if (html.contains("<template")) {
      templateHost.setInnerHTML(html);
      Element template = templateHost.getFirstElementChild();
      String id = template.getId();
      if (X_String.isEmpty(id)) {
        id = JsSupport.newId();
        template.setId(id);
      }
      templateHost.setInnerHTML("");
      initializer = In2Out1.with2(this::setShadowRootTemplate, template);
    } else {
      initializer = In2Out1.with2(this::setShadowRoot, html);
    }
    // Optimize for (common) cases of not having any plugins...
    if (plugins.length == 0 && this.plugins.isEmpty()) {
      return createdCallback(initializer.ignoreOutput());
    }
    return createdCallback(element->{
      Element root = initializer.io(element);
      for (ShadowDomPlugin plugin : plugins) {
        root = plugin.transform(element, root);
      }
      for (ShadowDomPlugin plugin : this.plugins.forEach()) {
        root = plugin.transform(element, root);
      }
    });

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

  public WebComponentBuilder createdCallback(JavaScriptObject function) {
    function = reapplyThis(function);
    if (useV1()) {
      if (prototype.getInit() == null) {
        prototype.setInit(function);
      } else {
        // append the functions together
        prototype.setInit(JsFunctionSupport.merge(
          function,
          prototype.getInit()
          ));
      }
    } else {
      if (prototype.getCreatedCallback() == null) {
        prototype.setCreatedCallback(function);
      } else {
        // append the functions together
        prototype.setCreatedCallback(JsFunctionSupport.merge(
          function,
          prototype.getCreatedCallback()
          ));
      }
    }
    return this;
  }

  private native JavaScriptObject reapplyThis(JavaScriptObject f)
  /*-{
    return function() {
      return f.apply(this, [this].concat(Array.prototype.slice.apply(arguments)));
    };
  }-*/;

  public WebComponentBuilder detachedCallback(final Do function) {
    return detachedCallback(wrapDo(function));
  }

  public <E extends Element> WebComponentBuilder detachedCallback(
    final Consumer<E> function) {
    return detachedCallback(wrapConsumerOfThis(function));
  }

  public <E extends Element, T> WebComponentBuilder detachedCallback(
      final In1Out1<E, T> mapper, final In1<T> function) {
    return detachedCallback(wrapIn1(function.map1(mapper)));
  }

  public WebComponentBuilder detachedCallback(final JavaScriptObject function) {
    if (useV1()) {
      if (prototype.getDisconnectedCallback() == null) {
        prototype.setDisconnectedCallback(function);
      } else {
        // append the functions together
        prototype.setDisconnectedCallback(JsFunctionSupport.merge(prototype
          .getDisconnectedCallback(), function));
      }
    } else {
      if (prototype.getDetachedCallback() == null) {
        prototype.setDetachedCallback(function);
      } else {
        // append the functions together
        prototype.setDetachedCallback(JsFunctionSupport.merge(prototype
          .getDetachedCallback(), function));
      }
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
    return new WebComponentBuilder(copy(prototype), DEFAULT_VERSION);
  }

  public WebComponentBuilder setExtends(final String tagName) {

    this.superTag = tagName;
    return this;
  }

  public WebComponentBuilder addValue(final String name, final JavaScriptObject value) {
    return addValue(name, value, false, true, true);
  }

  public WebComponentBuilder addFunction(final String name, final In1<Object[]> value) {
    final JavaScriptObject func = JsFunctionSupport.wrapInput(value);
    return addValue(name, func, true, false, false);
  }

  public WebComponentBuilder addFunction(final String name, final In2<Element, Object[]> value) {
    final JavaScriptObject func = JsFunctionSupport.wrapInput(value);
    return addValue(name, func, true, false, false);
  }

  public <T> WebComponentBuilder addFunction(final String name, final In1Out1<Element, T> mapper, final In2<T, Element> value) {
    final JavaScriptObject func = JsFunctionSupport.wrapInputOfThis(value.map1(mapper));
    return addValue(name, func, true, true, false);
  }

  public <T> WebComponentBuilder addFunction(final String name, final In1Out1<Element, T> mapper, final In3<T, Element, Object[]> value) {
    final JavaScriptObject func = JsFunctionSupport.wrapInput(value.map1(mapper));
    return addValue(name, func, true, true, false);
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

  public native WebComponentBuilder addValue(Symbol name,
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

  public <T> WebComponentBuilder addProperty(final String name, final Out1<T> get,
    final Consumer<T> set) {
    return addProperty(name, get, set, true, false);
  }

  public <T> WebComponentBuilder addPropertyReadOnly(final String name,
    final Out1<T> get) {
    return addProperty(name, get, null, true, false);
  }

  public <T> WebComponentBuilder addPropertyWriteOnly(final String name,
    final Consumer<T> set) {
    return addProperty(name, null, set, true, false);
  }

  public native <T> WebComponentBuilder addProperty(String name,
    Out1<T> get, Consumer<T> set, boolean enumerable, boolean configurable)
    /*-{
		var proto = {
			enumerable : enumerable,
			configurable : configurable,
		};
		if (get) {
			proto.get = function() {
				get.__caller__ = this;
				return get.@xapi.fu.Out1::out1()()
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

  public <T> WebComponentBuilder addProperty(String name,
            In1Out1<Element, T> get, In2<Element, T> set) {
    return addProperty(name, get, set, true, false);
  }

  public <C, T> WebComponentBuilder addProperty(String name,
            In1Out1<Element, C> mapper, In1Out1<C, T> get, In2<C, T> set) {
    return addProperty(name, get == null ? null : get.mapIn(mapper), set == null ? null : set.map1(mapper), true, false);
  }

  public <T> WebComponentBuilder addPropertyReadOnly(String name, In1Out1<Element, T> get) {
    return addProperty(name, get, null, true, false);
  }

  public <T> WebComponentBuilder addPropertyWriteOnly(String name, In2<Element, T> set) {
    return addProperty(name, null, set, true, false);
  }

  public native <T> WebComponentBuilder addProperty(String name,
            In1Out1<Element, T> get, In2<Element, T> set, boolean enumerable, boolean configurable)
    /*-{
		var proto = {
			enumerable : enumerable,
			configurable : configurable
		};
		if (get) {
			proto.get = function() {
				get.__caller__ = this;
				return get.@In1Out1::io(Ljava/lang/Object;)(this);
			};
		}
		if (set) {
			proto.set = function(i) {
				set.__caller__ = this;
				set.@In2::in(Ljava/lang/Object;Ljava/lang/Object;)(this, i);
			};
		}
		Object
				.defineProperty(
						this.@xapi.components.impl.WebComponentBuilder::prototype,
						name, proto);
		return this;
  }-*/;

  public WebComponentBuilder addPropertyInt(final String name, final Out1<Integer> get,
    final IntConsumer set) {
    return addPropertyInt(name, get, set, true, false);
  }

  public WebComponentBuilder addPropertyIntReadOnly(final String name, final Out1<Integer> get) {
    return addPropertyInt(name, get, null, true, false);
  }

  public WebComponentBuilder addPropertyIntWriteOnly(final String name,
    final IntConsumer set) {
    return addPropertyInt(name, null, set, true, false);
  }

  public native WebComponentBuilder addPropertyInt(String name,
    Out1<Integer> get, IntConsumer set, boolean enumerable, boolean configurable)
    /*-{
		var proto = {
			enumerable : enumerable,
			configurable : configurable,
		};
		if (get) {
			proto.get = function() {
				get.__caller__ = this;
				var i = get.@xapi.fu.Out1::out1()();
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

  public WebComponentBuilder addPropertyLong(final String name, final Out1<Long> get,
    final LongConsumer set) {
    return addPropertyLong(name, get, set, true, false);
  }

  public WebComponentBuilder addPropertyLongReadOnly(final String name,
    final Out1<Long> get) {
    return addPropertyLong(name, get, null, true, false);
  }

  public WebComponentBuilder addPropertyLongWriteOnly(final String name,
    final LongConsumer set) {
    return addPropertyLong(name, null, set, true, false);
  }

  @UnsafeNativeLong
  public native WebComponentBuilder addPropertyLong(String name,
    Out1<Long> get, LongConsumer set, boolean enumerable,
    boolean configurable)
    /*-{
		var proto = {
			enumerable : enumerable,
			configurable : configurable,
		};
		if (get) {
			proto.get = function() {
				get.__caller__ = this;
				var i = get.@xapi.fu.Out1::out1()();
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
    final Out1<Long> get, final LongConsumer set) {
    return addPropertyLongNativeUnbox(name, get, set, true, false);
  }

  public WebComponentBuilder addPropertyLongNativeUnboxReadOnly(final String name,
    final Out1<Long> get) {
    return addPropertyLongNativeUnbox(name, get, null, true, false);
  }

  public WebComponentBuilder addPropertyLongNativeUnboxWriteOnly(final String name,
    final LongConsumer set) {
    return addPropertyLongNativeUnbox(name, null, set, true, false);
  }

  @UnsafeNativeLong
  public native WebComponentBuilder addPropertyLongNativeUnbox(String name,
    Out1<Long> get, LongConsumer set, boolean enumerable,
    boolean configurable)
    /*-{
		var proto = {
			enumerable : enumerable,
			configurable : configurable,
		};
		if (get) {
			proto.get = function() {
				get.__caller__ = this;
				var i = get.@xapi.fu.Out1::out1()();
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

  public MapLike<String, Object> getExtras() {
    if (extras == null) {
      extras = X_Collect.newStringMap();
    }
    return extras;
  }

  public String getSuperTag() {
    return superTag;
  }

  public JavaScriptObject getComponentClass() {
    return componentClass;
  }
}
