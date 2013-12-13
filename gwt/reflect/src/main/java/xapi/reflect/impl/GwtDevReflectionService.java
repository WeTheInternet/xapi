package xapi.reflect.impl;

import javax.validation.constraints.NotNull;

import xapi.annotation.inject.SingletonDefault;
import xapi.platform.GwtDevPlatform;
import xapi.reflect.service.ReflectionService;

@GwtDevPlatform
@SingletonDefault(implFor=ReflectionService.class)
public class GwtDevReflectionService extends ReflectionServiceDefault {

  @Override
  public Package getPackage(@NotNull Object o) {
    Class<?> cls = o.getClass();
    // Gwt-dev can't use cls.getPackage(), so we force a classloader lookup
    return getPackage(cls.getCanonicalName().replace("."+cls.getSimpleName(), ""), cls.getClassLoader());
  }

}
