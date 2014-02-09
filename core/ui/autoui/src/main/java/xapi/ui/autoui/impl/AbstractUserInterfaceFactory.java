package xapi.ui.autoui.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

@SuppressWarnings("rawtypes")
public abstract class AbstractUserInterfaceFactory implements UserInterfaceFactory {

  @Override
  public <T, U extends UserInterface<T>> U createUi(Class<? extends T> type, Class<U> uiType) {
    UiRenderingContext[] options = getOptions(type);
    List<UiRenderingContext> 
      head = new ArrayList<UiRenderingContext>(),
      body = new ArrayList<UiRenderingContext>(),
      tail = new ArrayList<UiRenderingContext>()
    ;
    
    for (UiRenderingContext ctx : options) {
      (ctx.isHead()?head:ctx.isTail()?tail:body).add(ctx);
    }
    return instantiateUi(type, uiType, options);
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
    if (rendererOptions.template().length() > 0) {
      ctx.setTemplate(rendererOptions.template(), rendererOptions.templatekeys());
    }
    ctx.setSelector(getSelector(ctx, rendererOptions));
    ctx.setValidators(getValidators(ctx, rendererOptions));
  }

  protected Collection<UiRenderingContext> extractRenderingContext(UiOptions annotation) {
    List<UiRenderingContext> ctx = new ArrayList<UiRenderingContext>();
    for (UiRendererOptions rendererOption : annotation.renderers()) {
      for (Class<? extends UiRenderer> renderer : rendererOption.renderers()) {
        ctx.add(createContext(renderer, rendererOption));
      }
    }
    return ctx;
  }

  protected Collection<UiRenderingContext> extractRenderingContext(UiRendererOptions rendererOption) {
    List<UiRenderingContext> ctx = new ArrayList<UiRenderingContext>();
    for (Class<? extends UiRenderer> renderer : rendererOption.renderers()) {
      ctx.add(createContext(renderer, rendererOption));
    }
    return ctx;
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

  private final <T, U extends UserInterface<T>> U instantiateUi(
      final Class<? extends T> type,
      final Class<U> uiType,
      final UiRenderingContext[] head) {
    final U ui = newUi(type, uiType);
    try {
      return ui;
    } finally {
      ui.setRenderers(head);
    }
  }

  protected <T, U extends UserInterface<T>> U newUi(Class<? extends T> type, Class<U> uiType) {
    if (uiType == null) {
      throw new NullPointerException("Must specify UI type for "+type);
    }
    return create(uiType);
  }

}
