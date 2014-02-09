package xapi.ui.autoui.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import xapi.annotation.inject.InstanceDefault;
import xapi.ui.autoui.api.UiOptions;
import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.api.UserInterfaceFactory;

@InstanceDefault(implFor=UserInterfaceFactory.class)
public class UserInterfaceFactoryDefault extends AbstractUserInterfaceFactory{

  @Override
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
  
}
