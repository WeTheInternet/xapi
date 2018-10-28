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
    String check = System.getProperty(key, null);
    if (check == null) {
      check = System.getenv(envKey(key));
    }
    return check;
  }

  protected String envKey(String key) {
    return key;
  }

  @Override
  @MagicMethod(doNotVisit=true)
  public String getProperty(final String key, final String dflt) {
    String check = System.getProperty(key, dflt);
    if (check == null) {
      check = System.getenv(envKey(key));
    }
    return check;
  }

  @Override
  @MagicMethod(doNotVisit=true)
  @SuppressWarnings("StringEquality") //
  public String getProperty(final String key, final Out1<String> dflt) {
    String value = System.getProperty(key, DEFAULT);
    if (value == DEFAULT) { // we actually want referential comparison here, to distinguish between set to "" and empty...
      value = System.getenv(envKey(key));
      if (value == null) {
        return dflt.out1();
      }
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
