package xapi.components.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiBodyExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental2.dom.MutationObserver;
import elemental2.dom.MutationObserver.MutationObserverCallbackFn;
import elemental2.dom.MutationObserverInit;
import elemental2.dom.MutationRecord;
import jsinterop.base.Js;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.components.api.*;
import xapi.fu.*;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.api.component.IsComponent;
import xapi.ui.api.component.Slot;
import xapi.ui.api.component.SlotController;
import xapi.util.X_String;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import static xapi.components.impl.JsFunctionSupport.*;
import static xapi.components.impl.JsSupport.copy;
import static xapi.components.impl.JsSupport.getShadowRoot;
import static xapi.components.impl.ShadowDomControllerBuilder.DISABLE_SHADOW_SPLICING;
import static xapi.components.impl.WebComponentVersion.V0;
import static xapi.components.impl.WebComponentVersion.V1;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.UnsafeNativeLong;

public class WebComponentBuilder {

  private static final String RENDERED_CONTENT = "slot_rendered";
  private final WebComponentVersion version;
  private final JavaScriptObject componentClass;

  private MapLike<String, Object> extras;

  public static WebComponentBuilder create() {
    return new WebComponentBuilder(htmlElementPrototype(), WebComponentSupport.VERSION);
  }

  public static WebComponentBuilder create(final JavaScriptObject proto) {
    return new WebComponentBuilder(proto, WebComponentSupport.VERSION);
  }

  public static native JavaScriptObject htmlElementPrototype()
  /*-{
     return Object.create($wnd.HTMLElement.prototype);
   }-*/;

  public static native JavaScriptObject htmlElementClass()
  /*-{
     return $wnd.HTMLElement;
   }-*/;

  public static native JavaScriptObject htmlAnchorClass()
  /*-{
     return $wnd.HTMLAnchorElement;
   }-*/;

  private final WebComponentPrototype prototype;
  private String superTag;
  Fifo<ShadowDomPlugin> plugins;

