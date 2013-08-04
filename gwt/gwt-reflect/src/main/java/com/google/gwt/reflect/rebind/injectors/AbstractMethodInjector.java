package com.google.gwt.reflect.rebind.injectors;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorUtil;

public class AbstractMethodInjector extends MemberInjector {

  public JMethodCall getMethodFactory(TreeLogger logger, UnifyAstView ast, JMethod method,
      JClassLiteral classLit, boolean declaredOnly) throws UnableToCompleteException {
    String clsName = classLit.getRefType().getName();
    TypeOracle oracle = ast.getTypeOracle();
    ReflectionManifest manifest = ReflectionManifest.getReflectionManifest(logger, clsName, ast.getGeneratorContext());
    com.google.gwt.core.ext.typeinfo.JMethod m = ReflectionGeneratorUtil.transform(oracle, method);
    String factoryCls = getOrMakeMethodFactory(logger, ast.getRebindPermutationOracle().getGeneratorContext(), m, m.getEnclosingType(), manifest, declaredOnly);
    ast.getRebindPermutationOracle().getGeneratorContext().finish(logger);
    JDeclaredType factory = ast.searchForTypeBySource(factoryCls);
    // pull out the static accessor method
    for (JMethod factoryMethod : factory.getMethods()) {
      if (factoryMethod.isStatic() && factoryMethod.getName().equals("instantiate")) {
        return new JMethodCall(factoryMethod.getSourceInfo(), null, factoryMethod);
      }
    }
    logger.log(Type.ERROR, "Unable to find static initializer for Method subclass "+factoryCls);
    throw new UnableToCompleteException();
  }
  public String getOrMakeMethodFactory(TreeLogger logger, GeneratorContext ctx, com.google.gwt.core.ext.typeinfo.JMethod method,
      JType classType, ReflectionManifest manifest, boolean declaredOnly) throws UnableToCompleteException {
    // get cached manifest for this type
    String clsName = classType.getQualifiedSourceName();
    TypeOracle oracle = ctx.getTypeOracle();
    String name = method.getName();
    JClassType cls = oracle.findType(clsName);
    if (cls == null) {
      logger.log(Type.ERROR, "Unable to find enclosing class "+clsName);
      throw new UnableToCompleteException();
    }
    ArrayList<com.google.gwt.core.ext.typeinfo.JMethod> list = manifest.getMethodsNamed(logger, name, declaredOnly, ctx);
    if (list.size() == 0) {
      logger.log(Type.ERROR, "Unable to find any method named "+name+" in "+clsName);
      throw new UnableToCompleteException();
    }
      
    String methodFactoryName = getFactoryName(cls, name, method.getParameters(), list.toArray(new com.google.gwt.core.ext.typeinfo.JMethod[0]));
    JClassType factory;
    String pkgName = method.getEnclosingType().getPackage().getName();
    factory = oracle.findType(pkgName, methodFactoryName);
    if (factory == null) {
      return generateMethodFactory(logger, ctx, method, methodFactoryName, manifest);
    } else 
      return (pkgName.length()==0?"":pkgName+".")+ methodFactoryName;
  }

  protected com.google.gwt.dev.jjs.ast.JClassType generateMethodFactory(TreeLogger logger, UnifyAstView ast,
      JMethod method, String methodFactoryName, ReflectionManifest manifest) {
    
    int ind = methodFactoryName.lastIndexOf('.');
    String pkg = ind == -1 ? "" : methodFactoryName.substring(0, ind);
    String factoryName = methodFactoryName.replace(pkg+".", "");
    JDeclaredType type = method.getEnclosingType();
    SourceBuilder<JMethod> out = new SourceBuilder<JMethod>
      ("public class "+factoryName+" extends Method").setPackage(pkg);
    ClassBuffer cb = out.getClassBuffer();
    
    cb.addImports(Method.class)
      .createMethod("public static "+factoryName+" instantiate()")
      .returnValue("new "+factoryName+"()");
    
    MethodBuffer invoke = cb.createMethod(
        "public final native Object invoke(Object o, Object ... args)")
        .makeJsni()
        .print("return ");
    if (!method.isStatic())
      invoke.print("o.");
    invoke
      .print("@"+type.getName()+"::")
      .print(method.getName()+"(");
    JParameter[] params = method.getParams().toArray(new JParameter[0]);
    for (JParameter param : params) {
      invoke.print(param.getType().getJsniSignatureName());
    }
    invoke.println(")").print("(");
    for (int i = 0, m = params.length; i < m; i++ ) {
      if (i > 0)
        invoke.print(", ");
      JParameter param = params[i];
      if (param.getType() instanceof JPrimitiveType) {
        // unbox primitives!
      }
      invoke.print("args["+i+"]");
    }
    invoke.println(");");
    
    StandardGeneratorContext ctx = ast.getRebindPermutationOracle().getGeneratorContext();
    PrintWriter pw = ctx.tryCreate(logger, pkg, factoryName);
    
    pw.println(out.toString());
    
    ctx.commit(logger, pw);
    ctx.finish(logger);
    
    return (com.google.gwt.dev.jjs.ast.JClassType) ast.searchForTypeBySource(methodFactoryName);
  }

  private static ArrayList<JMethod> getMethods(JDeclaredType cls, String name) {
    ArrayList<JMethod> list = new ArrayList<JMethod>();
    for (JMethod method : cls.getMethods()) {
      if (method.getName().equals(name)) {
        list.add(method);
      }
    }
    return list;
  }
  private static ArrayList<com.google.gwt.core.ext.typeinfo.JMethod> getMethods(JClassType cls, String name) {
    ArrayList<com.google.gwt.core.ext.typeinfo.JMethod> list = new ArrayList<com.google.gwt.core.ext.typeinfo.JMethod>();
    for (com.google.gwt.core.ext.typeinfo.JMethod method : cls.getMethods()) {
      if (method.getName().equals(name)) {
        list.add(method);
      }
    }
    return list;
  }

  private static String getFactoryName(JDeclaredType type, String name,
      List<JParameter> list, List<JMethod> polymorphs) {
    StringBuilder b = new StringBuilder(type.getName());
    b.append(METHOD_SPACER).append(name).append("_");
    // Check for polymorphism
    if (polymorphs.size() > 1) {
      // Have to use the parameters to make a unique name. 
      // Might be worth it to try a deterministic hash first :(
      String uniqueName = ReflectionGeneratorUtil.toUniqueName(list, polymorphs);
      b.append('_').append(uniqueName);
    }
    return b.toString();
  }
  
}
