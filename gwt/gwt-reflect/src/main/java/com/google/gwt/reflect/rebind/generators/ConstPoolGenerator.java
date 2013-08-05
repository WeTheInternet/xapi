package com.google.gwt.reflect.rebind.generators;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.validation.Payload;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

public class ConstPoolGenerator {

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

  public static ConstPoolGenerator getGenerator() {
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

  
  private Map<Annotation,Integer> annos = Maps.newHashMap();
  private Map<Double,Integer> doubles = Maps.newHashMap();
  private Map<Enum<?>,Integer> enums = Maps.newHashMap();
  private Map<Integer,Integer> ints = Maps.newHashMap();
  private Map<Long,Integer> longs = Maps.newHashMap();
  private Map<String,Integer> strings = Maps.newHashMap();

  private Map<String,Integer> classes = Maps.newHashMap();

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

  public int rememberClass(JClassType type) {
    String name = type.getQualifiedSourceName();
    Integer pos = classes.get(name);
    if (pos == null) {
      pos = classes.size();
      classes.put(name, pos);
    }
    return pos;
  }

}
