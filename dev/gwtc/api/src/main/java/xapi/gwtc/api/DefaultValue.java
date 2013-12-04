package xapi.gwtc.api;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.log.X_Log;

import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {

  @ReflectionStrategy(
      methodRetention=@GwtRetention(annotationRetention=ReflectionStrategy.ALL),
      keepCodeSource=false,
      keepPackage=false
  )
  static class Defaults {

    @SuppressWarnings("unused")
    private static void defaults(
        @DefaultValue("'0'") char chars,
        @DefaultValue("0") int ints,
        @DefaultValue("0L") long longs,
        @DefaultValue("0f") float floats,
        @DefaultValue(".0") double doubles,
        @DefaultValue("false") boolean booleans,
        @DefaultValue("\"\"") String strings,
        @DefaultValue Object objects
    ){}
    
    public static final DefaultValue DEFAULT_CHAR;
    public static final DefaultValue DEFAULT_INT;
    public static final DefaultValue DEFAULT_LONG;
    public static final DefaultValue DEFAULT_FLOAT;
    public static final DefaultValue DEFAULT_DOUBLE;
    public static final DefaultValue DEFAULT_BOOLEAN;
    public static final DefaultValue DEFAULT_STRING;
    public static final DefaultValue DEFAULT_OBJECT;
    
    static {
      Annotation[][] annos = GwtReflect.getDeclaredMethod(Defaults.class,"defaults", 
          char.class, int.class, long.class, float.class,
          double.class, boolean.class, String.class, Object.class)
          .getParameterAnnotations();
      DEFAULT_CHAR = (DefaultValue)annos[0][0];
      DEFAULT_INT = (DefaultValue)annos[1][0];
      DEFAULT_LONG = (DefaultValue)annos[2][0];
      DEFAULT_FLOAT = (DefaultValue)annos[3][0];
      DEFAULT_DOUBLE = (DefaultValue)annos[4][0];
      DEFAULT_BOOLEAN = (DefaultValue)annos[5][0];
      DEFAULT_STRING = (DefaultValue)annos[6][0];
      DEFAULT_OBJECT = (DefaultValue)annos[7][0];
    }
    
  }
  
  String value() default "null";
  
}
