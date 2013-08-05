package com.google.gwt.reflect.rebind.generators;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilType;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator.GeneratedAnnotation;
import com.google.gwt.reflect.rebind.generators.MagicClassGenerator.MemberFilter;

@ReflectionStrategy
public class MemberGenerator {

  public static final ReflectionStrategy DEFAULT_STRATEGY = MemberGenerator.class.getAnnotation(ReflectionStrategy.class);

  public static final String METHOD_SPACER = "_m_";
  public static final String FIELD_SPACER = "_f_";
  public static final String CONSTRUCTOR_SPACER = "_c_";

  public static String getFieldFactoryName(JClassType type, String name) {
    StringBuilder b = new StringBuilder(type.getName());
    b.append(FIELD_SPACER).append(name);
    return b.toString();
  }
  
  public static String getConstructorFactoryName(JClassType type, JParameter[] list) {
    StringBuilder b = new StringBuilder(type.getName());
    b.append(CONSTRUCTOR_SPACER).append(ReflectionUtilType.toUniqueFactory(list, type.getConstructors()));
    return b.toString();
  }
  
  public static String getMethodFactoryName(JClassType type, String name,
      JParameter[] list) {
    StringBuilder b = new StringBuilder(type.getName());
    b.append(METHOD_SPACER).append(name);
    // Check for polymorphism
    JMethod[] overloads = type.getOverloads(name);
    if (overloads.length > 1) {
      // Have to use the parameters to make a unique name. 
      // Might be worth it to try a deterministic hash first :(
      String uniqueName = ReflectionUtilType.toUniqueFactory(list, overloads);
      b.append('_').append(uniqueName);
    }
    return b.toString();
  }

  public String generateFieldFactory(TreeLogger logger, GeneratorContext ctx,
      JField field, String factoryName, ReflectionManifest manifest) throws UnableToCompleteException {
    String pkg = field.getEnclosingType().getPackage().getName();
    JClassType enclosingType = field.getEnclosingType();
    JType fieldType = field.getType().getErasedType();
    String jni = field.getType().getJNISignature();
    factoryName = factoryName.replace('.', '_');
    
    SourceBuilder<JField> out = new SourceBuilder<JField>
    ("public final class "+factoryName).setPackage(pkg);
    ClassBuffer cb = out.getClassBuffer();

    final String ref = (field.isStatic() ? "" : "o.")+"@"+enclosingType.getQualifiedSourceName()+"::"+field.getName();
    MethodBuffer accessor = cb
        .createMethod("private static JavaScriptObject getAccessor()")
        .setUseJsni(true)
        .println("return {").indent()
        .println("getter: function(o) {")
        .indentln("return "+ref+";")
        .print("}")
    ;
    if (field.isFinal()) {
      accessor.println().outdent().println("};");
    } else {
      accessor
          .println(", setter: function(o, v) {")
          .indentln(ref+" = v;")
          .println("}")
          .outdent().println("};")
      ;
    }
    
    MethodBuffer instantiate = cb
      .createMethod("public static Field instantiate()")
      .print("return new ")
      .addImports(Field.class, JavaScriptObject.class);
    
    boolean isPrimitive = fieldType.isPrimitive() != null;
    if (isPrimitive) {
      switch (jni.charAt(0)) {
      case 'Z':
        instantiate.addImport("java.lang.reflect.Boolean_Field");
        instantiate.print("Boolean_Field(");
        break;
      case 'B':
        instantiate.print("Field(byte.class, ");
        break;
      case 'S':
        instantiate.print("Field(short.class, ");
        break;
      case 'C':
        instantiate.print("Field(char.class, ");
        break;
      case 'I':
        instantiate.print("Field(int.class, ");
        break;
      case 'J':
        accessor.addAnnotation(UnsafeNativeLong.class);
        instantiate.print("Field(long.class, ");
        break;
      case 'F':
        instantiate.print("Field(float.class, ");
        break;
      case 'D':
        instantiate.print("Field(double.class, ");
        break;
      default:
        logger.log(Type.ERROR, "Bad primitive type in field generator "+fieldType.getQualifiedSourceName());
        throw new UnableToCompleteException();
      }
    } else {
      JPrimitiveType isPrimitiveWrapper = ReflectionUtilType.isPrimitiveWrapper(fieldType);
      if (isPrimitiveWrapper == null) {
        // Normal field
        String imported = instantiate.addImport(fieldType.getQualifiedSourceName());
        instantiate.print("Field("+imported+".class, ");
      } else {
        // Number / Boolean / Character field
        switch (isPrimitiveWrapper.getJNISignature().charAt(0)) {
        case 'Z':
          instantiate.addImport("java.lang.reflect.BooleanField");
          instantiate.print("BooleanField(");
          break;
        case 'B':
          instantiate.print("Field(Byte.class, ");
          break;
        case 'S':
          instantiate.print("Field(Short.class, ");
          break;
        case 'C':
          instantiate.print("Field(Character.class, ");
          break;
        case 'I':
          instantiate.print("Field(Integer.class, ");
          break;
        case 'J':
          accessor.addAnnotation(UnsafeNativeLong.class);
          instantiate.print("Field(Long.class, ");
          break;
        case 'F':
          instantiate.print("Field(Float.class, ");
          break;
        case 'D':
          instantiate.print("Field(Double.class, ");
          break;
        default:
          logger.log(Type.ERROR, "Bad primitive type in field generator "+enclosingType.getName());
          throw new UnableToCompleteException();
        }
      }
    }
    String enclosing = instantiate.addImport(field.getEnclosingType().getQualifiedSourceName());
    instantiate
        .print(enclosing+".class, ")
        .print("\""+field.getName()+"\", ")
        .print(ReflectionUtilType.getModifiers(field)+", getAccessor());");

    GwtRetention retention = manifest.getRetention(field);
    if (retention.annotationRetention() > 0) {
      Annotation[] annos = ReflectionUtilType.extractAnnotations(retention.annotationRetention(), field);
      generateGetAnnos(logger, out, annos, ctx);
    } else {
      generateGetAnnos(logger, out, new Annotation[0], ctx);
    }
    
    PrintWriter pw = ctx.tryCreate(logger, pkg, factoryName);
    
    pw.println(out.toString());

    ctx.commit(logger, pw);
    return out.getQualifiedName();
  }

