package xapi.util.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;

import xapi.annotation.compile.MagicMethod;
import xapi.annotation.inject.SingletonDefault;
import xapi.fu.Out1;
import xapi.util.X_Namespace;
import xapi.util.service.PropertyService;

@SingletonDefault(implFor=PropertyService.class)
public class PropertyServiceDefault implements PropertyService{

  private static final String DEFAULT = "";

  @Override
  @MagicMethod(doNotVisit=true)
  public String getProperty(final String key) {
    return System.getProperty(key, null);
  }

  @Override
  @MagicMethod(doNotVisit=true)
  public String getProperty(final String key, final String dflt) {
    return System.getProperty(key, dflt);
  }

  @Override
  @MagicMethod(doNotVisit=true)
  public String getProperty(final String key, final Out1<String> dflt) {
    final String value = System.getProperty(key, DEFAULT);
    if (value == DEFAULT) { // we actually want referential comparison here, to distinguish between set to "" and empty...
      return dflt.out1();
    }
    return value;
  }

  @Override
  public void setProperty(final String key, final String value) {
    if (System.getSecurityManager()==null) {
      System.setProperty(key, value);
    } else {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          System.setProperty(key, value);
          return null;
        }
      });
    }
  }

  @Override
  public boolean isRuntimeInjection() {
    return !"false".equals(getProperty(X_Namespace.PROPERTY_USE_X_INJECT));
  }

}
