package xapi.gwtc.api;

import com.google.gwt.core.ext.TreeLogger.Type;

public @interface GwtcProperties {

  ObfuscationLevel obfuscationLevel() default ObfuscationLevel.OBFUSCATED;
  
  Type logLevel() default Type.INFO;
}