  public String generateConstructorFactory(TreeLogger logger, GeneratorContext ctx,
      JConstructor ctor, String factoryName, ReflectionManifest manifest) throws UnableToCompleteException {
    String pkg = ctor.getEnclosingType().getPackage().getName();
    JClassType type = ctor.getEnclosingType();
    factoryName = factoryName.replace('.', '_');
    SourceBuilder<JConstructor> out = new SourceBuilder<JConstructor>
    ("public final class "+factoryName+" extends Constructor").setPackage(pkg);

    String simpleName = out.getImports().addImport(type.getQualifiedSourceName());
    
    ClassBuffer cb = out.getClassBuffer();
    cb
        .addImports(Constructor.class)
        .setSuperClass("Constructor <"+simpleName+">")
        .createMethod("public static "+factoryName+" instantiate()")
        .returnValue("new "+factoryName+"()");
    
    
    MethodBuffer invoke = cb.createMethod(
        "public final native "+simpleName+ " newInstance(Object ... args)")
        .makeJsni();
    
    invoke.print("return ");
    
    invoke.print("@"+type.getQualifiedSourceName()+"::new(");
    JParameter[] params = ctor.getParameters();
    boolean hasLong = false;
    for (JParameter param : params) {
      if (param.getType().isPrimitive() == com.google.gwt.core.ext.typeinfo.JPrimitiveType.LONG) {
        hasLong = true;
      }
      invoke.print(param.getType().getJNISignature());
    }
    if (hasLong)
      invoke.addAnnotation(UnsafeNativeLong.class);
    
    invoke.print(")(");
    for (int i = 0, m = params.length; i < m; i++ ) {
      if (i > 0)
        invoke.print(", ");
      JParameter param = params[i];
      if (param.getType().isPrimitive() != null) {
        // unbox primitives!
        StringBuilder b = new StringBuilder();
        printUnboxing(b, param.getType().isPrimitive(), "args["+i+"]");
        invoke.print(b.toString());
      } else {
        invoke.print("args["+i+"]");
      }
    }
    invoke.print(")");
    
    invoke.println(";");
    
    GwtRetention retention = manifest.getRetention(ctor);
    
    
    if (retention.annotationRetention() > 0) {
      Annotation[] annos = ReflectionUtilType.extractAnnotations(retention.annotationRetention(), ctor);
      generateGetAnnos(logger, out, annos, ctx);
    } else {
      generateGetAnnos(logger, out, new Annotation[0], ctx);
    }
    
    generateGetParams(logger, cb, ctor.getParameters());
    generateGetExceptions(logger, cb, ctor.getThrows());
    generateGetModifier(logger, cb, ReflectionUtilType.getModifiers(ctor));
    generateGetDeclaringClass(logger, cb, ctor.getEnclosingType(), simpleName);
    
    PrintWriter pw = ctx.tryCreate(logger, pkg, factoryName);
    
    pw.println(out.toString());
    
    
    ctx.commit(logger, pw);
    
    return out.getQualifiedName();
  }
  
