package com.google.gwt.reflect.rebind.generators;

import static java.lang.reflect.Modifier.FINAL;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.validation.Payload;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MemberBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.ConstPool.ArrayConsts;
import com.google.gwt.reflect.client.ConstPool.ClassConsts;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator.GeneratedAnnotation;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

public class ConstPoolGenerator {

  @SuppressWarnings("unused")
  private static class ArrayCompare implements Comparator<Object> {

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int compare(Object o1, Object o2) {
      assert o1.getClass().getComponentType() == o2.getClass().getComponentType();
      int len1 = Array.getLength(o1);
      int len2 = Array.getLength(o2);
      if (len1 != len2)
        return len1 - len2;
      while (len1 --> 0) {
        Object obj1 = Array.get(o1, len1);
        Object obj2 = Array.get(o2, len1);
        if (!obj1.equals(obj2)) {
          if (obj1 instanceof Comparable) {
            return ((Comparable)obj1).compareTo(obj2);
          }
          int hash1 = obj1.hashCode();
          int hash2 = obj2.hashCode();
          if (hash1 != hash2)
            return hash1 - hash2;
          return System.identityHashCode(obj1) - System.identityHashCode(obj2);
        }
      }
      return 0;
    }
  }
  
  @SuppressWarnings("serial")
  private static class ComparableArray<T extends Number> extends ArrayList<T> implements Comparable<ComparableArray<T>> {
    @Override
    public int compareTo(ComparableArray<T> o) {
      int size = size();
      if (size != o.size())
        return size - o.size();
      for (; size --> 0;) {
        int diff = Double.compare(get(size).doubleValue(), o.get(size).doubleValue());
        if (diff != 0)
          return 0;
      }
      return 0;
    }
  }
  
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

  static ConstPoolGenerator getGenerator() {
    return generator.get();
  }

  public int generateReference(TreeLogger logger,
    GeneratorContext context, Object value) throws UnableToCompleteException {
    int pool = initBuffer();
    
    if (value instanceof String) {
  
    } else if (value instanceof Long) {
    } else if (value instanceof Float || value instanceof Double) {
        Double val = ((Float)value).doubleValue();
        Integer key = doubles.get(val);
        if (key == null) {
          key = doubles.size();
          doubles.put(val, key);
        }
        return key;
    } else if (value instanceof Number) {
      // Any non-floating point, non-long, is passed around as int
      return ((Number)value).intValue();
    } else if (value instanceof Class) {
    } else if (value.getClass().isArray()) {
      Class<?> type = value.getClass().getComponentType();
      if (type == boolean.class) {
        // because booleans aren't stored in singular format,
        // arrays of booleans are converted to int[],
        // and turned back into boolean[] when retrieved from ConstPool
        boolean[] bools = (boolean[])value;
        int len = bools.length;
        int[] ints = new int[len];
        for (;len-->0;) {
          ints[len] = bools[len] ? 1 : 0;
        }
        value = ints;
        type = int.class;
      }
      if (type == int.class) {
        // int[] is handled specially; all other array types boil down to an int[],
        // whose values points to the instance of each element in a returned array.
        Integer existing = intArrays.get(value);
        if (existing == null) {
          existing = intArrays.size();
          intArrays.put((int[])value, existing);
          out.getClassBuffer()
            .createField(int[].class, "ints"+existing)
            .makeFinal().makePublic().makeStatic()
          ;
        }
        return existing;
      }
      if (type.isPrimitive()) {
        value = boxArray(value, type);
      }
      int length = GwtReflect.arrayLength(value);
      int[] values = new int[length];
      for (int i = 0; i < length; i++ ) {
        Object item = GwtReflect.arrayGet(value, i);
        values[i] = generateReference(logger, context, item);
      }
      return generateReference(logger, context, values);
    } else if (value instanceof Character) {
  
    } else if (value instanceof Enum) {
  
    } else if (value instanceof Annotation) {
  
    } else {
      logger.log(Type.ERROR, "ConstPoolGenerator encountered a type it cannot handle");
      logger.log(Type.ERROR, "value type "+value.getClass()+" ");
      throw new UnableToCompleteException();
    }
    return 0;
  }

