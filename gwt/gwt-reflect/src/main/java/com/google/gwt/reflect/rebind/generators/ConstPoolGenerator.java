package com.google.gwt.reflect.rebind.generators;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.validation.Payload;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.GwtReflect;

public class ConstPoolGenerator {

  private static ThreadLocal<ConstPoolGenerator> generator = new ThreadLocal<ConstPoolGenerator>() {
    private int iteration;
    @Override
    protected ConstPoolGenerator initialValue() {
      return new ConstPoolGenerator(iteration++);
    };
  };
  
  public static void cleanup() {
    generator.remove();
  }

  public static ConstPoolGenerator getGenerator() {
    return generator.get();
  }

  public static synchronized String generateReference(TreeLogger logger,
    GeneratorContext context, Object value) throws UnableToCompleteException {
    String ref = generator.get().references.get(value);
    if (ref != null)
      return ref;
    String name;
    if (value == null)
      return "NULL";
    if (value instanceof String) {
  
    } else if (value instanceof Number) {
  
    } else if (value instanceof Class) {
    } else if (value.getClass().isArray()) {
      Class<?> type = value.getClass().getComponentType();
      int length = GwtReflect.arrayLength(value);
      for (int i = 0; i < length; i++ ) {
        Object item = GwtReflect.arrayGet(value, i);
        String constRef = generateReference(logger, context, item);
      }
    } else if (value instanceof Boolean) {
    } else if (value instanceof Character) {
  
    } else if (value instanceof Enum) {
  
    } else if (value instanceof Annotation) {
  
    } else {
      logger.log(Type.ERROR, "ConstPoolGenerator encountered a type it cannot handle");
      logger.log(Type.ERROR, "value type "+value.getClass()+" ");
      throw new UnableToCompleteException();
    }
    return "";
  }


  private int iteration;
  private int start;

  private LinkedHashMap<Object,String> references = new LinkedHashMap<Object,String>();
  private HashMap<Object,int[]> arraySeeds = new HashMap<Object,int[]>();

  private transient ArrayList<SourceBuilder<?>> sourceClasses = new ArrayList<SourceBuilder<?>>();
  private transient SourceBuilder<?> out;

  private ConstPoolGenerator(int iteration) {
    this.iteration = iteration;
    this.start = sourceClasses.size();
  }

  public void commit(TreeLogger logger, GeneratorContext ctx) {
    int pos = sourceClasses.size()-1;
    while (out != null) {
      // commit const pool(s)
      PrintWriter pw = ctx.tryCreate(logger, out.getPackage(), out.getClassBuffer().getSimpleName());
      pw.println(out.toString());
      ctx.commit(logger, pw);
      // erase our writer
      sourceClasses.set(pos--, null);
      if (pos < 0)
        out = null;
      else
        out = sourceClasses.get(pos);
    }
  }

  public void addDependencies(TreeLogger logger, ClassBuffer out) {
    for (int i = start, m = sourceClasses.size(); i < m; i++) {
      out.addInterface("contants.Const"+iteration+"_"+i);
    }
  }

  public void printNewArray(TreeLogger logger, PrintBuffer out, JClassLiteral classLit, JExpression size, boolean jsni) {
    int[] ref = getSeedArray(logger, classLit);
    if (jsni) {
      out.print("@constants.Const"+iteration+"_"+ref[0]+"::array"+ref[1]);
    } else {
      out.print("constants.Const"+iteration+"_"+ref[0]+".array"+ref[1]);
    }
  }

  public synchronized int[] getSeedArray(TreeLogger logger, JClassLiteral classLit) {
    int[] result = arraySeeds.get(classLit);
    if (result == null) {
      int constId = initBuffer();
      result = new int[] {constId, arraySeeds.size()};
      ClassBuffer into = out.getClassBuffer();
      String typeName = classLit.getRefType().getName();
      if (classLit.getRefType().getDefaultValue() != JNullLiteral.INSTANCE) {
        // primitive fields have defaults, and require special handling
        into.print(typeName+"[] array"+result[1]+" = ");
        into.print("("+typeName+"[])setPrimitiveArray(");
        into.println(typeName+".class, new "+typeName+"[0]);");
      } else {
        into.print(typeName+"[] array"+result[1]+" = setArray(");
        into.println(typeName+".class, new "+typeName+"[0]);");
      }
      arraySeeds.put(classLit, result);
    }
    return result;
  }

  private synchronized int initBuffer() {
    if (out == null) {
      out = new SourceBuilder<Payload>("public interface Const"+iteration+"_"+sourceClasses.size());
      out.setPackage("constants");
      out.getImports().addStatic(ConstPool.class.getName()+".setArray");
      out.getImports().addStatic(ConstPool.class.getName()+".setPrimitiveArray");
      sourceClasses.add(out);
    }
    return sourceClasses.size() - 1;
  }

  public String getConstClass(int[] array) {
    return "constants.Const"+iteration+"_"+array[0];
  }

}
