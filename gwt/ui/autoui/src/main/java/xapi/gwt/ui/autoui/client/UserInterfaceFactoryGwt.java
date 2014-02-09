package xapi.gwt.ui.autoui.client;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import xapi.annotation.inject.InstanceOverride;
import xapi.platform.GwtPlatform;
import xapi.ui.autoui.api.UiOptions;
import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.autoui.impl.AbstractUserInterfaceFactory;

@GwtPlatform
@InstanceOverride(implFor=UserInterfaceFactory.class)
public class UserInterfaceFactoryGwt extends AbstractUserInterfaceFactory{

  protected UiRenderingContext[] getOptions(Class<?> type) {
    List<UiRenderingContext> options = new ArrayList<UiRenderingContext>();
    // Check the package for options
    if (type.getPackage().isAnnotationPresent(UiOptions.class)) {
      options.addAll(extractRenderingContext(type.getPackage().getAnnotation(UiOptions.class)));
    }
    // check for enclosing types/methods?
    Class<?> check = type;
    while (check != null) {
      // Check the type for options
      addAllRendererContexts(options, check);
      Method enclosing = check.getEnclosingMethod();
      if (enclosing != null){
        addAllRendererContexts(options, enclosing);
      }
      check = check.getEnclosingClass();
    }
    
    for (Method m : type.getMethods()) {
      addAllRendererContexts(options, m);
    }
    return options.toArray(new UiRenderingContext[options.size()]);
  }

  protected void addAllRendererContexts(List<UiRenderingContext> options, AnnotatedElement element) {
    if (element.isAnnotationPresent(UiOptions.class)) {
      options.addAll(extractRenderingContext(element.getAnnotation(UiOptions.class)));
    }
    if (element.isAnnotationPresent(UiRendererOptions.class)) {
      options.addAll(extractRenderingContext(element.getAnnotation(UiRendererOptions.class)));
    }
  }
  
  @Override
  protected void applyOptions(UiRenderingContext ctx,
      UiRendererOptions rendererOptions) {
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

}
