package xapi.gwt.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

/**
 * An annotation used to describe the settings of a gwt compile.
 * 
 * Use this on a class implementing EntryPoint, or a method annotated with {@link Test},
 * (and 
 * 
 * @author "james.nelson@appian.com"
 *
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GwtCompile {

  static enum Mode {
    /** Select to launch a production compile */
    Prod, 
    /** Select to launch a super dev mode shell */
    SuperDev,
    /** Not yet supported */
    JUnit3,
    /** Not yet supported */
    JUnit4,
    /** Not yet supported */
    Dev
  }
  
  String module();
  
  Mode mode() default Mode.Prod;
  
  Class<? extends Annotation>[] userAgents() 
    default {UserAgentChrome.class, UserAgentFirefox.class, UserAgentIE10.class};
    
  TreeLogger.Type logLevel() default Type.INFO;
  
  int port() default 1337;
  
  String[] src() default {};
  
  String[] inherits() default {};
  
  String generatedOutput() default "";

  String extras() default "";
  
  /** Only valid for production compiles */
  boolean compileReport() default true;
  
}
