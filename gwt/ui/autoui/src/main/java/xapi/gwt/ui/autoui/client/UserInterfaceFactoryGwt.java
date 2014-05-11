package xapi.gwt.ui.autoui.client;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import xapi.annotation.inject.InstanceOverride;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.platform.GwtPlatform;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.autoui.api.DoNotIndex;
import xapi.ui.autoui.api.UiOptions;
import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.autoui.impl.AbstractUserInterfaceFactory;
import xapi.util.X_Debug;
import xapi.util.api.ConvertsValue;

@GwtPlatform
@InstanceOverride(implFor=UserInterfaceFactory.class)
public class UserInterfaceFactoryGwt extends AbstractUserInterfaceFactory{

  private static final int MAX_DEPTH = 10;

  @Override
  protected UiRenderingContext[] getOptions(Class<?> type) {
    List<UiRenderingContext> options = new ArrayList<UiRenderingContext>();
    
    BeanValueProvider values = getBeanProvider(type);
    // Check the package for options
    Package pkg = type.getPackage();
    if (pkg != null && pkg.isAnnotationPresent(UiOptions.class)) {
      options.addAll(extractRenderingContext(pkg.getAnnotation(UiOptions.class), values));
    }
    if (type.isAnnotationPresent(UiOptions.class)) {
      UiOptions opts = type.getAnnotation(UiOptions.class);
      if (opts.fields().length > 0) {
        values.setChildKeys(opts.fields());
      }
    }
    // check for enclosing types/methods?
    Class<?> check = type;
    while (check != null) {
      // Check the type for options
      addAllRendererContexts(options, check, values);
      check = check.getEnclosingClass();
    }
    
    for (Method m : type.getMethods()) {
      addAllRendererContexts(options, m, values);
    }
    return options.toArray(new UiRenderingContext[options.size()]);
  }

  protected void addAllRendererContexts(List<UiRenderingContext> options, AnnotatedElement element, BeanValueProvider values) {
    if (element.isAnnotationPresent(UiOptions.class)) {
      options.addAll(extractRenderingContext(element.getAnnotation(UiOptions.class), values));
    }
    if (element.isAnnotationPresent(UiRendererOptions.class)) {
      options.addAll(extractRenderingContext(element.getAnnotation(UiRendererOptions.class), values, 
          element instanceof Method ? getNameFromMethod((Method)element) : null ));
    }
  }
  
  @Override
  protected void recursiveAddBeanValues(BeanValueProvider bean, Class<?> cls,
      final ConvertsValue<Object, Object> converter, String prefix, int depth) {
    if (depth > MAX_DEPTH) {
      X_Log.warn(getClass(), "Recursion sickness detected in "+cls+" from "+prefix+"; depth reached "+MAX_DEPTH);
      if (X_Log.loggable(LogLevel.TRACE)) {
        X_Log.trace("Consider using the @DoNotIndex annotation in the recursion chain produced by "+prefix);
      }
      return;
    }
    // Add all method getter
    for (Method m : cls.getMethods()) {
      if (
        m.getParameterTypes().length == 0
        && m.getDeclaringClass() != Object.class 
        && m.getReturnType() != void.class
      ) {
        DoNotIndex noIndex = m.getAnnotation(DoNotIndex.class);
        if (noIndex != null && noIndex.unlessDepthLessThan() >= depth) {
          continue;
        }
        final String name = getNameFromMethod(m);
        String key = prefix.length()==0 ? name : prefix+"."+name;
//        bean.addProvider(key+".name()", new ConvertsValue<Object, Object>() {
//          @Override
//          public Object convert(Object from) {
//            return name;
//          }
//        });
        final Method rootMethod = getRootMethod(m);
        ConvertsValue<Object, Object> valueConverter = new ConvertsValue<Object, Object>() {
          @Override
          public Object convert(Object from) {
            Object parent = converter.convert(from);
            try {
              return rootMethod.invoke(parent);
            } catch (Exception e) {
              throw X_Debug.rethrow(e);
            }
          }
        };
        bean.addProvider(key, name, valueConverter);
        if (isNotPrimitive(m.getReturnType())) {
          recursiveAddBeanValues(bean, m.getReturnType(), valueConverter, key, depth+1);
        }
      }
    }
  }
  

  protected Method getRootMethod(Method m) {
    if (m.getDeclaringClass().isInterface()) {
      // When using interfaces, search for the deepest ancestor interface with the given method.
      Method winner = m;
      for (Class<?> c : m.getDeclaringClass().getInterfaces()) {
        try {
          Method method = c.getMethod(m.getName(), m.getParameterTypes());
          if (method.getDeclaringClass().isAssignableFrom(winner.getDeclaringClass())) {
            winner = method;
          }
        } catch (Throwable ignored){}
      }
      return winner;
    }
    return m;
  }

  protected boolean isNotPrimitive(Class<?> cls) {
    return 
        !cls.isPrimitive() 
        && cls != String.class
        && cls != Boolean.class
        && cls != Byte.class
        && cls != Character.class
        && cls != Short.class
        && cls != Integer.class
        && cls != Long.class
        && cls != Float.class
        && cls != Double.class
    ;
  }
  
}
