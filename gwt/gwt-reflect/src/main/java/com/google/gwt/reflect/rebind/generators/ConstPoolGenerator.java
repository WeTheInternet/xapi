package com.google.gwt.reflect.rebind.generators;

import static java.lang.reflect.Modifier.FINAL;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.reflect.client.ConstPool.ArrayConsts;
import com.google.gwt.reflect.client.ConstPool.ClassConsts;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.model.GeneratedAnnotation;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.xapi.dev.source.ClassBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.ImportSection;
import com.google.gwt.thirdparty.xapi.dev.source.MemberBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.PrintBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.SourceBuilder;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsNamedType;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsQualified;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Supplier;

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
//      return new ConstPoolGenerator(iteration);
      return new ConstPoolGenerator(iteration++);
    };

  };

  public static void cleanup() {
    generator.remove();
  }

  public static ConstPoolGenerator getGenerator() {
    return generator.get();
  }

  private final int iteration;

  private final Map<Long,String> longs = Maps.newHashMap();
  private final Map<String,String> strings = Maps.newHashMap();
  private final Map<String,String> classes = Maps.newHashMap();
  private final Map<String,String> classArrays = Maps.newHashMap();
  private final Map<String,String> stringArrays = Maps.newHashMap();
  private final Map<String,String> enumArrays = Maps.newHashMap();
  private final Map<Annotation,IsNamedType> annotations = Maps.newHashMap();
  private final Map<String,String> annotationArrays = Maps.newHashMap();
  private final Map<String,IsQualified> annotationArraySuppliers = Maps.newHashMap();

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

  private final HashMap<Object,int[]> arraySeeds = new HashMap<Object,int[]>();

  private transient ArrayList<SourceBuilder<?>> sourceClasses = new ArrayList<SourceBuilder<?>>();
  private transient SourceBuilder<?> out;

  private ConstPoolGenerator(final int iteration) {
    this.iteration = iteration;
  }

  public void commit(final TreeLogger logger, final GeneratorContext ctx) {
    int pos = sourceClasses.size()-1;
    final Type logLevel = logLevel();
    final boolean doLog = logger.isLoggable(logLevel);
    while (out != null) {
      // commit const pool(s)
      final PrintWriter pw = ctx.tryCreate(logger, out.getPackage(), out.getClassBuffer().getSimpleName());
      final String src = out.toString();
      if (doLog) {
        logger.log(logLevel, "Generating constPool: \n"+src);
      }
      pw.println(src);
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

  private Type logLevel() {
    return Type.DEBUG;

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
      out = new SourceBuilder<Object>("public interface Const"+iteration+"_"+sourceClasses.size());
      out.setPackage("constants");
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

  public String rememberClass(final Class<?> type) {
    return rememberClass(type.getCanonicalName());
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
      name = "constants."+cls+"."+name;
      classes.put(type, name);
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
      name = "constants."+cls+"."+name;
      strings.put(value, name);
    }
    return name;
  }

  public void arrayOfClasses(final TreeLogger logger, final MemberBuffer<?> buffer, final String ... names) {
    buffer.print(
      buffer.addImportStatic(rememberArrayOfClasses(logger, names) )
    );
  }

  public String rememberArrayOfClasses(final TreeLogger logger, final String ... names) {

    if (names.length == 0) {
      return ArrayConsts.class.getCanonicalName()+".EMPTY_CLASSES";
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
    return existing;
  }
  public String rememberArrayOfStrings(final TreeLogger logger, final String ... strs) {

    if (strs.length == 0) {
      return ArrayConsts.class.getCanonicalName()+".EMPTY_STRINGS";
    }

    String shortName;
    {
      final StringBuilder b = new StringBuilder();
      for (int i = strs.length; i --> 0; ) {
        b.append(strs[i]).append(',');
        strs[i] = rememberString(strs[i]);
      }
      shortName = b.toString();
    }

    String existing = stringArrays.get(shortName);
    if (existing == null) {
      final int pool = initBuffer();
      final String constName = getConstName(pool);
      final String name = "STRING_ARR_"+stringArrays.size();
      existing = "constants."+constName+"."+name;
      stringArrays.put(shortName, existing);

      final ClassBuffer b = this.out.getClassBuffer();
      final PrintBuffer init = b
          .createField("String[]",name)
          .setModifier(FINAL)
          .makePackageProtected()
          .getInitializer();

      init
      .println("new String[]{")
      .indent();
      String prefix = "";
      for (final String str : strs) {
        init.print(prefix).println(out.getImports().addStatic(str));
        prefix = ", ";
      }
      init.outdent();
      init.print("}");

    }
    return existing;
  }

  public String rememberLong(final TreeLogger logger, final long longValue) {
    String existing = longs.get(longValue);
    if (existing == null) {
      final int pool = initBuffer();
      final String constName = getConstName(pool);
      final String name = "LONG_"+longs.size();
      existing = "constants."+constName+"."+name;
      longs.put(longValue, existing);

      this.out.getClassBuffer()
        .createField("long",name)
        .setModifier(FINAL)
        .makePackageProtected()
        .getInitializer()
        .println(longValue+"L");
    }
    return existing;
  }

  public void arrayOfPrimitives(final TreeLogger logger, final MemberBuffer<?> buffer, final Object primitives) throws UnableToCompleteException {
    buffer.print(
      buffer.addImportStatic(rememberPrimitiveArray(logger, primitives))
    );
  }

  public void arrayOfObjects(final TreeLogger logger, final GeneratorContext ctx, final MemberBuffer<?> buffer, final Object ... objects) throws UnableToCompleteException {
    buffer.print(
      buffer.addImportStatic(rememberObjectArray(logger, ctx, objects))
    );
  }

  public void arrayOfAnnotations(final TreeLogger logger, final GeneratorContext ctx, final MemberBuffer<?> buffer, final Annotation ... annos) throws UnableToCompleteException {
    buffer.print(
      buffer.addImportStatic(rememberArrayOfAnnotations(logger, ctx, annos))
    );
  }

  public IsQualified annotationArraySupplier(final TreeLogger logger, final GeneratorContext ctx, final MemberBuffer<?> buffer, final Annotation ... annos) throws UnableToCompleteException {
    final String constant = rememberArrayOfAnnotations(logger, ctx, annos);
    IsQualified supplier = annotationArraySuppliers.get(constant);
    if (supplier == null) {
      final String simpleName = "AnnotationSupplier"+iteration+"_"+annotationArraySuppliers.size();
      final String pkgName = "constants";
      supplier = new IsQualified(pkgName, simpleName);
      final PrintWriter pw = ctx.tryCreate(logger, pkgName, simpleName);
      final SourceBuilder<Object> sb = new SourceBuilder<Object>("public class "+simpleName);
      sb.setPackage(pkgName);
      final ClassBuffer out = sb.getClassBuffer();

      final String supplierClass = out.addImport(Supplier.class);
      final String annotationClass = out.addImport(Annotation.class);
      out.addInterface(supplierClass+"<"+annotationClass+"[]>");

      out.createMethod("public "+annotationClass+"[] get()")
        .returnValue(constant);

      pw.print(sb.toString());
      ctx.commit(logger, pw);
      annotationArraySuppliers.put(constant, supplier);
    }
    return supplier;
  }

  @SuppressWarnings("unchecked")
  public <A extends Annotation> String rememberArrayOfAnnotations(final TreeLogger logger, final GeneratorContext ctx, final A ... annos) throws UnableToCompleteException {
    final String flatName = getFlatName(annos);
    final String existing = annotationArrays.get(flatName);
    if (existing != null) {
      return existing;
    }

    String[] items;
    {
      int i = annos.length;
      items = new String[i];
      for (; i-->0;) {
        items[i] = rememberAnnotation(logger, ctx, annos[i]).getQualifiedMemberName();
      }
    }

    out = null;
    final int pool = initBuffer();
    final String constName = getConstName(pool);
    final String arrayName = "ANNO_ARR_"+annotationArrays.size();
    final String finalName = "constants."+constName+"."+arrayName;
    annotationArrays.put(flatName, finalName);


    final PrintBuffer init = out
      .getClassBuffer()
      .createField(annos.getClass(), arrayName)
      .makePackageProtected()
      .makeFinal()
      .getInitializer()
    ;
    final ImportSection imports = out.getImports();
    final String typeName = imports.addImport(annos.getClass().getComponentType());
    init.print("new " + typeName + "[]{");
    if (items.length > 0) {
      init
      .indent()
      .println()
      .println(imports.addStatic(items[0]));
      for (int i = 1; i < items.length; i++) {
        init.print(",").println(imports.addStatic(items[i]));
      }
    }
    init
      .outdent()
      .print("}");

    return finalName;
  }

  @SuppressWarnings("unchecked")
  public <E extends Enum<E>> String rememberArrayOfEnums(final TreeLogger logger, final E ... enums) throws UnableToCompleteException {
    final String flatName = getFlatName(enums);
    final String existing = enumArrays.get(flatName);
    if (existing != null) {
      return existing;
    }

    final int pool = initBuffer();
    final String constName = getConstName(pool);
    final String arrayName = "ENUM_ARR_"+enumArrays.size();
    final String finalName = "constants."+constName+"."+arrayName;
    enumArrays.put(flatName, finalName);


    final PrintBuffer init = out
        .getClassBuffer()
        .createField(enums.getClass(), arrayName)
        .makePackageProtected()
        .makeFinal()
        .getInitializer()
        ;
    final Class<?> enumType = enums.getClass().getComponentType();
    final ImportSection imports = out.getImports();
    final String type = imports.addImport(enumType);
    init.print("new "+type+"[]{");
    if (enums.length > 0) {
      init
        .indent()
        .println()
        .println(imports.addStatic(enumType, enums[0].name()));
      for (int i = 0; i < enums.length; i++) {
        init.print(",").println(imports.addStatic(enumType, enums[i].name()));
      }
    }
      init
      .outdent()
      .print("}");

    return finalName;
  }

  private String getFlatName(final Annotation[] annos) {
    final TreeSet<String> ordered = new TreeSet<String>();
    ordered.add(annos.getClass().getComponentType().getCanonicalName());
    for (final Annotation anno : annos) {
      ordered.add(anno.toString());
    }
    return ordered.toString();
  }

  private <E extends Enum<E>> String getFlatName(final E[] enums) {
    final TreeSet<String> ordered = new TreeSet<String>();
    ordered.add(enums.getClass().getComponentType().getCanonicalName());
    for (final E enumItem : enums) {
      ordered.add(enumItem.name());
    }
    return ordered.toString();
  }

  public String[] getSourceNames(final Class<?> ... classes) {
    final String[] names = new String[classes.length];
    for (int i = classes.length; i --> 0; ) {
      assert classes[i] != null : "Null classes are not supported";
      names[i] = classes[i].getCanonicalName();
    }
    return names;
  }

  public IsNamedType rememberAnnotation(final TreeLogger logger, final GeneratorContext ctx, final Annotation anno) throws UnableToCompleteException {
    IsNamedType existing = annotations.get(anno);
    if (existing != null) {
      return existing;
    }

    // Whenever we start an annotation, we want to do so in a fresh class, so, we null out our sourceBuilder
    // to force the initBuffer() to add a new class
    out = null;
    final String constName = getConstName(initBuffer());
    final String annoName = "ANNO_"+annotations.size();
    final SourceBuilder<?> sb = out;

    existing = new IsNamedType("constants."+constName, annoName);
    annotations.put(anno, existing);

    final Method[] methods = ReflectionUtilJava.getMethods(anno);
    final String[] values = new String[methods.length];
    for (int i = 0, m = methods.length; i < m; i++) {
      try {
      final Object value = methods[i].invoke(anno);
      if (value.getClass().isArray()) {
        // Arrays need extra-special lovin'
        if (value.getClass().getComponentType().isPrimitive()) {
          values[i] = rememberPrimitiveArray(logger, value);
        } else {
          values[i] = rememberObjectArray(logger, ctx, (Object[])value);
        }
      } else {
        if (value instanceof Annotation) {
          final IsNamedType provider = rememberAnnotation(logger, ctx, (Annotation) value);
          values[i] = sb.getImports().addStatic(provider.getQualifiedMemberName());
        } else if (value instanceof Class){
          values[i] =
              sb.getImports().addImport(((Class<?>)value).getCanonicalName())
              +".class";
        } else if (value instanceof Enum) {
          final Enum<?> e = (Enum<?>) value;
          values[i] =
              sb.getImports().addStatic(e.getDeclaringClass().getCanonicalName()+"."+e.name());
        } else if (value instanceof String) {
          values[i] = "\""+GwtReflect.escape((String)value)+"\"";
        } else {
          values[i] = String.valueOf(value);
        }
      }
      } catch (final Exception e) {
        logger.log(Type.ERROR, "Fatal error reading values reflectively from annotation "+anno);
        throw new UnableToCompleteException();
      }
    }

    final PrintBuffer init = sb
        .getClassBuffer()
        .createField(anno.annotationType(), annoName)
        .makeFinal()
        .makePackageProtected()
        .getInitializer();

    final GeneratedAnnotation gen = GwtAnnotationGenerator.generateAnnotationProxy(logger, anno, ctx);
    init
      .print("new ")
      .print(sb.getImports().addImport(gen.getProxyName()))
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

  private String rememberPrimitiveArray(final TreeLogger logger, final Object array) throws UnableToCompleteException {
    final Class<?> componentType = array.getClass().getComponentType();
    // Primitives
    final String name = componentType.getName();
    final StringBuilder b = new StringBuilder("new "+name+"[]{");
    String prefix = "";
    for (int i = 0, m = Array.getLength(array); i < m; i ++ ) {
      switch (name) {
        case "boolean":
          b.append(prefix).append(Array.getBoolean(array, i));
          break;
        case "byte":
          b.append(prefix).append(Array.getByte(array, i));
          break;
        case "short":
          b.append(prefix).append(Array.getShort(array, i));
          break;
        case "char":
          b.append(prefix).append(Array.getChar(array, i));
          break;
        case "int":
          b.append(prefix).append(Array.getInt(array, i));
          break;
        case "long":
          b.append(prefix).append(Array.getLong(array, i));
          break;
        case "float":
          b.append(prefix).append(Array.getFloat(array, i));
          break;
        case "double":
          b.append(prefix).append(Array.getDouble(array, i));
          break;
        default:
          throw new UnsupportedOperationException();
      }
      prefix = ", ";
    }
    b.append("}");
    return b.toString();

  }

  @SuppressWarnings("unchecked")
  private <T> String rememberObjectArray(final TreeLogger logger, final GeneratorContext ctx, final T ... values) throws UnableToCompleteException {
    final Class<?> componentType = values.getClass().getComponentType();
    if (componentType.isAnnotation()) {
      return rememberArrayOfAnnotations(logger, ctx, (Annotation[])values);
    } else if (componentType == Class.class) {
      return rememberArrayOfClasses(logger, getSourceNames((Class<?>[])values));
    } else if (componentType.isEnum()) {
      return rememberArrayOfEnums(logger, (Enum[])values);
    } else if (componentType == String.class) {
      return rememberArrayOfStrings(logger, (String[])values);
    } else {
      throw new UnsupportedOperationException("Cannot call rememberObjectArray on a array of type "+componentType+".  You want rememberPrimitiveArray instead.\n"
          + "\nFailure on: "+Arrays.asList(values));
    }
  }

  public static void maybeCommit(final TreeLogger logger, final GeneratorContext ctx) {
    getGenerator().commit(logger, ctx);
  }

}
