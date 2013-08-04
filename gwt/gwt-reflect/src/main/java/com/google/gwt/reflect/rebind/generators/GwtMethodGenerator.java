package com.google.gwt.reflect.rebind.generators;

import static com.google.gwt.core.ext.typeinfo.JPrimitiveType.BOOLEAN;
import static com.google.gwt.core.ext.typeinfo.JPrimitiveType.CHAR;
import static com.google.gwt.core.ext.typeinfo.JPrimitiveType.LONG;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator.GeneratedAnnotation;

public class GwtMethodGenerator extends ReflectionGeneratorUtil{

  /**
   * The jsni signature for the constructor of java.lang.Method<init>()
   *
   * We should just have a static global instance of the js function instead.
   */
  private static final String METHOD_CTOR = "new(" +
  		"Ljava/lang/Class;" +
  		"Ljava/lang/String;" +
  		"[Ljava/lang/Class;" +
  		"Ljava/lang/Class;" +
  		"[Ljava/lang/Class;" +
  		"I" +
  		"Lcom/google/gwt/core/client/JavaScriptObject;" +
  		"Lcom/google/gwt/core/client/JavaScriptObject;" +
  		")";

  public static void generateMethods(
    TreeLogger logger, SourceBuilder<?> sb, GeneratorContext context,
    JClassType type, ReflectionManifest manifest
  ) throws UnableToCompleteException {
    Map<JMethod, Annotation[]> methodMap = manifest.methods;
    JMethod[] methods = methodMap.keySet().toArray(new JMethod[methodMap.size()]);
    if (methods.length==0) {
      logger.log(Type.WARN, "No methods found for "+type.getQualifiedSourceName());
      return;
    }
    ClassBuffer out = sb.getClassBuffer();
    out
      .addImports(Method.class)
      .addImports("import static com.google.gwt.reflect.client.MemberMap.*;")
      ;

    // In order to provide methods, we need to rip the jsni function out;
    // since we don't know the runtime names of types and fields at generate time,
    // but we do know all of the constructor parameter types,
    // we use the class seedId that will be on the class literals at gwt runtime to construct keys,
    // and generate methods to return javascript functions that overlay the given method,


    // We want it to be lazily loaded, so we init as late as possible
    // and we cache results by replacing the initializer function
    // with one that simply returns the one-and-only-one Method object per method.

    MethodBuffer initMethod =
        out.createMethod("public static void enhanceMethods(Class<?> cls)")
    		.setUseJsni(true)
    		.addAnnotation(UnsafeNativeLong.class)
    		// grab the classes JSO "method repo" (dictionary from string to provider)
    		.println("var map = cls.@java.lang.Class::methods;")
    		.println("if (map) return;")// we may later implement super-class enhancement, to reduce code size
    		.println("map = cls.@java.lang.Class::methods = {};")
    		// pull out the constructor for Method, so we reduce the runtime lookup of the function
    		.println("var newMethod = @java.lang.reflect.Method::"+METHOD_CTOR+";")
		;

    // now, the fun part...  jsni extractors for our methods!
    for (int i = 0; i < methods.length; i++) {
      JMethod method = methods[i];
      boolean isDeclared = method.getEnclosingType() == type;
      if (!isDeclared && !method.isPublic()) {
        continue;
      }
      // First, we need to construct our runtime key.
      // We don't actually pay for runtime lookup,
      // because we can just generate a to-string method inline, with genrator-time data.
      initMethod
        .println("var sig"+i+" = ").indent()
        .println("'" +method.getName()+"'")// we don't close this var until later
      ;

      JType[] params = method.getParameterTypes();
      // Unfortunately, we do have to un/box primitives
      JPrimitiveType[] boxing = new JPrimitiveType[params.length];
      StringBuilder jsniSig = new StringBuilder();
      StringBuilder methodSig = new StringBuilder();
      for (int p = 0; p < params.length; p++) {
        JType param = params[p];
        JPrimitiveType primitive = param.isPrimitive();
        assert primitive != JPrimitiveType.VOID; // can't have void params
        if (primitive!=null) {
          // primitives don't have a Class.seedId, so we must use their runtime name "int", "float", etc.
          // as these class names are at least consistent across runtime and generate time.
          initMethod.println(" + '_"+param.getQualifiedSourceName()+"'");
          boxing[p] = primitive;
        }else {
          // big line of code, compiles down to integer constant
          initMethod.println(" + '_' + @"+param.getQualifiedSourceName()+"::class.@java.lang.Class::seedId");
        }
        // prepare the method's jsni signature we'll need in a minute
        jsniSig.append(param.getJNISignature());
        // also build variable names for params, to avoid resorting to arguments[n] calls
        methodSig.append((char)('A'+p));
        if (p < params.length-1) {
          methodSig.append(',');
        }
      }// end key builder loop
      initMethod.outdent().println(";");
      // grab a copy of the key var, since we reuse it and we don't want it to update!

      // now, we have the key, let's generate a method provider.
      // this method will create a new method for the given signature
      // and then replace itself with a method to directly return the singleton.
      initMethod.println("map[sig" +i+"] = function(){").indent();
      // create Method lazily, and reset the provider function to give back
      // the singleton instance; (no sense creating an object every time)
      if (methodSig.length()>0) {
        initMethod.println("var methodCall = function(inst, "+methodSig.toString()+"){");
      } else {
        initMethod.println("var methodCall = function(inst){");
      }
      initMethod.indent();

      // we need to handle unboxing here in jsni
      // all primitives are boxed into Object ... varargs in reflection invocation,
      // so, to avoid potential compiler issues, we unbox to match
      // your constructor signature exactly.
      for (int box = 0; box < boxing.length; box++) {
        char var = (char)('A'+box);
        JPrimitiveType unbox = boxing[box];
        if (unbox != null) {

          //need to unbox
          initMethod.print("if (typeof "+var+" == 'object'");//note we didn't close this if
          if (unbox == LONG) {
            //long needs special treatment, as its primitive IS typeof 'object'
            initMethod.println(" && "+var+"['l'] == undefined)")
              .indentln(var+" = @java.lang.Class::unboxLong(Ljava/lang/Long;)(" +var+");")
              .println("else if ("+var+" == undefined) "+var+" = {h:0,m:0,l:0};");
          } else if (unbox == CHAR){
            initMethod.println(")")
              .indentln(var+" = "+var+".@java.lang.Character::charValue()();")
              .println("else if ("+var+" == undefined) "+var+" = 0;");
          } else if (unbox == BOOLEAN){
            initMethod.println(")")
              .indentln(var+" = "+var+".@java.lang.Boolean::booleanValue()();")
              .println("else if ("+var+" == undefined) "+var+" = false;");
          } else {
            //all non-long numbers convert to double to avoid narrowing.
            //they will already be narrowed in object form.
            initMethod.println(")")
              .indentln(var+" = "+var+".@java.lang.Number::doubleValue()();")
              .println("else if ("+var+" == undefined) "+var+" = 0;");
          }
        }
      }

      // the moment the method is called!!
      // need to be sure to reference static or instance call properly!
      if (!"V".equals(method.getReturnType().getJNISignature()))
        initMethod.println("return ");

      if (method.isStatic()) {
        initMethod.print("@"+type.getQualifiedSourceName());
      } else {
        initMethod.print("inst.@"+type.getQualifiedSourceName());
      }
      // print the jsni method call that we are enclosing in a js function
      initMethod
        .println("::" + method.getName()+"("+jsniSig+")(" +methodSig+");")
        .outdent()
        .println("};")// end lazy wrapper method
        ;


      // collect the annotations and send them along to our method
      initMethod.println("var annos = {};");
      for (Annotation a : methodMap.get(method)) {
        GeneratedAnnotation anno = GwtAnnotationGenerator.generateAnnotationProvider(logger, sb, a, context);
        initMethod
          .print("annos[")
          .print("@" +anno.getAnnoName()+ "::class.@java.lang.Class::getName()()")
          .println("] = @"+anno.latest.getQualifiedName()+"::"+anno.latest.getName()+"()();")
        ;
      }

      //create a singleton Method instance
      initMethod.print("var mthd = newMethod")
        .println("(" +
      		"cls, " +
      		"'" + method.getName()+ "', " +
      		ReflectionTypeUtil.toJsniClassLits(method.getParameterTypes()) +", " +
      		ReflectionTypeUtil.toJsniClassLit(method.getReturnType())+"," +
      		ReflectionTypeUtil.toJsniClassLits(method.getThrows()) +", " +
      		ReflectionTypeUtil.getModifiers(method) + ", " +
      		"methodCall, annos" +
      		");");

      //replace provider method to skip this instantiation next time
      initMethod
        .println("map[sig" +i+"] = function() { return mthd; }");

      // mark the provider function with whether the method is declared or merely accessible
      if (isDeclared)
        initMethod.println("map[sig"+i+"].declared = true;");
      if (method.isPublic())
        initMethod.println("map[sig"+i+"].pub = true;");
      initMethod
      //return the method to whoever asked for it.
        .println("return mthd;")
        .outdent()
        .println("};");
      if (isDeclared)
        initMethod.println("map[sig"+i+"].declared = true;");
      if (method.isPublic())
        initMethod.println("map[sig"+i+"].pub = true;");
    }
    // End MethodMap

    if (logger.isLoggable(Type.DEBUG))
      logger.log(Type.DEBUG, "MethodMap: "+initMethod);

  }

}