  public WebComponentBuilder(JavaScriptObject prototype) {
    this(prototype, WebComponentSupport.VERSION);
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
      var constructor = function() {
        // delegates to an optional init property that we can build imperatively
        return this.init && this.init.apply(this, arguments);
      }

      var clazz = function() {
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

  public <E extends Element, T> WebComponentBuilder attachedCallbackMapped(
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

  public <E extends Element> WebComponentBuilder afterCreatedCallback(final In1<E> callback) {
    return afterCreatedCallback(wrapIn1(callback));
  }

  public <E extends Element, C extends IsComponent<Node, E>> WebComponentBuilder afterCreatedCallback(final In2<E, ComponentOptions<Node, E, C>> callback) {
    return afterCreatedCallback(wrapIn2(callback));
  }

  public <E extends Element, T> WebComponentBuilder afterCreatedCallback(In1Out1<E, T> mapper, final In1<T> callback) {
    return afterCreatedCallback(wrapIn1(callback.map1(mapper)));
  }

  public WebComponentBuilder afterCreatedCallback(final Do function) {
    return afterCreatedCallback(wrapDo(function));
  }


  public WebComponentBuilder createdCallback(final Runnable function) {
    return createdCallback(wrapRunnable(function));
  }

  public <E extends Element> WebComponentBuilder createdCallback(final In1<E> callback) {
    return createdCallback(wrapIn1(callback));
  }

  public <E extends Element, C extends IsComponent<Node, E>> WebComponentBuilder createdCallback(final In2<E, ComponentOptions<Node, E, C>> callback) {
    return createdCallback(wrapIn2(callback));
  }

  public <E extends Element, T> WebComponentBuilder createdCallback(In1Out1<E, T> mapper, final In1<T> callback) {
    return createdCallback(wrapIn1(callback.map1(mapper)));
  }

  public WebComponentBuilder createdCallback(final Do function) {
    return createdCallback(wrapDo(function));
  }


  private static final Element templateHost = JsSupport.doc().createElement("div");

  public void addShadowDomPlugin(ShadowDomPlugin plugin) {
    plugins.give(plugin);
  }

  public final WebComponentBuilder addShadowRoot(String html, ShadowDomPlugin ... plugins) {
    return addShadowRoot(ShadowMode.OPEN, html, plugins);
  }

  protected Maybe<UiContainerExpr> maybeVirtualRoot(ShadowMode mode, String html, ShadowDomPlugin ... plugins) {
    if (html.trim().startsWith("<virtual")) {
      assert mode != ShadowMode.CLOSED : "Cannot use a closed shadow root with a virtual shadow implementation (it isn't possible)";
      // parse our "html", and construct a virtual root from our source;
      return Maybe.immutable(getParser().io(html));
    }
    if (alwaysUseVirtualRoot()) {
      // Whenever we want to always use virtual root, we simply bypass child unwrapping...
      final UiContainerExpr expr = getParser().io(html.startsWith("<virtual") ? html : "<virtual>" + html + "</virtual>");
      return Maybe.immutable(expr);
    }
    return Maybe.not();
  }

  private boolean alwaysUseVirtualRoot() {
    // we will change the default to true for non-native browsers later,
    // once our "one at a time" implementations are fully supported
    return "true".equals(System.getProperty("virtual.shadow.root", "false"));
  }

  private In1Out1Unsafe<String, UiContainerExpr> getParser() {
    return JavaParser::parseUiContainer;
  }

  public WebComponentBuilder addShadowRoot(ShadowMode mode, String html, ShadowDomPlugin ... plugins) {
    final In1Out1<Element, Element> initializer;
    final Maybe<UiContainerExpr> virtualRoot = maybeVirtualRoot(mode, html, plugins);
    final Mutable<ShadowDomControllerBuilder> slotController = new Mutable<>();

    if (!virtualRoot.isPresent()) {
      // When there is no virtual root, our life is easier.
      addNativeShadowRoot(mode, html, plugins);
      return this;
    }

    // When a virtual root is requested, we want to construct regular dom for our source html,
    // then hookup mutation listeners so future attached elements can be analyzed, then slotted.
    // This will construct the same effective DOM as regular shadow DOM, but without the shadow barrier.
    // This is a stopgap solution until polyfills/browsers can get a decent V1 shadow dom experience,
    // unless you are some brave soul who intends to support all ancient browsers using this to polyfill forever

    // With our virtual shadow root, we need to setup a few things.
    // First, we want to insert our shadow dom sometime before being attached,
    // and sometime after the constructor (cannot append children in custom element constructor).
    // Then, we want to maintain the same shadow DOM elements whenever innerHTML is set,
    // plus slotting inserted elements whenever they are added to the root element.

    // The tricky part is the innerHTML, since we want to remove our virtual shadow DOM,
    // clear out each of the slot, disable the slotting of new elements,
    // set the innerHTML, put back the shadow elements,
    // then enable the slotter and slot whatever was set by innerHTML
    // It's not pretty, but it works.


    final String slotHtml = toHtmlVirtual(virtualRoot.get(), slotController);
    if (html.contains("<template")) {
        templateHost.setInnerHTML(slotHtml);
      Element template = templateHost.getFirstElementChild();
      if (X_String.isEmpty(template.getId())) {
        template.setId(JsSupport.newId());
      }

      templateHost.setInnerHTML("");
      final ShadowDomControllerBuilder controller = slotController.out1();
      // template with virtual root
      initializer = In3Out1.with1(this::setVirtualShadowRootTemplate, template)
            .supply2(controller);
    } else {
      // no template

      // we want to trigger getShadowRoot whenever attached, as our virtual controllers will use this to
      // perform JustInTime initialization of shadow root.  We are aware this will lead to some race conditions,
      // but these issues will not be dealt with at this time (the plan so far would be to override any functions
      // which attempt to read child state, and modify them to trigger lazy initialization).
      // Ideally, we will make due with crappier, racier implementation for now, to wait out either native shadow
      // DOM support in browsers, or better polyfills which do the heavy per-browser lifting for us.
      final ShadowDomControllerBuilder controller = slotController.out1();
      initializer = In3Out1.with1(this::setVirtualShadowRoot, slotHtml)
            .supply2(controller);
    }
    // ensure that we have initialized shadow root whenever we are attached to the document.
    attachedCallback(JsSupport::getShadowRoot);

    // Analyze our virtual root, and build
    final ShadowDomControllerBuilder controller = slotController.out1();

    if (controller != null) {
      controller.bindToComponent(this, controller, initializer, plugins);
    }

    return this;

  }

  private WebComponentBuilder addNativeShadowRoot(ShadowMode mode, String html, ShadowDomPlugin[] plugins) {

    final In1Out1<Element, Element> initializer;
    final ShadowDomControllerBuilder controller;
    final Mutable<ShadowDomControllerBuilder> slotController = new Mutable<>();
    if (html.contains("<template")) {
        final String slotHtml = toHtmlNative(html, slotController);
        templateHost.setInnerHTML(slotHtml);

        Element template = templateHost.getFirstElementChild();
        if (X_String.isEmpty(template.getId())) {
          template.setId(JsSupport.newId());
        }

        templateHost.setInnerHTML("");
        controller = slotController.out1();
        initializer = In4Out1.with1(this::setNativeShadowRootTemplate, controller)
            .supply1(mode)
            .supply2(template);
    } else {
      // no template
      final String nativeHtml = toHtmlNative(html, slotController);

      controller = slotController.out1();
      initializer = In4Out1.with1(this::setNativeShadowRoot, controller)
          .supply1(mode)
          .supply2(nativeHtml);
    }

    // we rely on native Shadow DOM support
    // we can do native Shadow DOM modifications because we are not
    // altering the light DOM element in the constructor.
    return createdCallback(element->{
      Element root = initializer.io(element);
      for (ShadowDomPlugin plugin : plugins) {
        root = plugin.transform(element, root);
      }
      for (ShadowDomPlugin plugin : this.plugins.forEach()) {
        root = plugin.transform(element, root);
      }

      if (controller != null && controller != SlotController.NO_SLOTS && !controller.isNativeSupport()) {
        // We have a slot controller that needs a mutation observer to slot matched elements.
        final SlotController<Element, Element> myController = controller.forElement(element);
        MutationObserver observer = new MutationObserver((items, obs)->{
          // Whenever an element is attached, if it lacks a slot attribute,
          // or if that slot does not exist, we will run it through the selectors in the controller
          for (MutationRecord record : items) {
            ElIterable.forEach(record.addedNodes)
                .map(Js::<Element>cast)
                .filter(e->X_String.isEmpty(e.getAttribute("slot")))
                .forAll(myController::setSlotName);
          }

          return true;
        });

        final MutationObserverInit options = Js.uncheckedCast(JavaScriptObject.createObject());
        options.childList = true;
        options.subtree = false;
        observer.observe(Js.cast(element), options);
      }
    });

  }

  protected String toHtmlNative(String html, Mutable<ShadowDomControllerBuilder> slotController) {
    // optionally adds selector-matching support for V1 slotted elements
    if (html.contains("<slot")) {
      // if a html has slots, check if they use selectors.
      try {
        UiContainerExpr parsed = JavaParser.parseUiContainer(html);
        if (hasSelectorSlot(parsed)) {
          // Only if our source uses a selector slot do we perform slotting.
          // Note, what we should do in this case is not tear off inserted elements;
          // rather, we should just set the correct slot names (as this method assumes native slot support)
          final ShadowDomControllerBuilder builder = ShadowDomControllerBuilder.forSource(parsed, true);
          slotController.in(builder);
        }
        if (parsed.getName().equals("virtual")) {
          return parsed.getBody().toSource();
        }
      } catch (ParseException e) {
        throw new IllegalArgumentException("Bad xapi element", e);
      }
    }
    return html;
  }

  protected SlotController<Element, Element> defaultShadowDomSlotController(ShadowMode mode, String html) {
    return SlotController.noSlots();
  }

  protected String toSlotHtml(UiContainerExpr html, Mutable<SlotController<Element, Element>> slotController) {
    return html.toSource();
  }

  /**
   * This is called when compiling a source template element;
   * we will modify the slot controller in the mutable parameter
   * if any slot elements will be required.
   * @param html
   * @param slotController
   * @return
   */
  protected String toHtmlVirtual(UiContainerExpr html, Mutable<ShadowDomControllerBuilder> slotController) {
    if (hasSlot(html)) {
      final ShadowDomControllerBuilder controller = ShadowDomControllerBuilder.forSource(html, false);
      slotController.in(controller);
    }
    return html.getName().equalsIgnoreCase("virtual") ? html.getBody().toSource() : html.toSource();
  }

  private boolean hasSelectorSlot(UiContainerExpr html) {

    return html.accept(new GenericVisitorAdapter<Boolean, Object>() {
      boolean sawSlot;
      @Override
      public Boolean visit(UiContainerExpr n, Object arg) {
        if (n.getName().equalsIgnoreCase("slot")) {
          // if we have a slot, then we should add it to the controller.
          if (n.getAttribute("selector").isPresent()) {
            sawSlot = true;
            return true;
          }
        }
        return super.visit(n, arg);
      }

      @Override
      public Boolean visit(UiBodyExpr n, Object arg) {
        for (Expression uiExpr : n.getChildren()) {
          uiExpr.accept(this, arg);
          if (sawSlot) {
            return true;
          }
        }
        return false;
      }
    }, null);
  }
  private boolean hasSlot(UiContainerExpr html) {
    return html.accept(new GenericVisitorAdapter<Boolean, Object>() {
      boolean sawSlot;
      @Override
      public Boolean visit(UiContainerExpr n, Object arg) {
        if (n.getName().equalsIgnoreCase("slot")) {
          // if we have a slot, then we should add it to the controller.
          sawSlot = true;
          return true;
        }
        return super.visit(n, arg);
      }

      @Override
      public Boolean visit(UiBodyExpr n, Object arg) {
        for (Expression uiExpr : n.getChildren()) {
          uiExpr.accept(this, arg);
          if (sawSlot) {
            return true;
          }
        }
        return false;
      }
    }, null);
  }

  private Element setNativeShadowRootTemplate(ShadowDomControllerBuilder controller, ShadowMode mode, Element element, Element template) {
    final Element root = JsSupport.getShadowRoot(element, mode);
    if (template != null) {
      final Node node = JsSupport.getNode(template, "content");
      final Node clone = JsSupport.doc().importNode(node, true);
      root.appendChild(clone);
    }
    if (controller != ShadowDomControllerBuilder.NO_SLOT_BUILDER) {
      attachSlotController(controller, element);
    }
    return root;
  }
  private Element setShadowRootTemplate(ShadowMode mode, Element element, Element template) {
    final Element root = JsSupport.getShadowRoot(element, mode);
    if (template != null) {
      final Node node = JsSupport.getNode(template, "content");
      final Node clone = JsSupport.doc().importNode(node, true);
      root.appendChild(clone);
    }
    return root;
  }

  private Element setNativeShadowRoot(ShadowDomControllerBuilder controller, ShadowMode mode, Element element, String html) {
    final Element result = setShadowRoot(mode, element, html);
    if (controller != null && !controller.isNativeSupport()) {
      // our controller has to manage some selector-based slotting.
      // TODO: wire up and test this.
    }
    return result;
  }

  private Element setShadowRoot(ShadowMode mode, Element element, String html) {
    Element root = getShadowRoot(element, mode);
    if (html != null) {
      root.setInnerHTML(html);
    }
    return root;
  }

  private Element setVirtualShadowRootTemplate(Element element, Element template, ShadowDomControllerBuilder controller) {
    if (template != null) {
      Node clone = JsSupport.doc().importNode((Node)JsSupport.getNode(template, "content"), true);
      // append the document fragment from the template, filling our dom with "fake shadow root"
      element.setAttribute(DISABLE_SHADOW_SPLICING, "true");
      element.setInnerHTML("");
      element.removeAttribute(DISABLE_SHADOW_SPLICING);
      element.appendChild(clone);
    }
    return element;
  }

  protected void attachSlotController(ShadowDomControllerBuilder controller, Element element) {
    if (controller == ShadowDomControllerBuilder.NO_SLOT_BUILDER) {
      return;
    }
    final JsLazyExpando<Element, SlotController<Element, Element>> bind = controller.getSlotBinder();
    if (bind.isDefined(element)) {
      return;
    }

    final SlotController<Element, Element> slotter = controller.forElement(element);
    bind.setValue(element, slotter);
    final MutationObserverCallbackFn callback = (records, observer)->{
      for (MutationRecord record : records) {
        // For every child added, slot it via the controller.
        NodeIterable.forEach(record.addedNodes)
            .map(Js::<elemental.dom.Element>cast)
            .filter(WebComponentBuilder::shouldInsert)
            .forAll(slotter::insertChild);
        // We handle removals in the slots themselves
      }
      return false;
    };

    MutationObserver observer = new MutationObserver(callback);
    final MutationObserverInit options = Js.uncheckedCast(JavaScriptObject.createObject());
    options.childList = true;
    options.subtree = false; // only handle insertions directly into root node; ignore any insertions to our children
    observer.observe(Js.cast(element), options);

    final MappedIterable<Slot<Element, Element>> slots = slotter.getSlots();
    MutationObserver removeObserver = new MutationObserver((removed, obs)->{
      for (MutationRecord rec : removed) {
        ElIterable.forEach(rec.removedNodes)
            .map(Js::<elemental.dom.Element>cast)
            .forAll(slotter::deslot);
      }
      return true;
    });
    slots.forAll(slot->{
      Element slotEl = slot.getElement();
      removeObserver.observe(Js.cast(slotEl), options);
    });

    // TODO: detach observers in a sane manner.
  }

  private static boolean shouldInsert(Node element) {
    // TODO: enable muting / pausing
    if (ShadowDomControllerBuilder.isShadowRoot(element)) {
      return false;
    }
    return element.getNodeType() != Node.ELEMENT_NODE
        || !"true".equals(((Element)element).getAttribute(ComponentNamespace.ATTR_IS_SLOTTED));
  }

  private Element setVirtualShadowRoot(String html, Element element, ShadowDomControllerBuilder slotter) {
    // This is invoked in the constructor of the element, which is a sensitive time for a component.
    // Since we are not allowed to set innerHTML in the constructor, we need to implement some smart, lazy semantics.
    // We want to initialize the shadow root as soon as absolutely possible, but no sooner.
    // We use the element.shadowController to encapsulate this, as we will prefer that when reading shadow root,
    // and then we will forcibly ensure it is initialized whenever an element is connected.

    element.setAttribute(ShadowDomControllerBuilder.DISABLE_SHADOW_SPLICING, "true");
    element.setInnerHTML(html);
    element.removeAttribute(ShadowDomControllerBuilder.DISABLE_SHADOW_SPLICING);

    return element;
  }

  protected String staticRender(
      UiContainerExpr dom
  ) {
    if (dom.hasExtra(RENDERED_CONTENT)) {
      return (String)dom.getExtras().get(RENDERED_CONTENT);
    }
    String rendered = dom.toSource();
    dom.addExtra(RENDERED_CONTENT, rendered);
    return rendered;
  }

  public WebComponentBuilder createdCallback(JavaScriptObject function) {
    function = reapplyThis(function);
    if (useV1()) {
      if (prototype.getInit() == null) {
        prototype.setInit(function);
      } else {
        // append the functions together
        prototype.setInit(JsFunctionSupport.merge(
          prototype.getInit(),
          function
          ));
      }
    } else {
      if (prototype.getCreatedCallback() == null) {
        prototype.setCreatedCallback(function);
      } else {
        // append the functions together
        prototype.setCreatedCallback(JsFunctionSupport.merge(
          prototype.getCreatedCallback(),
          function
          ));
      }
    }
    return this;
  }

  public WebComponentBuilder afterCreatedCallback(JavaScriptObject function) {
    function = reapplyThis(function);
      if (prototype.getAfterInit() == null) {
        // First time... let's also setup our run-once handlers
        prototype.setAfterInit(function);

        createdCallback(e->{
          // Inside the constructor, we setup our timeouts to call the afterCreated callbacks.
          int handle = RunSoon.schedule(()->
            JsFunctionSupport.invoke(prototype.getAfterInit(), e)
          );
          JsSupport.addElementTasks(e, handle);
          if (JsFunctionSupport.isInGwtCallStack()) {
            Scheduler.get().scheduleFinally(()->
              JsSupport.flushElementTasks(e)
            );
          }
        });
        attachedCallback(JsSupport::flushElementTasks);
      } else {
        // append the functions together
        prototype.setAfterInit(JsFunctionSupport.merge(
          prototype.getAfterInit(),
          function
          ));
      }
    return this;
  }

  public WebComponentBuilder detachedCallback(final Do function) {
    return detachedCallback(wrapDo(function));
  }

  public <E extends Element> WebComponentBuilder detachedCallback(
    final Consumer<E> function) {
    return detachedCallback(wrapConsumerOfThis(function));
  }

  public <E extends Element, T> WebComponentBuilder detachedCallbackMapped(
      In1Out1<E, T> mapper, final In2<T, E> function) {
    return detachedCallback(wrapInputOfThis(function.map1(mapper)));
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
    return new WebComponentBuilder(copy(prototype), WebComponentSupport.VERSION);
  }

  public WebComponentBuilder setExtends(final String tagName) {

    this.superTag = tagName;
    return this;
  }

  public PropertyConfiguration configureProperty(String name) {
    switch (name) {
      case "innerHTML":
      case "appendChild":
      case "insertChild":
        // TODO: elucidate this some more
      return new PropertyConfiguration(Js.cast(JsSupport.elementClass().getPrototype()), Js.cast(prototype), name);
    }
    return new PropertyConfiguration(Js.cast(prototype), name);
  }

  public WebComponentBuilder addValue(final String name, final Object value) {
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
    final Object value) {
    return addValue(name, value, false, true, false);
  }

  public WebComponentBuilder addValueReadOnly(final Symbol name,
    final Object value) {
    return addValue(name, value, false, true, false);
  }

  public native WebComponentBuilder addValue(String name,
    Object value, boolean enumerable, boolean configurable,
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
    Object value, boolean enumerable, boolean configurable,
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

  public <T> WebComponentBuilder addProperty(Symbol name,
    Out1<T> get, Consumer<T> set, boolean enumerable, boolean configurable) {
    return doAddProperty(name, get, set, enumerable, configurable);
  }
  public <T> WebComponentBuilder addProperty(String name,
    Out1<T> get, Consumer<T> set, boolean enumerable, boolean configurable) {
    return doAddProperty(name, get, set, enumerable, configurable);
  }

  private native <T> WebComponentBuilder doAddProperty(
      Object name,
      Out1<T> get,
      Consumer<T> set,
      boolean enumerable,
      boolean configurable)
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

  public <E extends Element, C, T> WebComponentBuilder addProperty(String name,
            In1Out1<E, C> mapper, In1Out1<C, T> get, In2<C, T> set) {
    return addProperty(name, get == null ? null : get.mapIn(mapper), set == null ? null : set.map1(mapper), true, false);
  }

  public <E extends Element, T> WebComponentBuilder addPropertyReadOnly(String name, In1Out1<E, T> get) {
    return addProperty(name, get, null, true, false);
  }

  public <E extends Element, T> WebComponentBuilder addPropertyReadOnly(Symbol name, In1Out1<E, T> get) {
    return addProperty(name, get, null, true, false);
  }

  public <T> WebComponentBuilder addPropertyWriteOnly(String name, In2<Element, T> set) {
    return addProperty(name, null, set, true, false);
  }

  public <E extends Element, T> WebComponentBuilder addProperty(Symbol name,
            In1Out1<E, T> get, In2<E, T> set, boolean enumerable, boolean configurable) {
    return doAddProperty(name, get, set, enumerable, configurable);
  }

  public <E extends Element, T> WebComponentBuilder addProperty(String name,
            In1Out1<E, T> get, In2<E, T> set, boolean enumerable, boolean configurable) {
    return doAddProperty(name, get, set, enumerable, configurable);
  }

  private native <E extends Element, T> WebComponentBuilder doAddProperty(Object name, In1Out1<E, T> get, In2<E, T> set, boolean enumerable, boolean configurable)
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

  public WebComponentPrototype getPrototype() {
    return prototype;
  }
}