  public String generateMethodFactory(TreeLogger logger, GeneratorContext ctx,
      JMethod method, String factoryName, ReflectionManifest manifest) throws UnableToCompleteException {
    String pkg = method.getEnclosingType().getPackage().getName();
    JClassType type = method.getEnclosingType();
    factoryName = factoryName.replace('.', '_');
    SourceBuilder<JMethod> out = new SourceBuilder<JMethod>
      ("public final class "+factoryName+" extends Method").setPackage(pkg);
    ClassBuffer cb = out.getClassBuffer();
    cb.addImports(Method.class)
      .createMethod("public static "+factoryName+" instantiate()")
      .returnValue("new "+factoryName+"()");
    
    MethodBuffer ctor = cb.createConstructor(Modifier.PRIVATE);
    ctor.println("super(0);");
    
    MethodBuffer invoke = cb.createMethod(
        "public final native Object invoke(Object o, Object ... args)")
        .makeJsni();
    
    if (!"V".equals(method.getReturnType().getJNISignature()))
        invoke.print("return ");

    if (method.getReturnType().getJNISignature().equals("J"))
      invoke.addAnnotation(UnsafeNativeLong.class);
    
    StringBuilder b = new StringBuilder();
    if (!method.isStatic())
      b.append("o.");
    b.append("@"+type.getQualifiedSourceName()+"::")
        .append(method.getName()+"(");
    JParameter[] params = method.getParameters();
    boolean hasLong = false;
    for (JParameter param : params) {
      if (param.getType().isPrimitive() == com.google.gwt.core.ext.typeinfo.JPrimitiveType.LONG) {
        hasLong = true;
      }
      b.append(param.getType().getJNISignature());
    }
    if (hasLong)
      invoke.addAnnotation(UnsafeNativeLong.class);
    
    b.append(")(");
    for (int i = 0, m = params.length; i < m; i++ ) {
      if (i > 0)
        b.append(", ");
      JParameter param = params[i];
      if (param.getType().isPrimitive() != null) {
        // unbox primitives!
        printUnboxing(b, param.getType().isPrimitive(), "args["+i+"]");
      } else {
        b.append("args["+i+"]");
      }
    }
    b.append(")");
    maybePrintBoxing(invoke, method.getReturnType(), b.toString());
    
    invoke.println(";");
    
    GwtRetention retention = manifest.getRetention(method);
    
    
    if (retention.annotationRetention() > 0) {
      Annotation[] annos = ReflectionUtilType.extractAnnotations(retention.annotationRetention(), method);
      generateGetAnnos(logger, out, annos, ctx);
    } else {
      generateGetAnnos(logger, out, new Annotation[0], ctx);
    }
    
    generateGetParams(logger, cb, method.getParameters());
    generateGetExceptions(logger, cb, method.getThrows());
    generateGetReturnType(logger, cb, method);
    generateGetName(logger, cb, method);
    generateGetModifier(logger, cb, ReflectionUtilType.getModifiers(method));
    generateGetDeclaringClass(logger, cb, method.getEnclosingType(), "?");
    
    PrintWriter pw = ctx.tryCreate(logger, pkg, factoryName);
    
    pw.println(out.toString());
    
    
    ctx.commit(logger, pw);
    
    return out.getQualifiedName();
  }
  
  private void printUnboxing(StringBuilder b, JPrimitiveType type, String source) {
    switch(type) {
    case BOOLEAN:
      b.append(source+".@java.lang.Boolean::booleanValue()()");
      break;
    case CHAR:
      b.append(source+".@java.lang.Character::charValue()()");
      break;
    case LONG:
      b.append("@"+GwtReflect.class.getName()+"::unboxLong(Ljava/lang/Long;)("+source+")");
      break;
    case BYTE:
    case DOUBLE:
    case INT:
    case FLOAT:
    case SHORT:
      b.append(source+".@java.lang.Number::doubleValue()()");
      break;
    case VOID:
    default:
      throw new AssertionError("Can't get here");
    }
  }