  private Object boxArray(Object value, Class<?> type) {
    int len = Array.getLength(value);
    if (type == double.class) {
      Double[] arr = new Double[len];
      for (; len --> 0;) {
        arr[len] = Array.getDouble(value, len);
      }
      return arr;
    } else if (type == float.class) {
      Float[] arr = new Float[len];
      for (; len --> 0;) {
        arr[len] = Array.getFloat(value, len);
      }
      return arr;
    } else if (type == long.class) {
      Long[] arr = new Long[len];
      for (; len --> 0;) {
        arr[len] = Array.getLong(value, len);
      }
      return arr;
    } else if (type == short.class) {
      Short[] arr = new Short[len];
      for (; len --> 0;) {
        arr[len] = Array.getShort(value, len);
      }
      return arr;
    } else if (type == byte.class) {
      Byte[] arr = new Byte[len];
      for (; len --> 0;) {
        arr[len] = Array.getByte(value, len);
      }
      return arr;
    } else if (type == char.class) {
      Character[] arr = new Character[len];
      for (; len --> 0;) {
        arr[len] = Array.getChar(value, len);
      }
      return arr;
    }
    return null;
  }

  private int iteration;
  private int start;

  
  private Map<Double,Integer> doubles = Maps.newHashMap();
  private Map<Enum<?>,Integer> enums = Maps.newHashMap();
  private Map<Integer,Integer> ints = Maps.newHashMap();
  private Map<Long,Integer> longs = Maps.newHashMap();
  private Map<String,Integer> strings = Maps.newHashMap();

  private Map<String,String> classes = Maps.newHashMap();
  private Map<String,String> classArrays = Maps.newHashMap();
  private Map<Annotation,String> annos = Maps.newHashMap();
  private Map<String,String> annoArrays = Maps.newHashMap();

