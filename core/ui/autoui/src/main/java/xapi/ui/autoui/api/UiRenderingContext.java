package xapi.ui.autoui.api;

import static xapi.collect.X_Collect.newStringMap;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import xapi.collect.api.StringTo;
import xapi.source.write.MappedTemplate;
import xapi.util.impl.ImmutableProvider;

public class UiRenderingContext {

  private static final MappedTemplate DEFAULT_TEMPLATE = new MappedTemplate("");
  @SuppressWarnings("rawtypes")
  private static final Validator[] DEFAULT_VALIDATORS = new Validator[0];
  private static final Object[] EMPTY_MESSAGES = new Object[0];


  private boolean head = false;

  private final Provider<UiRenderer<?>> renderProvider;
  private UiRendererSelector selector = UiRendererSelector.ALWAYS_TRUE;
  private boolean tail = false;

  private MappedTemplate template = DEFAULT_TEMPLATE;
  @SuppressWarnings("rawtypes")
  private Validator[] validators = DEFAULT_VALIDATORS;
  private boolean wrapper = false;
  private BeanValueProvider valueProvider;
  private String name = "";


  public UiRenderingContext(Provider<UiRenderer<?>> renderProvider) {
    this.renderProvider = renderProvider;
  }

  public UiRenderingContext(UiRenderer<?> renderer) {
    this(new ImmutableProvider<UiRenderer<?>>(renderer));
  }

  public String getName() {
    return name;
  }

  @SuppressWarnings("rawtypes")
  public final UiRenderer getRenderer() {
    return renderProvider.get();
  }

  public UiRendererSelector getSelector() {
    return selector;
  }

  public MappedTemplate getTemplate() {
    return template;
  }

  public String applyTemplate(String name, Object val) {
    StringTo<Object> map = newStringMap(Object.class);
    BeanValueProvider bean = getBeanValueProvider();
    MappedTemplate template = getTemplate();
    bean.fillMap(name, template, map, val);
    return template.applyMap(map.entries());
  }

  @SuppressWarnings("rawtypes")
  public Validator[] getValidators() {
    return validators;
  }

  public boolean isHead() {
    return head;
  }

  public boolean isTail() {
    return tail;
  }

  public boolean isTemplateSet() {
    return template != DEFAULT_TEMPLATE;
  }

  public boolean isValid(Object o) {
    if (validators.length == 0)
      return true;
    for (Validator<?> validator : validators) {
      Object error = validator.isValid(o);
      if (!(Boolean.TRUE.equals(error) || error == null))
        return false;
    }
    return true;
  }

  public boolean isWrapper() {
    return wrapper;
  }

  public UiRenderingContext setHead(boolean head) {
    this.head = head;
    return this;
  }

  public UiRenderingContext setName(String name) {
    this.name = name;
    return this;
  }

  public UiRenderingContext setSelector(UiRendererSelector selector) {
    assert selector != null : "You must supply a valid UiRendererSelector; "
        + "Use UiRendererSelector.AlwaysTrue instead of null";
    this.selector = selector;
    return this;
  }

  public UiRenderingContext setTail(boolean tail) {
    this.tail = tail;
    return this;
  }

  public UiRenderingContext setTemplate(MappedTemplate template) {
    this.template = template;
    return this;
  }

  @SuppressWarnings("rawtypes")
  public UiRenderingContext setValidators(Validator[] validators) {
    this.validators = validators;
    return this;
  }

  public UiRenderingContext setWrapper(boolean wrapper) {
    this.wrapper = wrapper;
    return this;
  }

  public Object[] validate(Object o) {
    if (validators.length == 0)
      return EMPTY_MESSAGES;
    List<Object> errors = new ArrayList<Object>();
    for (Validator<?> validator : validators) {
      Object error = validator.isValid(o);
      if (!(Boolean.TRUE.equals(error) || error == null)) {
        errors.add(error);
      }
    }
    return errors.toArray(new Object[errors.size()]);
  }

  public void setBeanProvider(BeanValueProvider bean) {
    this.valueProvider = bean;
  }

  public BeanValueProvider getBeanValueProvider() {
    return valueProvider;
  }

}
