package xapi.gwtc.api;

import com.google.gwt.core.ext.TreeLogger.Type;

public @interface GwtcProperties {

  String DEFAULT_WAR = "/tmp/gwtWar";

  ObfuscationLevel obfuscationLevel() default ObfuscationLevel.OBFUSCATED;

  Type logLevel() default Type.INFO;

  String warDir() default DEFAULT_WAR;
}
