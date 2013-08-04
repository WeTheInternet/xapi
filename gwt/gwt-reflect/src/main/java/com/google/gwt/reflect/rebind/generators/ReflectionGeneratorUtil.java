package com.google.gwt.reflect.rebind.generators;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dev.jjs.ast.JAbstractMethodBody;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.js.JsniClassLiteral;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

public class ReflectionGeneratorUtil {

  ReflectionGeneratorUtil() {}

  public static String generatedMagicClassName(String simpleName) {
    return simpleName+"_MC";
  }

  public static Method[] getMethods(Annotation anno) {
    ArrayList<Method> methods = new ArrayList<Method>();
    Class<? extends Annotation> cls = anno.annotationType();
    for (Method method : cls.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() == cls) {
        methods.add(method);
      }
    }
    return methods.toArray(new Method[methods.size()]);
  }

  public static Class<?>[] getMethodReturnTypes(Annotation anno) {
    Method[] methods = getMethods(anno);
    Class<?>[] classes = new Class[methods.length];
    for (int i = 0, m = methods.length; i < m; i++ ) {
      classes[i] = methods[i].getReturnType();
    }
    return classes;
  }

  public static String toJsniParams(Class<?> ... classes) {
    StringBuilder b = new StringBuilder();
    for (Class<?> c : classes) {
      b.append(toJsniParam(c));
    }
    return b.toString();
  }
  public static String toJsniParam(Class<?> type) {
    StringBuilder b = new StringBuilder();

    while (type.isArray()) {
      b.append('[');
      type = type.getComponentType();
    }
    if (type.isPrimitive())
      b.append(JPrimitiveType.parse(type.getName()).getJNISignature());
    else {
      b.append('L');
      String[] bits = type.getPackage().getName().split("[.]");
      if (bits.length > 0) {
        for (int i = 0, m = bits.length; i < m; i ++) {
          b.append(bits[i]);
          b.append('/');
        }
        b.append(type.getName().substring(type.getPackage().getName().length()+1));
      } else {
        b.append(type.getName());
      }
      b.append(';');
    }
    return b.toString();
  }

  public static String toJsniValue(Annotation anno, Method m) {
    try {
      Object o = m.invoke(anno);
      StringBuilder b = new StringBuilder();
      buildJsniConstructor(b, o);
      return b.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void buildJsniConstructor(StringBuilder b, Object o) {
    assert o != null;
    if (o.getClass().isArray()) {

    }
    if (o instanceof Number) {

    } else if (o instanceof String) {
    } else if (o instanceof Class) {
    } else if (o instanceof Boolean) {
    } else if (o instanceof Annotation) {
    } else if (o instanceof Enum) {
    } else if (o instanceof Character) {

    }
  }

  public static String sourceName(Object defaultValue) {
    StringBuilder b = new StringBuilder();
    if (defaultValue instanceof Enum<?>) {
      Enum<?> e = (Enum<?>)defaultValue;
      b.append(e.getDeclaringClass().getCanonicalName());
      b.append('.');
      b.append(e.name());
    } else if (defaultValue instanceof Class<?>) {
      b.append(((Class<?>)defaultValue).getCanonicalName()+".class");
    } else if (defaultValue instanceof Annotation) {
      b.append(ReflectionGeneratorUtil.annotationToString((Annotation)defaultValue));
    } else if (defaultValue.getClass().isArray()) {
      Class<?> c = defaultValue.getClass().getComponentType();
      b.append("new ");
      b.append(c.getCanonicalName());
      b.append("[]{ ");
      int length = GwtReflect.arrayLength(defaultValue);
      if (length > 0) {
        b.append(sourceName(GwtReflect.arrayGet(defaultValue, 0)));
      }
      for (int i = 1; i < length; i++ ) {
        b.append(", ");
        b.append(sourceName(GwtReflect.arrayGet(defaultValue, i)));
      }
      b.append("}");
    } else if (defaultValue instanceof String) {
      b.append('"');
      b.append(GwtReflect.escape((String)defaultValue));
      b.append('"');
    } else {
      // a primitive
      b.append(defaultValue);
      if (defaultValue instanceof Long) {
        if (((Long)defaultValue).longValue() > Integer.MAX_VALUE) {
          b.append('L');
        }
      }
    }
    return b.toString();
  }

  public static String annotationToString(Annotation defaultValue) {
    if (GWT.isScript()) {
      return defaultValue.toString();
    } else {
      // dev mode has to dig for the serialized annotation.
      StringBuilder b = new StringBuilder("@");
      Class<? extends Annotation> cls = defaultValue.annotationType();
      b.append(cls.getCanonicalName());
      Method[] methods = cls.getMethods();
      if (methods.length > 0) {
        b.append('(');
        for (int i = 0, m = methods.length; i < m; i ++) {
          Method method = methods[i];
          if (method.getDeclaringClass().getName().equals("java.lang.Object"))
            continue;
          if (method.getName().equals("annotationType"))
            continue;
          if (i > 0)
            b.append(", ");
          b.append(method.getName());
          b.append('=');
          try {
            b.append(sourceName(method.invoke(defaultValue)));
          } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        }
        b.append(')');
      }
      return b.toString();
    }
  }

  public static JClassLiteral extractClassLiteral(TreeLogger logger, JMethodCall methodCall, int paramPosition) throws UnableToCompleteException {
    List<JExpression> args = methodCall.getArgs();
    JExpression arg = args.get(paramPosition);
    JClassLiteral classLit = extractClassLiteral(logger, arg, false);
    if (classLit == null) {
      logger.log(Type.ERROR, "The method " +
        methodCall.getTarget().toSource() + " only accepts class literals." +
        " You sent a " + arg.getClass() + " : " + arg.toSource()+";");
      throw new UnableToCompleteException();
    }
    return classLit;
  }

  public static String toSourceName(ParameterizedType returnType) {
    StringBuilder b = new StringBuilder();
    Class<?> c = (Class<?>)returnType.getRawType();
    int arrayDepth = 0;

    while (c.getComponentType() != null) {
      arrayDepth ++;
      c = c.getComponentType();
    }
    b.append(c.getCanonicalName());
    java.lang.reflect.Type[] params = returnType.getActualTypeArguments();
    if (params.length > 0) {
      b.append('<');
      b.append(toSourceName(params[0]));
      for (int i = 1, m = params.length; i < m; i ++ ) {
        b.append(", ").append(toSourceName(params[0]));
      }
      b.append("> ");
    }
    for(; arrayDepth --> 0; ) {
      b.append("[]");
    }
    return b.toString();
  }

  public static String toSourceName(GenericArrayType type) {
    return toSourceName(type.getGenericComponentType())+"[]";
  }
  
  public static String toSourceName(WildcardType type) {
    StringBuilder b = new StringBuilder("?");
    java.lang.reflect.Type[] bounds;
    if (type.getUpperBounds().length > 0) {
      b.append(" extends ");
      bounds = type.getUpperBounds();
    } else if (type.getLowerBounds().length > 0){
      b.append(" super ");
      bounds = type.getLowerBounds();
    } else {
      return b.toString();
    }
    b.append(toSourceName(bounds[0]));
    for (int i = 1, m = bounds.length; i < m; i ++) {
      b.append(" & ").append(toSourceName(bounds[i]));
    }
    return b.toString();
  }
  public static String toSourceName(java.lang.reflect.Type type) {
    if (type instanceof Class) {
      return toSourceName((Class<?>)type);
    } else if (type instanceof TypeVariable) {
      return toSourceName((TypeVariable<?>)type);
    } else if (type instanceof ParameterizedType){
      return toSourceName((ParameterizedType)type);
    } else if (type instanceof WildcardType){
      return toSourceName((WildcardType)type);
    } else if (type instanceof GenericArrayType){
      return toSourceName((GenericArrayType)type);
    } else {
      System.err.println("Unknown type "+type+"; "+type.getClass().getName());
      throw new RuntimeException();
    }
  }

  public static String toSourceName(Class<?> returnType) {
    StringBuilder b = new StringBuilder();
    int arrayDepth = 0;
    while (returnType.getComponentType() != null) {
      arrayDepth ++;
      returnType = returnType.getComponentType();
    }
    b.append(returnType.getCanonicalName());
    TypeVariable<?>[] params = returnType.getTypeParameters();
    if (params.length > 0) {
      b.append('<');
      b.append(toSourceName(params[0]));
      for (int i = 1, m = params.length; i < m; i ++ ) {
        b.append(", ").append(toSourceName(params[0]));
      }
      b.append("> ");
    }
    for (;arrayDepth-->0;) {
      b.append("[]");
    }
    return b.toString();
  }

  public static String toSourceName(TypeVariable<?> typeVariable) {
    StringBuilder b = new StringBuilder();
    b.append(typeVariable.getName());
    java.lang.reflect.Type[] bounds = typeVariable.getBounds();
    if (bounds.length > 0) {
      b.append(toSourceName(bounds[0]));
      for (int i = 1, m = bounds.length; i < m; i++) {
        b.append(", ").append(toSourceName(bounds[0]));
      }
    }
    return b.toString();
  }

  public static JClassLiteral extractClassLiteral(TreeLogger logger, JExpression inst, boolean strict) throws UnableToCompleteException {
    return extractImmutableNode(logger, JClassLiteral.class, inst, strict);
  }
  public static <X extends JExpression> X extractImmutableNode(TreeLogger logger, Class<X> type, JExpression inst, boolean strict) throws UnableToCompleteException {
    boolean doLog = logger.isLoggable(Type.TRACE);
    if (inst == null) {
      failIfStrict(logger, strict, inst, type);
      return null;
    }
    else if (type.isAssignableFrom(inst.getClass())) {
      // We have a winner!
      return (X)inst;
    } else if (inst instanceof JLocalRef) {
        JLocal local = ((JLocalRef)inst).getLocal();
        if (local.isFinal()) {
          JExpression localInit = local.getInitializer();
          if (localInit == null) {
            inst = localInit;
          } else {
            return extractImmutableNode(logger, type, localInit, true);
          }
        } else {
          if (doLog) logNonFinalError(logger, inst);
        }
    } else if (inst instanceof JFieldRef) {
      com.google.gwt.dev.jjs.ast.JField field = ((JFieldRef)inst).getField();
      if (field.isFinal()) {
        return extractImmutableNode(logger, type, field.getInitializer(), strict);
      } else {
        if (doLog) logNonFinalError(logger, inst);
      }
    } else if (inst instanceof JMethodCall){
      JMethodCall call = (JMethodCall)inst;
      JMethod target = (call).getTarget();
      if (isGetMagicClass(target)) {
        return extractImmutableNode(logger, type, call.getArgs().get(0), strict);
      }
      JAbstractMethodBody method = target.getBody();
      // TODO: maybe enforce final / static on method calls
      if (method.isNative()) {
        JsniMethodBody jsni = (JsniMethodBody)method;
        if (JClassLiteral.class.isAssignableFrom(type)) {
          List<JsniClassLiteral> literals = jsni.getClassRefs();
          if (literals.size() == 1) {
            // Might want to not allow jsni methods to magically tag class literals...
            return (X)literals.get(0);
          }
        }
      } else {
        JMethodBody java = (JMethodBody)method;
        ArrayList<JReturnStatement> returns = new ArrayList<JReturnStatement>();
        for (JStatement statement : java.getStatements()) {
          if (statement instanceof JReturnStatement)
            returns.add((JReturnStatement)statement);
        }
        if (returns.size() == 1) {
          return extractImmutableNode(logger, type, returns.get(0).getExpr(), strict);
        } else {
          if (logger.isLoggable(Type.TRACE)) {
            logger.log(Type.TRACE, "Java "+type.getName()+" provider method must have one " +
              "and only one return statement, which returns a "+ type.getName()+ " " + method);
          }
        }
      }
    } else {
      if (isUnknownType(inst)) {
        logger.log(Type.WARN, "Encountered unhandled type while searching for "+
            type.getName()+ ": "+debug(inst));
      }
    }
    failIfStrict(logger, strict, inst, type);
    return null;
  }

  private static boolean isGetMagicClass(JMethod target) {
    return 
        target.getName().equals("magicClass") &&
        target.getEnclosingType().getName().equals(GwtReflect.class.getName());
  }

  private static void failIfStrict(TreeLogger logger, boolean strict,
      JExpression inst, Class<?> type) throws UnableToCompleteException {
    if (strict) {
      logger.log(Type.TRACE, "Unable to acquire a " + type.getCanonicalName()
          + " from "+debug(inst));
      throw new UnableToCompleteException();
    }
  }

  public static String debug(JExpression inst) {
    return inst.getClass()+" ["+inst.toSource()+"] @"+inst.getSourceInfo();
  }

  private static boolean isUnknownType(JExpression inst) {
    return !(inst instanceof JParameterRef);
  }

  private static void logNonFinalError(TreeLogger logger, JExpression inst) {
    logger.log(Type.TRACE, "Traced class literal down to a "+ debug(inst)+","
        + " but this member was not marked final."
        + " Aborting class literal search due to lack of determinism.");
  }

  public static String toUniqueName(
      List<JParameter> params, List<JMethod> methods) {
    int length = params.size();
    ArrayList<JMethod> sameSize = new ArrayList<JMethod>();
    for (JMethod method : methods) {
      if (method.getParams().size() == length)
        sameSize.add(method);
    }
    if (sameSize.size() == 1) {
      return "";
    }
    HashSet<String> unique = new HashSet<String>();
    for (JMethod method : sameSize) {
      String simple = simplify(method.getParams());
      unique.add(simple);
    }
    if (unique.size() == sameSize.size()) {
      return simpleType(params);
    }
    return verboseType(params);
  }
  public static String toUniqueFactory(
      com.google.gwt.core.ext.typeinfo.JParameter params[], com.google.gwt.core.ext.typeinfo.JMethod[] methods) {
    int length = params.length;
    ArrayList<com.google.gwt.core.ext.typeinfo.JMethod> sameSize = new ArrayList<com.google.gwt.core.ext.typeinfo.JMethod>();
    for (com.google.gwt.core.ext.typeinfo.JMethod method : methods) {
      if (method.getParameters().length == length)
        sameSize.add(method);
    }
    if (sameSize.size() == 1) {
      return "";
    }
    HashSet<String> unique = new HashSet<String>();
    for (com.google.gwt.core.ext.typeinfo.JMethod method : sameSize) {
      String simple = simplify(method.getParameters());
      unique.add(simple);
    }
    if (unique.size() == sameSize.size()) {
      return simpleParams(params);
    }
    return verboseParams(params);
  }

  private static String simplify(List<JParameter> params) {
    StringBuilder b = new StringBuilder();
    for (JParameter param : params) {
      b.append(param.getType().getName().charAt(0));
    }
    return b.toString();
  }

  private static String simplify(com.google.gwt.core.ext.typeinfo.JParameter[] params) {
    StringBuilder b = new StringBuilder();
    for (com.google.gwt.core.ext.typeinfo.JParameter param : params) {
      b.append(param.getType().getErasedType().getQualifiedSourceName().charAt(0));
    }
    return b.toString();
  }

  private static String simpleType(List<JParameter> params) {
    StringBuilder b = new StringBuilder();
    for (JParameter param : params) {
      b.append(param.getType().getName().charAt(0));
    }
    return b.toString();
  }
  
  private static String verboseType(List<JParameter> params) {
    StringBuilder b = new StringBuilder();
    for (JParameter param : params) {
      b.append(param.getType().getName()).append('_');
    }
    return b.toString();
  }

  private static String simpleParams(com.google.gwt.core.ext.typeinfo.JParameter[] params) {
    StringBuilder b = new StringBuilder();
    for (com.google.gwt.core.ext.typeinfo.JParameter param : params) {
      b.append(param.getType().getErasedType().getQualifiedSourceName().charAt(0));
    }
    return b.toString();
  }
  
  private static String verboseParams(com.google.gwt.core.ext.typeinfo.JParameter[] params) {
    StringBuilder b = new StringBuilder();
    for (com.google.gwt.core.ext.typeinfo.JParameter param : params) {
      String name = param.getType().getErasedType().getSimpleSourceName();
      name = name.replaceAll("\\[\\]", "");
      b.append(name).append('_');
    }
    return b.toString();
  }

  public static com.google.gwt.core.ext.typeinfo.JMethod transform(
      TypeOracle oracle, JMethod method) {
    com.google.gwt.core.ext.typeinfo.JClassType type = oracle.findType(method.getEnclosingType().getName());
    loop:
    for (com.google.gwt.core.ext.typeinfo.JMethod m : type.getMethods()) {
      if (m.getName().equals(method.getName())) {
        com.google.gwt.core.ext.typeinfo.JParameter[] p1 = m.getParameters();
        List<JParameter> p2 = method.getParams();
        if (p1.length == p2.size()) {
          for (int i = 0, max = p1.length; i < max; i++ ) {
            com.google.gwt.core.ext.typeinfo.JParameter param1 = p1[i];
            JParameter param2 = p2.get(i);
            if (!param1.getType().getErasedType().getJNISignature().equals(param2.getType().getJsniSignatureName()))
              continue loop;
          }
          return m;
        }
      }
    }
    return null;
  }

  public static int getModifier(com.google.gwt.core.ext.typeinfo.JMethod method) {
    return method.isPublic() ? Modifier.PUBLIC : 
           method.isProtected() ? Modifier.PROTECTED : 
           method.isPrivate() ? Modifier.PRIVATE : 
           0;
  }

  public static Annotation[] extractAnnotations(int annotationRetention,
      HasAnnotations method) {
    ArrayList<Annotation> annos = new ArrayList<Annotation>();
    boolean keepClass = (annotationRetention | ReflectionStrategy.COMPILE) == annotationRetention;
    boolean keepRuntime = (annotationRetention | ReflectionStrategy.RUNTIME) == annotationRetention;
    for (Annotation anno : method.getAnnotations()) {
      Retention retention = anno.annotationType().getAnnotation(Retention.class);
      if (retention == null) {
        if (keepClass)
          annos.add(anno);
      } else {
        switch (retention.value()) {
          case CLASS:
            if (keepClass)
              annos.add(anno);
            break;
          case RUNTIME:
            if (keepRuntime)
              annos.add(anno);
          default:
        }
      }
    }
    return annos.toArray(new Annotation[annos.size()]);
  }

}
