package com.google.gwt.reflect.rebind.generators;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.regex.Pattern;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.reflect.client.ClassMap;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator.GeneratedAnnotation;

public class GwtFieldGenerator extends ReflectionGeneratorUtil{

  private static final String FIELD_CTOR = "new(" +
  		"Ljava/lang/Class;" +
  		"Ljava/lang/String;" +
  		"Ljava/lang/Class;" +
  		"I" +
  		"Lcom/google/gwt/core/client/JavaScriptObject;)";

  public static boolean generateFields(
    TreeLogger logger, SourceBuilder<?> sb, GeneratorContext context,
    JClassType type, Map<JField, Annotation[]> fieldMap
  ) throws UnableToCompleteException {

    if (fieldMap.size()==0) {
      logger.log(Type.WARN, "No fields found for "+type.getQualifiedSourceName());
      return false;
    }

    ClassBuffer out = sb.getClassBuffer();

    //In order to provide fields, we need to rip the jsni field refs out;
    //we then provide a simple accessor object which applies the given
    //get or set operation on the jsni field in question.


    //We want it to be lazily loaded, so we provide static methods here.
    //This will keep the actual getField(s) methods clean,
    //and likely encourage pruning by the gwt compiler.

    MethodBuffer initFields = out.
      createMethod("private static native void enhanceFields" +
          "(final Class<?> cls)")
      .setUseJsni(true)
      .addAnnotation(UnsafeNativeLong.class)
      .println("var map = cls.@java.lang.Class::fields;")
      .println("if (map) return;")// we may later implement super-class enhancement, to reduce code size
      .println("map = cls.@java.lang.Class::fields = {};")
      // pull out the constructor for Method, so we reduce the runtime lookup of the function
      .println("var newField = @java.lang.reflect.Field::"+FIELD_CTOR+";")

    ;

    JField[] fields = fieldMap.keySet().toArray(new JField[fieldMap.size()]);
    //now, let's write jsni accessors for the fields
    for (int i = 0; i < fields.length; i++) {
      JField field = fields[i];
      boolean isDeclared = field.getEnclosingType() == type;
      if (!isDeclared && !field.isPublic())
        continue; // obey java reflection api
      String accessor = getSafeAccessor(field.getName());
      initFields.println("map"+accessor+" = function(){").indent();

      boolean isPrimitive = field.getType().isPrimitive() != null;
      initFields
        .println("var accessor = {")
        .indent()
        .println("getter : function(inst) {")
        .indent();

      String jsni;
      String fieldMount = field.getEnclosingType().getQualifiedSourceName();
      if (field.isStatic()) {
        jsni = "@"+fieldMount+"::"+field.getName();
      } else {
        jsni = "inst.@"+fieldMount+"::"+field.getName();
      }
      //we force boxing onto the getter method,
      //since you are getting back an object,
      //and we are referencing the primitive field.

      if (isPrimitive) {
        //we have to unbox primitives, so we have to process each get a little
        initFields.println("var val = "+jsni+";");
        switch(field.getType().isPrimitive()) {
        case LONG:
          //long needs special treatment
          initFields
            .println("if (typeof val == 'object' && val['l'] != undefined)")
            .indentln("return @java.lang.Class::boxLong(J)(val);");
          break;
        case BOOLEAN:
          initFields
            .println("if (typeof val == 'boolean')")
            .indentln("return val ? @java.lang.Boolean::TRUE : @java.lang.Boolean::FALSE;");
          break;
        case CHAR:
          initFields
            .println("if (typeof val == 'number')")
            .indentln("return @java.lang.Character::new(C)(val);");
          break;
        case BYTE:
          initFields
            .println("if (typeof val == 'number')")
            .indentln("return @java.lang.Byte::new(B)(val);");
          break;
        case DOUBLE:
          initFields
            .println("if (typeof val == 'number')")
            .indentln("return @java.lang.Double::new(D)(val);");
          break;
        case FLOAT:
          initFields
            .println("if (typeof val == 'number')")
            .indentln("return @java.lang.Float::new(F)(val);");
          break;
        case INT:
          initFields
            .println("if (typeof val == 'number')")
            .indentln("return @java.lang.Integer::new(I)(val);");
          break;
        case SHORT:
          initFields
            .println("if (typeof val == 'number')")
            .indentln("return @java.lang.Short::new(S)(val);");
          break;
        default:
        }//end switch
      } //end primitive block
      //finish the getter
      initFields
        .println("return "+jsni+";")
        .outdent()
        .println("},")//end getter
      // start the setter
        .println("setter : function(inst, val) {")
        .indent();
        //our setter is receiving an object, so any primitives will be boxed.
        //however, since you might send in primitives from jsni,
        //we do perform a runtime box-checking.
      if (field.isFinal()){
        // throw the same as java reflection
        initFields.println("@"+ClassMap.class.getName()+"::throwIllegalAccess()()");
      }
      else if (isPrimitive) {
        //we have to unbox primitives, so we have to process each get a little
        initFields.print("if (typeof val == 'object'");//this also covers null check
        //note we didn't close this if statement; Long needs to perform an &&
        switch(field.getType().isPrimitive()) {
        case LONG:
          //long needs special treatment.
          //a native long is actually formed as {l:0,m:0,h:0}
          initFields
            .println(" && val['l'] == undefined)")
            .indentln(jsni+" = @java.lang.Class::unboxLong(Ljava/lang/Long;)(val);")
            .println("else if (val == undefined) " +jsni+ " =  {l:0,m:0,h:0};");
          break;
        case BOOLEAN:
          initFields
            .println(")")
            .indentln(jsni + " = val.@java.lang.Boolean::booleanValue()();")
            .println("else if (val == undefined) " +jsni +" = false;");
          break;
        case CHAR:
          initFields
            .println(")")
            .indentln(jsni+" = val.@java.lang.Character::charValue()();")
            .println("else if (val == undefined) " +jsni+" = 0;");
          break;
        default : //prefer default to suppress warning about ignoring VOID
          initFields
            .println(")")
            .indentln(jsni + " = val.@java.lang.Number::doubleValue()();")
            .println("else if (val == undefined) " +jsni+" = 0;");
        }//end switch
        //this value is already primitive, which can happen if you use jsni on
        //your field reference and skip the automatic boxing on the method. :)
        initFields.println("else "+jsni+" = val;");
      } //end primitive block
      else {
        //objects don't need pre-processing
        //at runtime a Field is as efficient as any setter method,
        //except it cannot be inlined by the compiler.
        //JIT browsers like chrome will likely still inline it if reused though.
        initFields.println(jsni + " = val;");
      }

      initFields
        .outdent().println("}")// end setter
        .outdent().println("};")//end accessor
         // start annotation support
        .println("accessor.annos = function(){").indent()
        .println("var key, result = {};");

      for (Annotation a : fieldMap.get(field)) {
        GeneratedAnnotation anno = GwtAnnotationGenerator.generateAnnotationProvider(logger, sb, a, context);
        initFields
          .println("key = @" +anno.getAnnoName()+ "::class.@java.lang.Class::getName()();")
          .print("result[key] = @"+anno.latest.getQualifiedName()+"::"+anno.latest.getName()+"()();")
        ;
      }

      initFields
        .println("accessor.annos = function(){return result;};")
        .println("return result;")
        .outdent().println("};");// end annos



      //create a singleton field
      initFields
        .println("var field = newField" +
        "(cls, '" +
          field.getName()+ "', @" +
          field.getType().getQualifiedSourceName()+"::class, " +
          ReflectionTypeUtil.getModifiers(field)+", "+
          "accessor);");

      //replace provider field to skip this instantiation next time
      initFields.println("map"+accessor+" = function() { return field; };");
      // set marker fields on the direct provider
      if (isDeclared)
        initFields.println("map"+accessor+".declared = true;");
      if (field.isPublic())
        initFields.println("map"+accessor+".pub = true;");

      //return the field to whomever asked for it.
      initFields.println("return field;");
      initFields.outdent();
      initFields.println("};");//end accessor

      // set marker fields on the lazy provider
      if (isDeclared)
        initFields.println("map"+accessor+".declared = true;");
      if (field.isPublic())
        initFields.println("map"+accessor+".pub = true;");
    }
    return true;
  }

  private static final Pattern RestrictedNames = Pattern.compile(
      "(var|typeof|break|export|return|case|for|switch|comment|function" +
      "|this|continue|if|default|import|delete|in|void|do|label|while" +
      "else|new|with|true|false|catch|enum|throw|class|extends|try|const|finally)"
  );

  private static String getSafeAccessor(String name) {
    if (RestrictedNames.matcher(name).matches()){
      return "['"+name+"']";
    }
    return "."+name;
  }

}
