package xapi.ui.autoui.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;

import xapi.log.X_Log;
import xapi.source.template.MappedTemplate;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.autoui.api.UiOptions;
import xapi.ui.autoui.api.UiRenderer;
import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.api.UiRendererSelector;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.autoui.api.Validator;
import xapi.util.X_Debug;
import xapi.util.X_Util;
import xapi.util.api.ConvertsValue;

@SuppressWarnings("rawtypes")
public abstract class AbstractUserInterfaceFactory implements UserInterfaceFactory {

  @SuppressWarnings("unchecked")
  @Override
  public <T, U extends UserInterface<T>> U createUi(Class<? extends T> type, Class<? super U> uiType) {
    UiRenderingContext[] options = getOptions(type);
    List<UiRenderingContext>
      head = new ArrayList<UiRenderingContext>(),
      body = new ArrayList<UiRenderingContext>(),
      tail = new ArrayList<UiRenderingContext>()
    ;

    for (UiRenderingContext ctx : options) {
      (ctx.isHead()?head:ctx.isTail()?tail:body).add(ctx);
    }
    return (U) instantiateUi((Class)type, (Class)uiType, options);
  }

  protected abstract UiRenderingContext[] getOptions(Class<?> type);

  protected UiRenderingContext createContext(Class<? extends UiRenderer> renderer, UiRendererOptions rendererOptions) {
    // The default createContext method will simply instantiate all necessary instances immediately.
    // This method is left protected so you can optionally implement caching or lazy loading.
    UiRenderingContext ctx = new UiRenderingContext(create(renderer));
    applyOptions(ctx, rendererOptions);
    return ctx;
  }

  protected void applyOptions(UiRenderingContext ctx, UiRendererOptions rendererOptions) {
    if (rendererOptions.isHead()) {
      ctx.setHead(true);
    } else if (rendererOptions.isTail()) {
      ctx.setTail(true);
    }
    if (rendererOptions.isWrapper()) {
      ctx.setWrapper(true);
    }
    ctx.setSelector(getSelector(ctx, rendererOptions));
    ctx.setValidators(getValidators(ctx, rendererOptions));
  }

  protected Collection<UiRenderingContext> extractRenderingContext(UiOptions annotation, BeanValueProvider bean) {
    List<UiRenderingContext> ctxes = new ArrayList<UiRenderingContext>();
    for (UiRendererOptions rendererOption : annotation.renderers()) {
      ctxes.addAll(extractRenderingContext(rendererOption, bean, null));
    }
    return ctxes;
  }

  protected Collection<UiRenderingContext> extractRenderingContext(UiRendererOptions rendererOption, BeanValueProvider bean, String methodName) {
    List<UiRenderingContext> ctxes = new ArrayList<UiRenderingContext>();
    for (Class<? extends UiRenderer> renderer : rendererOption.renderers()) {
      UiRenderingContext ctx = createContext(renderer, rendererOption);
      ctxes.add(ctx);
      final BeanValueProvider ctxBean;
      if (methodName == null) {
        if (rendererOption.isWrapper()) {
          // We must rebase $name and $value ad-hoc for each method
          ctxBean = bean.rebaseAll();
        } else {
          ctxBean = bean;
        }
      } else {
        // Rebase $name and $value to match the given method
        ctxBean = bean.rebase(methodName);
      }
      ctx.setBeanProvider(ctxBean);
      final String t = rendererOption.template();
      if (t.length() > 0) {
        // Assemble all the keys to be used in the template.
        List<String> replaceables = new ArrayList<String>();
        for (String key : rendererOption.templatekeys()) {
          if (t.contains(key)) {
            replaceables.add(key);
          }
        }
        for (String key : ctxBean.getChildKeys()) {
          if (t.contains("${"+key+"}")) {
            replaceables.add("${"+key+"}");
          }
          if (t.contains("${"+key+".name()}")) {
            replaceables.add("${"+key+".name()}");
          }
        }
        ctx.setTemplate(new MappedTemplate(t, replaceables.toArray(new String[replaceables.size()])));
      }

    }
    return ctxes;
  }

  protected Validator[] getValidators(UiRenderingContext ctx,
      UiRendererOptions rendererOptions) {
    return null;
  }

  protected UiRendererSelector getSelector(UiRenderingContext ctx,
      UiRendererOptions rendererOptions) {
    return create(rendererOptions.selector());
  }

  protected <X> X create(Class<? extends X> renderer) {
    try {
      return renderer.newInstance();
    } catch (InstantiationException e) {
      throw X_Debug.rethrow(e.getCause() == null ? e : e.getCause());
    } catch (IllegalAccessException e) {
      throw X_Util.rethrow(e);
    }
  }

  @Override
  public BeanValueProvider getBeanProvider(Class<?> cls) {
    BeanValueProvider bean = new BeanValueProvider();
    ConvertsValue<Object, Object> valueConverter = new ConvertsValue<Object, Object>() {
      @Override
      public Object convert(Object from) {
        return from;
      }
    };
    bean.addProvider("this", getName(cls), valueConverter);
    // Now, go through all the getter methods, and add bean providers for everything we need
    recursiveAddBeanValues(bean, cls, valueConverter, "", 0);
    return bean;
  }

  protected abstract void recursiveAddBeanValues(BeanValueProvider bean, Class<?> cls, ConvertsValue<Object, Object> valueConverter, String prefix, int depth);

  protected String getNameFromMethod(Method from) {
    if (from.isAnnotationPresent(Named.class)) {
      return from.getAnnotation(Named.class).value();
    }
    String name = from.getName();
    if (name.startsWith("get") || name.startsWith("has")) {
      if (name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
        name = Character.toLowerCase(name.charAt(3)) +
            (name.length() > 4 ? name.substring(4) : "");
      }
    } else if (name.startsWith("is")) {
      if (name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
        name = Character.toLowerCase(name.charAt(2)) +
            (name.length() > 3 ? name.substring(3) : "");
      }
    }
    return name;

  }
  protected String getName(Class<?> from) {
    if (from.isAnnotationPresent(Named.class)) {
      return from.getAnnotation(Named.class).value();
    }
    return from.getName();
  }

  @SuppressWarnings("unchecked")
  private final <T, U extends UserInterface<T>> U instantiateUi(
      final Class<? extends T> type,
      final Class<? super U> uiType,
      final UiRenderingContext[] head) {
    final U ui = (U) newUi((Class)type, (Class)uiType);
    try {
      return ui;
    } finally {
      ui.setRenderers(head);
    }
  }

  @SuppressWarnings("unchecked")
  protected <T, U extends UserInterface<T>> U newUi(Class<? extends T> type, Class<? super U> uiType) {
    if (uiType == null) {
      throw new NullPointerException("Must specify UI type for "+type);
    }
    try {
      return (U) uiType.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      X_Log.error(getClass(), "Unable to instantiate",uiType, e);
      throw X_Debug.rethrow(e);
    }
  }

}
