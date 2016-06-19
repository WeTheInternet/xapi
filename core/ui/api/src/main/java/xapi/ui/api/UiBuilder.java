package xapi.ui.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.Immutable;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.ui.service.UiService;
import xapi.util.X_String;
import xapi.util.api.DebugRethrowable;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public abstract class UiBuilder <E extends UiElement> implements DebugRethrowable {

  private String type;
  private String source;
  private Out1<UiService> uiService;
  private Lazy<IntTo<UiBuilder>> children;
  private UiBuilder parent;

  protected abstract E instantiate();

  private boolean frozen;

  protected final Lazy<E> instance;

  public UiBuilder() {
    this("");
  }

  public UiBuilder(String type) {
    assert type != null : "Specify empty string for null type UiBuilders (lists)";
    instance = Lazy.deferred1(this::initialize, this::instantiate);
    uiService = UiService::getUiService;
    children = Lazy.deferred1(()->X_Collect.newList(UiBuilder.class));
    setType(type);
  }

  protected void checkNotFrozen() {
    assert !frozen : "Do not reuse a UiBuilder (build() has already been called)!" +
        "  If you want to reuse settings, use the .duplicate() method to clone this instance.";
  }

  protected E initialize(E inst) {

    return inst;
  }

  public E build() {
    E inst = instance.out1();
    frozen = true;
    return inst;
  }

  public UiBuilder <E> duplicate() {
    try {
      UiBuilder copy = getClass().newInstance();
      copy.copySettings(this);
      return copy;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  protected void copySettings(UiBuilder<E> other) {
    this.type = other.type;
    this.source = other.source;
  }

  public UiBuilder<E> setType(String type) {
    checkNotFrozen();
    this.type = type;
    return this;
  }

  public <Type extends UiElement, Generic extends Type> UiBuilder<Type> addChild(Class<Generic> type) {
    final UiBuilder<Type> child = getUiService().newBuilder(type);
    child.setParent(this);
    return child;
  }

  public <Type extends UiElement, Generic extends Type> UiBuilder<E> withChild(
      Class<Generic> type, In1<UiBuilder<Type>> callback) {
    final UiBuilder<Type> child = getUiService().newBuilder(type);
    child.setParent(this);
    children.out1().add(child);
    callback.in(child);
    return this;
  }

  protected void setParent(UiBuilder parent) {
    this.parent = parent;
  }

  protected UiBuilder getParent() {
    return parent;
  }

  public String getType() {
    return type;
  }

  public void setSource(String ... source) {
    checkNotFrozen();
    this.source = X_String.join("\n", source);
  }

  public String getOriginalSource() {
    return source;
  }

  public UiService getUiService() {
    return uiService.out1();
  }

  public void setUiService(UiService uiService) {
    this.uiService = Immutable.immutable1(uiService);
  }
}