  private static Map<String,String> wellKnownClasses = Maps.newHashMap();
  static {
    try {
      String prefix = ClassConsts.class.getCanonicalName();
      for (Field f : ClassConsts.class.getFields()) {
        Class<?> cls = (Class<?>) f.get(null);
        wellKnownClasses.put(cls.getCanonicalName(), prefix+"."+f.getName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Map<int[],Integer> intArrays = new TreeMap<int[], Integer>(new Comparator<int[]>() {
    @Override
    public int compare(int[] o1, int[] o2) {
      int l1 = o1.length, l2 = o2.length;
      if (l1 != l2)
        return l1 - l2;
      for(;l1 --> 0;) {
        l2 = o1[l1] - o2[l1];
        if (l2 != 0)
          return l2;
      }
      return 0;
    }
  });
  
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

  public void printNewArray(TreeLogger logger, PrintBuffer out, JClassLiteral classLit, boolean jsni) {
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

  private String getConstName(int pos) {
    return "Const"+iteration+"_"+pos;
  }
  
  public String getConstClass(int[] array) {
    return "constants.Const"+iteration+"_"+array[0];
  }

  public String rememberClass(JClassType type) {
    return rememberClass(type.getQualifiedSourceName());
  }
  public String rememberClass(String type) {
    String name = classes.get(type);
    if (name == null) {
      name = wellKnownClasses.get(type);
    }
    if (name == null) {
      int pool = initBuffer();
      String cls = getConstName(pool);
      name = "CLASS_"+classes.size();
      String shortType = out.getImports().addImport(type);
      out.getClassBuffer()
        .createField("Class<"+shortType+">", name)
        .setInitializer(shortType+".class")
        .setModifier(FINAL)
        .makePackageProtected();
      classes.put(type, "constants."+cls+"."+name);
    }
    return name;
  }

  public void arrayOfClasses(TreeLogger logger, MemberBuffer<?> out, String ... names) {
    
    if (names.length == 0) {
      out.print(
          out.addImportStatic(ArrayConsts.class, "EMPTY_CLASSES")
      );
      return;
    }
    
    String shortName;
    {
      StringBuilder b = new StringBuilder();
      for (int i = names.length; i --> 0; ) {
        b.append(names[i]).append(',');
        names[i] = rememberClass(names[i]);
      }
      shortName = b.toString();
    }
    
    String existing = classArrays.get(shortName);
    if (existing == null) {
      int pool = initBuffer();
      String constName = getConstName(pool);
      String name = "CLS_ARR_"+classArrays.size();
      ClassBuffer b = this.out.getClassBuffer();
      PrintBuffer init = b
          .createField("Class<?>[]",name)
          .setModifier(FINAL)
          .makePackageProtected()
          .getInitializer();
      init
        .println("new Class<?>[]{")
        .indent();
      String prefix = "";
      for (String cls : names) {
        String clsField = this.out.getImports().addStatic(cls);
        init.print(prefix).println(clsField);
        prefix = ", ";
      }
      init.outdent();
      init.print("}");
      
      existing = "constants."+constName+"."+name;
      classArrays.put(shortName, existing);
    }
    out.print(
      out.addImportStatic(existing)
    );
  }

  public void arrayOfAnnotations(TreeLogger logger, GeneratorContext ctx, MemberBuffer<?> buffer, Annotation ... annos) throws UnableToCompleteException {
    if (annos.length == 0) {
      buffer.print(
          buffer.addImportStatic(ArrayConsts.class, "EMPTY_ANNOTATIONS")
          );
      return;
    }
    String flatName;
    { 
      TreeSet<String> ordered = new TreeSet<String>();
      for (Annotation anno : annos) {
        ordered.add(anno.toString());
      }
      flatName = ordered.toString();
    }
    
    String existing = annoArrays.get(flatName);
    if (existing != null) {
      buffer.print(
          buffer.addImportStatic(existing)
          );
      return;
    }
    
    String[] items;
    {
      int i = annos.length;
      items = new String[i];
      for (; i-->0;) {
        items[i] = rememberAnnotation(logger, ctx, annos[i]);
      }
    }
    
    int pool = initBuffer();
    String constName = getConstName(pool);
    String arrayName = "ANNO_ARR_"+annoArrays.size();
    String finalName = "constants."+constName+"."+arrayName;
    annoArrays.put(flatName, finalName);
    
    
    PrintBuffer init = out
      .getClassBuffer()
      .createField(Annotation[].class, arrayName)
      .makePackageProtected()
      .makeFinal()
      .getInitializer()
    ;
    buffer.addImport(Annotation.class);
    init.print("new Annotation[]{");
    if (items.length == 1) {
      init
        .print(" ")
        .print(buffer.addImportStatic(items[0]))
        .print(" }")
      ;
    } else {
      init
        .indent()
        .println()
        .println(buffer.addImportStatic(items[0]));
      for (int i = 1; i < items.length; i++) {
        init.print(",").println(buffer.addImportStatic(items[i]));
      }
      init
        .outdent()
        .print("}");
    }
    
    buffer.print(
        buffer.addImportStatic(finalName)
    );
  }

  private String rememberAnnotation(TreeLogger logger, GeneratorContext ctx, Annotation anno) throws UnableToCompleteException {
    String existing = annos.get(anno);
    if (existing != null) {
      return existing;
    }
    String constName = getConstName(initBuffer());
    String annoName = "ANNO_"+annos.size();

    existing = "constants."+constName+"."+annoName;
    annos.put(anno, existing);
    
    Method[] methods = ReflectionUtilJava.getMethods(anno);
    String[] values = new String[methods.length];
    for (int i = 0, m = methods.length; i < m; i++) {
      try {
      Object value = methods[i].invoke(anno);
      if (value instanceof Annotation) {
        values[i] = out.getImports().addStatic(rememberAnnotation(logger, ctx, (Annotation) value));
      } else if (value instanceof Class){
        values[i] = 
            out.getImports().addImport(((Class<?>)value).getCanonicalName())
            +".class";
      } else if (value instanceof Enum) {
        Enum<?> e = (Enum<?>) value;
        values[i] = 
            out.getImports().addStatic(e.getDeclaringClass().getCanonicalName()+"."+e.name());
      } else if (value instanceof String) {
        values[i] = "\""+GwtReflect.escape((String)value)+"\"";
      } else {
        values[i] = String.valueOf(value);
      }
      } catch (Exception e) {
        logger.log(Type.ERROR, "Fatal error reading values reflectively from annotation "+anno);
        throw new UnableToCompleteException();
      }
    }
    
    PrintBuffer init = out
        .getClassBuffer()
        .createField(anno.annotationType(), annoName)
        .makeFinal()
        .makePackageProtected()
        .getInitializer();
    
    GeneratedAnnotation gen = GwtAnnotationGenerator.generateAnnotation(logger, ctx, anno);
//    IsNamedType known = gen.knownInstances.get(anno);
//    if (known != null) {
//    }
    init
      .print("new ")
      .print(out.getImports().addImport(gen.proxyName))
      .print("(");
    
    if (values.length > 0) {
      init.print(values[0]);
      for (int i = 1, m = values.length; i < m; i++) {
        init.print(", ").print(values[i]);
      }
    }
    
    init.print(")");
    
    
    return existing;
  }

}
