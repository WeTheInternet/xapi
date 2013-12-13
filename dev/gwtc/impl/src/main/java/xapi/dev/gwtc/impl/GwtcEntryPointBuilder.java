package xapi.dev.gwtc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.reflect.client.GwtReflect;

import xapi.dev.X_Gwtc;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.gwtc.api.DefaultValue;
import xapi.log.X_Log;
import xapi.source.X_Source;

public class GwtcEntryPointBuilder {

  private GwtcService gwtc;
  protected final MethodBuffer out;
  private final ClassBuffer cls;

  private final Map<String, String> instanceProviders;

  public GwtcEntryPointBuilder(SourceBuilder<GwtcService> gwtc) {
    this.gwtc = gwtc.getPayload();
    this.cls = gwtc.getClassBuffer();
    out = cls.addInterface(EntryPoint.class)
        .createMethod("void onModuleLoad");
    instanceProviders = new LinkedHashMap<String, String>();
  }

  public void println(String text) {
    out.println(text);
  }
  
  public String formatInstanceCall(Method method, boolean onNewInstance) {
    String cls = formatInstanceProvider(method.getDeclaringClass());
    StringBuilder b = new StringBuilder();
    b.append(cls).append(".");
    b.append(formatMethodCall(method));
    b.append(";");
    return b.toString();
  }

  public String formatInstanceProvider(Class<?> declaringClass) {
    String field = instanceProviders.get(declaringClass.getCanonicalName());
    if (field == null) {
      X_Log.trace(getClass(), "Generating instance field for ",declaringClass);
      field = X_Source.toStringEnclosed(declaringClass).replace('.', '_');
      field = Character.toLowerCase(field.charAt(0)) + field.substring(1);
      instanceProviders.put(declaringClass.getCanonicalName(), field);
      FieldBuffer buffer = cls.createField(declaringClass, field, Modifier.PRIVATE | Modifier.FINAL);
      StringBuilder b = new StringBuilder()
         .append("new ")
         .append(cls.addImport(declaringClass))
         .append("(");
      // Find the best constructor
      Constructor<?> winner = null;
      Constructor<?>[] ctors = declaringClass.getConstructors();
      search:
      for (int i = ctors.length; i --> 0;) {
        Constructor<?> ctor = ctors[i];
        if (ctor.getAnnotation(Inject.class) != null) {
          // Backup default, we'll accept @Inject constructors
          winner = ctor;
        }
        for (Annotation[] annos : ctor.getParameterAnnotations()) {
          for (Annotation anno : annos) {
            if (anno instanceof DefaultValue) {
              winner = ctor;
              break search;
            }
          }
        }
      }
      if (winner == null) {
        winner = GwtReflect.getPublicConstructor(declaringClass);
      }
      if (winner == null) {
        String error =
          "Cannot instantiate instance of "+declaringClass.getCanonicalName()+"; "
              + "as it does not have an any public constructors annotated with "
              + "@DefaultValue, or a zero-arg public constructor.";
        IllegalArgumentException exception = new IllegalArgumentException(error);
        X_Log.error(getClass(), error, exception);
        throw exception;
      }
      b.append(formatParameters(winner.getParameterTypes(), winner.getParameterAnnotations()));
      buffer.setInitializer(b+");");
    }
    return field;
  }

  public String formatMethodCall(Method method) {
    StringBuilder b = new StringBuilder();
    b.append(method.getName()).append("(");
    b.append(formatParameters(method.getParameterTypes(), method.getParameterAnnotations()));
    return b.append(")").toString();
  }

  public  String formatParameters(Class<?>[] params, Annotation[][] annos) {
    StringBuilder b = new StringBuilder();
    for (int i = 0, m = params.length; i < m; i++){
      Class<?> param = params[i];
      DefaultValue value = X_Gwtc.getDefaultValue(param, annos[i]);
      if (value == null) {
        b.append("null");
      } else {
        b.append(value.value());
      }
      if (i > 0) {
        b.append(", ");
      }
    }
    return b.toString();
  }
  
  public String formatStaticCall(Method method) {
    String clazz = cls.addImport(method.getDeclaringClass());
    StringBuilder b = new StringBuilder();
    b .append(clazz)
      .append(".")
      .append(formatMethodCall(method))
      .append(";");
    return b.toString();
  }

}
