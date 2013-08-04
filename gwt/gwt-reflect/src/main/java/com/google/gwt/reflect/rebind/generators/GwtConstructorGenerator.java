package com.google.gwt.reflect.rebind.generators;

import static com.google.gwt.core.ext.typeinfo.JPrimitiveType.BOOLEAN;
import static com.google.gwt.core.ext.typeinfo.JPrimitiveType.CHAR;
import static com.google.gwt.core.ext.typeinfo.JPrimitiveType.LONG;

import java.lang.annotation.Annotation;
import java.util.Map;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator.GeneratedAnnotation;

public class GwtConstructorGenerator extends ReflectionGeneratorUtil{

  private static final String CONSTRUCTOR_CTOR = "new(" +
    "Ljava/lang/Class;" +
    "Lcom/google/gwt/core/client/JavaScriptObject;" +
    "Lcom/google/gwt/core/client/JavaScriptObject;" +
  ")";

  /**
   * @throws UnableToCompleteException
   */
  public static void generateConstructors(
    TreeLogger logger, SourceBuilder<?> sb, GeneratorContext context,
    JClassType toEnhance, Map<JConstructor, Annotation[]> constructors
  ) throws UnableToCompleteException {

    ClassBuffer out = sb.getClassBuffer();
    MethodBuffer initCtors = out
        .createMethod("public static native void enhanceConstructors(final Class<?> cls)")
        .setUseJsni(true)
        .addAnnotation(UnsafeNativeLong.class);
    if (constructors.size()==0 || toEnhance instanceof JEnumType) {
      return;
    }

    //In order to provide constructors, we need to rip the jsni methods out;
    //since we don't know the runtime names of types and fields at generate time
    //but we do know all of the constructor parameter types,
    //we generate methods to return javascript functions,
    //as well as runtime parameter signature mapping,
    //using the seedId that will be on the class literals at runtime.
    initCtors
        .println("var map = cls.@java.lang.Class::constructors;")
        .println("if (map) return;")// we may later implement super-class enhancement, to reduce code size
        .println("map = cls.@java.lang.Class::constructors = {};")
        // pull out the constructor for Method, so we reduce the runtime lookup of the function
        .println("var newConstructor = @java.lang.reflect.Constructor::"+CONSTRUCTOR_CTOR+";")
    ;

    //now, let's write jsni transforms for the constructors!
    JConstructor[] ctors = constructors.keySet().toArray(new JConstructor[constructors.size()]);
    for (int i = 0; i < ctors.length; i++) {
      JConstructor ctor = ctors[i];
      boolean isDeclared = ctor.getEnclosingType() == toEnhance;
      if (!isDeclared)
        continue; 
      //First, we need to construct our runtime key.
      initCtors.println("var sig"+i+" = '__init'").indent();

      JType[] params = ctor.getParameterTypes();
      JPrimitiveType[] boxing = new JPrimitiveType[params.length];
      StringBuilder jsniSig = new StringBuilder();
      StringBuilder methodSig = new StringBuilder();
      for (int p = 0; p < params.length; p++) {
        JType param = params[p];
        JPrimitiveType primitive = param.isPrimitive();
        if (primitive!=null) {
          switch(primitive) {
          case BOOLEAN:
          case BYTE:
          case CHAR:
          case DOUBLE:
          case FLOAT:
          case INT:
          case LONG:
          case SHORT:
            boxing[p] = primitive;
          default:
          }
        }
        initCtors.print(" + '_' + @"+ConstPool.class.getName()+"::constId(Ljava/lang/Class;)");
        initCtors.println("(@" + param.getQualifiedSourceName()+"::class)");

        //prepare the jsni signature we'll need in a minute
        jsniSig.append(param.getJNISignature());
        //also build variable names to avoid resorting to array param references
        methodSig.append((char)('A'+p));
        if (p < params.length-1) {
          methodSig.append(',');

        }
      }//end key builder loop
      initCtors.outdent().println(";");

      //now, we have the key, let's generate a constructor provider.
      //this method will create a new constructor for the given signature
      //and then replace itself with a method to directly return the singleton.
      initCtors.println("map[sig" +i+"] = function(){").indent();
      //create constructor lazily, and reset the provider function to give back
      //the singleton instance; (no sense creating an object every time)
      initCtors.println("var newInstance = function("+methodSig.toString()+"){");

      //we need to handle unboxing here in jsni
      //all primitives are boxes into the Object ... varargs in reflection
      //invocation, so, to avoid potential compiler issues, we unbox to match
      //your constructor signature exactly.
      for (int box = 0; box < boxing.length; box++) {
        char var = (char)('A'+box);
        JPrimitiveType unbox = boxing[box];
        if (unbox != null) {

          //need to unbox
          initCtors.print("if (typeof "+var+" == 'object'");//note we didn't close this if
          if (unbox == LONG) {
            //long needs special treatment
            initCtors.println(" && "+var+"['l'] == undefined)")
              .indentln(var+" = @java.lang.Class::unboxLong(Ljava/lang/Long;)(" +var+");")
              .println("else if ("+var+" == undefined) "+var+" = {h:0,m:0,l:0};");
          } else if (unbox == CHAR){
            initCtors.println(")")
              .indentln(var+" = "+var+".@java.lang.Character::charValue()();")
              .println("else if ("+var+" == undefined) "+var+" = 0;");
          } else if (unbox == BOOLEAN){
            initCtors.println(")")
              .indentln(var+" = "+var+".@java.lang.Boolean::booleanValue()();")
              .println("else if ("+var+" == undefined) "+var+" = false;");
          } else {
            //all non-long numbers convert to double to avoid narrowing.
            //they will already be narrowed in object form.
            initCtors.println(")")
              .indentln(var+" = "+var+".@java.lang.Number::doubleValue()();")
              .println("else if ("+var+" == undefined) "+var+" = 0;");
          }
        }
      }

      // the moment of instantiation!
      initCtors.print("return @"+toEnhance.getQualifiedSourceName())
        .println("::new("+jsniSig+")(" +methodSig+");")
        .print("};");// ends our js wrapper function

      // extract the annotations
      initCtors.println("var annos = {};");
      for (Annotation a : constructors.get(ctor)) {
        GeneratedAnnotation anno = GwtAnnotationGenerator.generateAnnotationProvider(logger, sb, a, context);
        initCtors
          .print("annos[")
          .print("@" +anno.getAnnoName()+ "::class.@java.lang.Class::getName()()")
          .println("] = @"+anno.latest.getQualifiedName()+"::"+anno.latest.getName()+"()();")
        ;
      }

      //create a singleton constructor
      initCtors.println("var ctor = newConstructor(cls, newInstance, annos);");

      //replace provider method to skip this instantiation next time
      initCtors.println("map[sig" +i+"] = function() { return ctor; }");
      if (isDeclared)
        initCtors.println("map[sig" +i+"].declared = true;");
      if (ctor.isPublic())
        initCtors.println("map[sig" +i+"].pub = true;");

      //return the contructor to whoever asked for it.
      initCtors.print("return ctor;")
        .outdent()
        .println("};");

      if (isDeclared)
        initCtors.println("map[sig" +i+"].declared = true;");
      if (ctor.isPublic())
        initCtors.println("map[sig" +i+"].pub = true;");
    }

  }

}