  private void maybePrintBoxing(PrintBuffer invoke, JType returnType, String source) {
    if (returnType.isPrimitive() == null) {
      invoke.print(source);
    } else {
      switch (returnType.isPrimitive()) {
      case BOOLEAN:
        invoke.print(source);
        invoke.print(" ? @java.lang.Boolean::TRUE : @java.lang.Boolean::FALSE");
        return;
      case LONG:
        invoke.print("@"+GwtReflect.class.getName()+"::boxLong(J)(");
        invoke.print(source);
        invoke.print(")");
        return;
      case BYTE:
        invoke.print("@java.lang.Byte::new(B)(");
        break;
      case CHAR:
        invoke.print("@java.lang.Character::new(C)(");
        break;
      case DOUBLE:
        invoke.print("@java.lang.Double::new(D)(");
        break;
      case FLOAT:
        invoke.print("@java.lang.Float::new(F)(");
        break;
      case INT:
        invoke.print("@java.lang.Integer::new(I)(");
        break;
      case SHORT:
        invoke.print("@java.lang.Short::new(S)(");
        break;
      case VOID:
        invoke.print(source);
        return;
      }
      invoke.print(source).print(")");
    }
  }
  private void generateGetAnnos(TreeLogger logger, SourceBuilder<?> sb, Annotation[] annos, GeneratorContext ctx) throws UnableToCompleteException {
    MethodBuffer getAnno = sb.getClassBuffer().createMethod(
        "public <T extends Annotation> T getAnnotation(Class<T> cls)")
        .setUseJsni(true);
    MethodBuffer getAnnos = sb.getClassBuffer().createMethod(
        "public Annotation[] getAnnotations()")
        .println("return new Annotation[]{");
    getAnnos.addImport(Annotation.class);
    if (annos.length > 0) {
      GeneratedAnnotation gen = GwtAnnotationGenerator.generateAnnotationProvider(logger, sb, annos[0], ctx);
      getAnnos.println(gen.providerClass()+"."+gen.providerMethod()+"()");
      getAnnos.addImport(gen.providerQualifiedName());
      getAnno
        .println("switch (@"+ConstPool.class.getName()+"::constId(Ljava/lang/Class;)(cls)) {")
        .indent()
        .print("case @"+ConstPool.class.getName()+"::constId(Ljava/lang/Class;)")
        .println("(@" + gen.getAnnoName() + "::class) :")
        .indentln(" return @" + gen.providerQualifiedName() + "::" + gen.providerMethod()+"()();");
      for (int i = 1, m = annos.length; i < m; i++ ) {
        gen = GwtAnnotationGenerator.generateAnnotationProvider(logger, sb, annos[i], ctx);
        getAnnos.println(", "+gen.providerClass()+"."+gen.providerMethod()+"()");
        getAnnos.addImport(gen.providerQualifiedName());
        getAnno
          .print("case @"+ConstPool.class.getName()+"::constId(Ljava/lang/Class;)")
          .println("(@" + gen.getAnnoName() + "::class) :")
          .indentln(" return @" + gen.providerQualifiedName() + "::" + gen.providerMethod()+"()();");
      }
      getAnno.outdent().println("}");
    }
    getAnno
      .println("return null;");
    getAnnos
      .println("};");
  }
  private void generateGetName(TreeLogger logger, ClassBuffer cb, JMethod method) {
    cb.createMethod("public String getName()").returnValue("\""+method.getName()+ "\"");
  }

  private void generateGetReturnType(TreeLogger logger, ClassBuffer cb, JMethod method) {
    cb.createMethod("public Class<?> getReturnType()").returnValue(
        method.getReturnType().getErasedType().getQualifiedSourceName()+".class");
  }
  
  private void generateGetModifier(TreeLogger logger, ClassBuffer cb, int mod) {
    cb.createMethod("public int getModifiers()").returnValue(Integer.toString(mod));
  }
  private void generateGetDeclaringClass(TreeLogger logger, ClassBuffer cb, JClassType type, String generic) {
    MethodBuffer getDeclaringClass = cb.createMethod("public Class<"+generic+ "> getDeclaringClass()");
    if (type.isPrivate()) {
      getDeclaringClass
        .setUseJsni(true)
        .returnValue("@"+type.getQualifiedSourceName()+"::class");
    } else {
      getDeclaringClass
        .returnValue(type.getQualifiedSourceName()+".class");
    }
  }
  
  private void generateGetParams(TreeLogger logger, ClassBuffer cb, JParameter[] params) {
    MethodBuffer getParameters = cb
      .createMethod("public Class<?>[] getParameterTypes()")
      .println("return new Class<?>[]{");
    if (params.length > 0) {
      getParameters.println(toClass(params[0]));
      for (int i = 1, m = params.length; i < m; i ++) {
        getParameters.println(", "+toClass(params[i]));
      }
    }
    getParameters.println("};");
  }
  
  private void generateGetExceptions(TreeLogger logger, ClassBuffer cb, JClassType[] exceptions) {
    MethodBuffer getExceptions = cb
        .createMethod("public Class<?>[] getExceptionTypes()")
        .println("return new Class<?>[]{");
    if (exceptions.length > 0) {
      getExceptions.println(toClass(exceptions[0]));
      for (int i = 1, m = exceptions.length; i < m; i ++) {
        getExceptions.println(", "+toClass(exceptions[i]));
      }
    }
    getExceptions.println("};");
  }
  
  private String toClass(JParameter param) {
    return param.getType().getErasedType().getQualifiedSourceName()+".class";
  }

  private String toClass(JClassType param) {
    return param.getErasedType().getQualifiedSourceName()+".class";
  }
  
}
