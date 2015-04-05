package com.google.gwt.reflect.rebind.generators;

import static java.lang.reflect.Modifier.FINAL;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.ConstPool.ArrayConsts;
import com.google.gwt.reflect.client.ConstPool.ClassConsts;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.model.GeneratedAnnotation;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.xapi.dev.source.ClassBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.MemberBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.PrintBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.SourceBuilder;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.validation.Payload;

/**
 * This generator is used to contruct interfaces which contain references to constant values, and to also
 * store those contants in a
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ConstPoolGenerator {

  @SuppressWarnings("unused")
  private static class ArrayCompare implements Comparator<Object> {

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int compare(final Object o1, final Object o2) {
      assert o1.getClass().getComponentType() == o2.getClass().getComponentType();
      int len1 = Array.getLength(o1);
      final int len2 = Array.getLength(o2);
      if (len1 != len2) {
        return len1 - len2;
      }
      while (len1 --> 0) {
        final Object obj1 = Array.get(o1, len1);
        final Object obj2 = Array.get(o2, len1);
        if (!obj1.equals(obj2)) {
          if (obj1 instanceof Comparable) {
            return ((Comparable)obj1).compareTo(obj2);
          }
          final int hash1 = obj1.hashCode();
          final int hash2 = obj2.hashCode();
          if (hash1 != hash2) {
            return hash1 - hash2;
          }
          return System.identityHashCode(obj1) - System.identityHashCode(obj2);
        }
      }
      return 0;
    }
  }

  @SuppressWarnings("serial")
  private static class ComparableArray<T extends Number> extends ArrayList<T> implements Comparable<ComparableArray<T>> {
    @Override
    public int compareTo(final ComparableArray<T> o) {
      int size = size();
      if (size != o.size()) {
        return size - o.size();
      }
      for (; size --> 0;) {
        final int diff = Double.compare(get(size).doubleValue(), o.get(size).doubleValue());
        if (diff != 0) {
          return diff;
        }
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

  public int generateReference(final TreeLogger logger,
    final GeneratorContext context, Object value) throws UnableToCompleteException {
    initBuffer();

    if (value instanceof String) {

    } else if (value instanceof Long) {
    } else if (value instanceof Float || value instanceof Double) {
        final Double val = ((Float)value).doubleValue();
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
        final boolean[] bools = (boolean[])value;
        int len = bools.length;
        final int[] ints = new int[len];
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
      final int length = GwtReflect.arrayLength(value);
      final int[] values = new int[length];
      for (int i = 0; i < length; i++ ) {
        final Object item = GwtReflect.arrayGet(value, i);
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

  private Object boxArray(final Object value, final Class<?> type) {
    int len = Array.getLength(value);
    if (type == double.class) {
      final Double[] arr = new Double[len];
      for (; len --> 0;) {
        arr[len] = Array.getDouble(value, len);
      }
      return arr;
    } else if (type == float.class) {
      final Float[] arr = new Float[len];
      for (; len --> 0;) {
        arr[len] = Array.getFloat(value, len);
      }
      return arr;
    } else if (type == long.class) {
      final Long[] arr = new Long[len];
      for (; len --> 0;) {
        arr[len] = Array.getLong(value, len);
      }
      return arr;
    } else if (type == short.class) {
      final Short[] arr = new Short[len];
      for (; len --> 0;) {
        arr[len] = Array.getShort(value, len);
      }
      return arr;
    } else if (type == byte.class) {
      final Byte[] arr = new Byte[len];
      for (; len --> 0;) {
        arr[len] = Array.getByte(value, len);
      }
      return arr;
    } else if (type == char.class) {
      final Character[] arr = new Character[len];
      for (; len --> 0;) {
        arr[len] = Array.getChar(value, len);
      }
      return arr;
    }
    return null;
  }

  private final int iteration;
  private final int start;


  private final Map<Double,Integer> doubles = Maps.newHashMap();
  private final Map<String,String> strings = Maps.newHashMap();
  private final Map<String,String> classes = Maps.newHashMap();
  private final Map<String,String> classArrays = Maps.newHashMap();
  private final Map<Annotation,String> annos = Maps.newHashMap();
  private final Map<String,String> annoArrays = Maps.newHashMap();
  private final Set<String> dependencies = new HashSet<String>();

  private static Map<String,String> wellKnownClasses = Maps.newHashMap();
  static {
    try {
      final String prefix = ClassConsts.class.getCanonicalName();
      for (final Field f : ClassConsts.class.getFields()) {
        final Class<?> cls = (Class<?>) f.get(null);
        wellKnownClasses.put(cls.getCanonicalName(), prefix+"."+f.getName());
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private final Map<int[],Integer> intArrays = new TreeMap<int[], Integer>(new Comparator<int[]>() {
    @Override
    public int compare(final int[] o1, final int[] o2) {
      int l1 = o1.length, l2 = o2.length;
      if (l1 != l2) {
        return l1 - l2;
      }
      for(;l1 --> 0;) {
        l2 = o1[l1] - o2[l1];
        if (l2 != 0) {
          return l2;
        }
      }
      return 0;
    }
  });

  private final HashMap<Object,int[]> arraySeeds = new HashMap<Object,int[]>();

  private transient ArrayList<SourceBuilder<?>> sourceClasses = new ArrayList<SourceBuilder<?>>();
  private transient SourceBuilder<?> out;

  private ConstPoolGenerator(final int iteration) {
    this.iteration = iteration;
    this.start = sourceClasses.size();
  }

  public void commit(final TreeLogger logger, final GeneratorContext ctx) {
    int pos = sourceClasses.size()-1;
    while (out != null) {
      // commit const pool(s)
      final PrintWriter pw = ctx.tryCreate(logger, out.getPackage(), out.getClassBuffer().getSimpleName());
      pw.println(out.toString());
      ctx.commit(logger, pw);
      // erase our writer
      sourceClasses.set(pos--, null);
      if (pos < 0) {
        out = null;
      } else {
        out = sourceClasses.get(pos);
      }
    }
  }

  public void addDependencies(final TreeLogger logger, final ClassBuffer out) {
    for (int i = start, m = sourceClasses.size(); i < m; i++) {
      final String dependency = "constants.Const"+iteration+"_"+i;
      out.addInterface(dependency);
      dependencies.remove(dependency);
    }
    for (final String dependency : dependencies) {
      out.addInterface(dependency);
    }
    dependencies.clear();
  }

  public void printNewArray(final TreeLogger logger, final PrintBuffer out, final JClassLiteral classLit, final boolean jsni) {
    final int[] ref = getSeedArray(logger, classLit);
    if (jsni) {
      out.print("@constants.Const"+iteration+"_"+ref[0]+"::array"+ref[1]);
    } else {
      out.print("constants.Const"+iteration+"_"+ref[0]+".array"+ref[1]);
    }
  }

  public synchronized int[] getSeedArray(final TreeLogger logger, final JClassLiteral classLit) {
    int[] result = arraySeeds.get(classLit);
    if (result == null) {
      final int constId = initBuffer();
      result = new int[] {constId, arraySeeds.size()};
      final ClassBuffer into = out.getClassBuffer();
      final String typeName = classLit.getRefType().getName();
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

  private String getConstName(final int pos) {
    return "Const"+iteration+"_"+pos;
  }

  public String getConstClass(final int[] array) {
    return "constants.Const"+iteration+"_"+array[0];
  }

  public String rememberClass(final JClassType type) {
    return rememberClass(type.getQualifiedSourceName());
  }

  public String rememberClass(final String type) {
    String name = classes.get(type);
    if (name == null) {
      name = wellKnownClasses.get(type);
    }
    if (name == null) {
      final int pool = initBuffer();
      final String cls = getConstName(pool);
      name = "CLASS_"+classes.size();
      final String shortType = out.getImports().addImport(type);
      out.getClassBuffer()
        .createField("Class<"+shortType+">", name)
        .setInitializer(shortType+".class")
        .setModifier(FINAL)
        .makePackageProtected();
      classes.put(type, "constants."+cls+"."+name);
    }
    return name;
  }


  public String rememberString(final String value) {
    String name = strings.get(value);
    final int pool = initBuffer();
    final String cls = getConstName(pool);
    if (name == null) {
      name = "STRING_"+strings.size();
      out.getClassBuffer()
        .createField("String", name)
        .setInitializer("\""+Generator.escape(value)+"\"")
        .setModifier(FINAL)
        .makePublic();
      strings.put(value, "constants."+cls+"."+name);
    } else {
      String constName = "constants."+cls+".";
      if (name.startsWith(constName)) {
        return name.substring(constName.length());
      } else {
        constName = name.substring(0, name.lastIndexOf('.'));
        dependencies.add(constName);
      }
    }
    return name;
  }

  public void arrayOfClasses(final TreeLogger logger, final MemberBuffer<?> out, final String ... names) {

    if (names.length == 0) {
      out.print(
          out.addImportStatic(ArrayConsts.class, "EMPTY_CLASSES")
      );
      return;
    }

    String shortName;
    {
      final StringBuilder b = new StringBuilder();
      for (int i = names.length; i --> 0; ) {
        b.append(names[i]).append(',');
        names[i] = rememberClass(names[i]);
      }
      shortName = b.toString();
    }

    String existing = classArrays.get(shortName);
    if (existing == null) {
      final int pool = initBuffer();
      final String constName = getConstName(pool);
      final String name = "CLS_ARR_"+classArrays.size();
      final ClassBuffer b = this.out.getClassBuffer();
      final PrintBuffer init = b
          .createField("Class<?>[]",name)
          .setModifier(FINAL)
          .makePackageProtected()
          .getInitializer();
      init
        .println("new Class<?>[]{")
        .indent();
      String prefix = "";
      for (final String cls : names) {
        final String clsField = this.out.getImports().addStatic(cls);
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

  public void arrayOfAnnotations(final TreeLogger logger, final GeneratorContext ctx, final MemberBuffer<?> buffer, final UnifyAstView ast, final Annotation ... annos) throws UnableToCompleteException {
    if (annos.length == 0) {
      buffer.print(
          buffer.addImportStatic(ArrayConsts.class, "EMPTY_ANNOTATIONS")
          );
      return;
    }
    String flatName;
    {
      final TreeSet<String> ordered = new TreeSet<String>();
      for (final Annotation anno : annos) {
        ordered.add(anno.toString());
      }
      flatName = ordered.toString();
    }

    final String existing = annoArrays.get(flatName);
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
        items[i] = rememberAnnotation(logger, ctx, annos[i], ast);
      }
    }

    final int pool = initBuffer();
    final String constName = getConstName(pool);
    final String arrayName = "ANNO_ARR_"+annoArrays.size();
    final String finalName = "constants."+constName+"."+arrayName;
    annoArrays.put(flatName, finalName);


    final PrintBuffer init = out
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

  private String rememberAnnotation(final TreeLogger logger, final GeneratorContext ctx, final Annotation anno, final UnifyAstView ast) throws UnableToCompleteException {
    String existing = annos.get(anno);
    if (existing != null) {
      return existing;
    }
    final String constName = getConstName(initBuffer());
    final String annoName = "ANNO_"+annos.size();

    existing = "constants."+constName+"."+annoName;
    annos.put(anno, existing);

    final Method[] methods = ReflectionUtilJava.getMethods(anno);
    final String[] values = new String[methods.length];
    for (int i = 0, m = methods.length; i < m; i++) {
      try {
      final Object value = methods[i].invoke(anno);
      if (value instanceof Annotation) {
        values[i] = out.getImports().addStatic(rememberAnnotation(logger, ctx, (Annotation) value, ast));
      } else if (value instanceof Class){
        values[i] =
            out.getImports().addImport(((Class<?>)value).getCanonicalName())
            +".class";
      } else if (value instanceof Enum) {
        final Enum<?> e = (Enum<?>) value;
        values[i] =
            out.getImports().addStatic(e.getDeclaringClass().getCanonicalName()+"."+e.name());
      } else if (value instanceof String) {
        values[i] = "\""+GwtReflect.escape((String)value)+"\"";
      } else {
        values[i] = String.valueOf(value);
      }
      } catch (final Exception e) {
        logger.log(Type.ERROR, "Fatal error reading values reflectively from annotation "+anno);
        throw new UnableToCompleteException();
      }
    }

    final PrintBuffer init = out
        .getClassBuffer()
        .createField(anno.annotationType(), annoName)
        .makeFinal()
        .makePackageProtected()
        .getInitializer();

    final GeneratedAnnotation gen = GwtAnnotationGenerator.generateAnnotationProxy(logger, anno, ast);
//    IsNamedType known = gen.knownInstances.get(anno);
//    if (known != null) {
//    }
    init
      .print("new ")
      .print(out.getImports().addImport(gen.getProxyName()))
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
