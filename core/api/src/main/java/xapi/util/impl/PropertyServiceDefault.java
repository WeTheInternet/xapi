package xapi.util.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;

import xapi.annotation.inject.SingletonDefault;
import xapi.util.X_Namespace;
import xapi.util.service.PropertyService;

@SingletonDefault(implFor=PropertyService.class)
public class PropertyServiceDefault implements PropertyService{

  @Override
  public String getProperty(String key) {
    return System.getProperty(key, null);
  }

  @Override
  public String getProperty(String key, String dflt) {
    return System.getProperty(key, dflt);
  }

  @Override
  public void setProperty(final String key, final String value) {
    if (System.getSecurityManager()==null)
      System.setProperty(key, value);
    else
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          System.setProperty(key, value);
          return null;
        }
      });
  }

  @Override
  public boolean isRuntimeInjection() {
    return !"false".equals(getProperty(X_Namespace.PROPERTY_USE_X_INJECT));
  }

}
