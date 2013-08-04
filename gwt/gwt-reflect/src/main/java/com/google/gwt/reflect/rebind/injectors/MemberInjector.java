package com.google.gwt.reflect.rebind.injectors;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator.GeneratedAnnotation;
import com.google.gwt.reflect.rebind.generators.MagicClassGenerator.MemberFilter;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorUtil;
import com.google.gwt.reflect.rebind.generators.ReflectionTypeUtil;

@ReflectionStrategy
public class MemberInjector {

  public static final ReflectionStrategy DEFAULT_STRATEGY = MemberInjector.class.getAnnotation(ReflectionStrategy.class);

  public static final String METHOD_SPACER = "_m_";
  public static final String FIELD_SPACER = "_f_";
  public static final String CONSTRUCTOR_SPACER = "_c_";

  public boolean extractMethods(TreeLogger logger, ReflectionStrategy strategy, MemberFilter<JMethod> keepMethod, JClassType injectionType,
    ReflectionManifest manifest) {
    Set<String> seen = new HashSet<String>();
    Set<? extends JClassType> allTypes = injectionType.getFlattenedSupertypeHierarchy();

    for(JClassType nextClass : allTypes) {
      for (JMethod method : nextClass.getMethods()) {
        if (keepMethod.keepMember(method, method.getEnclosingType() == injectionType, method.isPublic(), strategy)){
          // do not include overridden methods
          // TODO check for covariance?
          String sig = method.getName()+"("+ReflectionTypeUtil.toJsniClassLits(method.getParameterTypes())+")";
          if (seen.add(sig)) {
            final Annotation[] annos;
              final List<Annotation> keepers = new ArrayList<Annotation>();
              for (Annotation anno : method.getAnnotations()) {
                if (keepMethod.keepAnnotation(method, anno, strategy))
                  keepers.add(anno);
              }
              annos = keepers.toArray(new Annotation[keepers.size()]);
            manifest.methods.put(method, annos);
          }
        }
      }
      nextClass = nextClass.getSuperclass();
    }
    return true;
  }

  protected static String getFactoryName(JClassType type, String name,
      com.google.gwt.core.ext.typeinfo.JParameter[] list, com.google.gwt.core.ext.typeinfo.JMethod[] polymorphs) {
    StringBuilder b = new StringBuilder(type.getName());
    b.append(METHOD_SPACER).append(name);
    // Check for polymorphism
    if (polymorphs.length > 1) {
      // Have to use the parameters to make a unique name. 
      // Might be worth it to try a deterministic hash first :(
      String uniqueName = ReflectionGeneratorUtil.toUniqueFactory(list, polymorphs);
      b.append('_').append(uniqueName);
    }
    return b.toString();
  }

  public String generateMethodFactory(TreeLogger logger, GeneratorContext ctx,
      JMethod method, ReflectionManifest manifest) throws UnableToCompleteException {
    String methodFactoryName = getFactoryName(method.getEnclosingType(), method.getName(), method.getParameters(), method.getEnclosingType().getMethods());
    return generateMethodFactory(logger, ctx, method, methodFactoryName, manifest);
  }
  public String generateMethodFactory(TreeLogger logger, GeneratorContext ctx,
      JMethod method, String factoryName, ReflectionManifest manifest) throws UnableToCompleteException {
    String pkg = method.getEnclosingType().getPackage().getName();
    JClassType type = method.getEnclosingType();
    factoryName = factoryName.replace('.', '_');
    SourceBuilder<JMethod> out = new SourceBuilder<JMethod>
      ("public class "+factoryName+" extends Method").setPackage(pkg);
    ClassBuffer cb = out.getClassBuffer();
    cb.addImports(Method.class)
      .createMethod("public static "+factoryName+" instantiate()")
      .returnValue("new "+factoryName+"()");
    
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
      Annotation[] annos = ReflectionGeneratorUtil.extractAnnotations(retention.annotationRetention(), method);
      generateGetAnnos(logger, out, annos, ctx);
    } else {
      generateGetAnnos(logger, out, new Annotation[0], ctx);
    }
    
    generateGetParams(logger, cb, method);
    generateGetReturnType(logger, cb, method);
    generateGetName(logger, cb, method);
    generateGetModifier(logger, cb, method);
    generateGetDeclaringClass(logger, cb, method);
    
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
  
  private void generateGetModifier(TreeLogger logger, ClassBuffer cb, JMethod method) {
    cb.createMethod("public int getModifiers()").returnValue(Integer.toString(ReflectionGeneratorUtil.getModifier(method)));
  }
  private void generateGetDeclaringClass(TreeLogger logger, ClassBuffer cb, JMethod method) {
    MethodBuffer getDeclaringClass = cb.createMethod("public Class<?> getDeclaringClass()");
    if (method.getEnclosingType().isPrivate()) {
      getDeclaringClass
        .setUseJsni(true)
        .returnValue("@"+method.getEnclosingType().getQualifiedSourceName()+"::class");
    } else {
      getDeclaringClass
        .returnValue(method.getEnclosingType().getQualifiedSourceName()+".class");
    }
  }
  
  private void generateGetParams(TreeLogger logger, ClassBuffer cb, JMethod method) {
    MethodBuffer getParameters = cb
      .createMethod("public Class<?>[] getParameterTypes()")
      .println("return new Class<?>[]{");
    JParameter[] params = method.getParameters();
    if (params.length > 0) {
      getParameters.println(toClass(params[0]));
      for (int i = 1, m = params.length; i < m; i ++) {
        getParameters.println(", "+toClass(params[i]));
      }
    }
    getParameters.println("};");
  }
  private String toClass(JParameter param) {
    return param.getType().getErasedType().getQualifiedSourceName()+".class";
  }
  
}
