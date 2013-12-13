package xapi.dev.reflect;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import xapi.annotation.reflect.KeepArrays;
import xapi.annotation.reflect.KeepClass;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.except.NotConfiguredCorrectly;
import xapi.reflect.impl.GwtDevReflectionService;
import xapi.reflect.service.ReflectionService;
import xapi.util.X_Runtime;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.reflect.client.GwtReflectJre;

public class MonolithReflectionGenerator extends IncrementalGenerator{

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext context, String typeName)
    throws UnableToCompleteException {

    if (!context.isProdMode()) {
      return new RebindResult(RebindMode.USE_EXISTING, GwtDevReflectionService.class.getName());
    }

    // Our default reflection service generator;
    // gwt dev will return a simple class that gives direct access to jre class
    // prod mode, however, is limited to supporting properly annotated types.
    // This means we have to iterate and inspect all types found... ick.
    TypeOracle oracle = context.getTypeOracle();


    String resultPackage = "xapi.reflect.impl";
    String resultType = "MonolithicReflectionService";
    String fqcn = resultPackage + "."+resultType;
    PrintWriter pw = null;
    pw = context.tryCreate(logger, resultPackage, resultType);
    String next = resultType;
    int unique = 0;
    while (pw == null) {
      // Due to super-dev-mode recompiler, we have to use a fresh name for our class each iteration.
      next = resultType+"_"+unique++;
      pw = context.tryCreate(logger, resultPackage, next);
    }
    resultType = next;

    Set<JClassType> needMagicClass = new LinkedHashSet<JClassType>();
    for (JClassType type : oracle.getTypes()) {
      if (type.isAnnotationPresent(KeepClass.class)) {
        needMagicClass.add(type);
      }
    }
    Map<JClassType, KeepArrays> needArrays = new LinkedHashMap<JClassType, KeepArrays>();
    for (JClassType type : oracle.getTypes()) {
      KeepArrays keeper = type.getAnnotation(KeepArrays.class);
      if (keeper != null) {
        needArrays.put(type, keeper);
      }
    }

    // Now that we have our types, let's get to building the service...


    SourceBuilder<Object> sw =
      new SourceBuilder<Object>("public class "+resultType)
      .setPackage(resultPackage)
    ;

    ClassBuffer buffer =
      sw.getClassBuffer()
      .addInterfaces(ReflectionService.class)
      ;

    // Add some static fields for reflection support.
    buffer
      .indent()
      .println("private static final Object[] OBJECT_ARRAY = new Object[0];")
      .outdent()
      .println();

    // switch/case from class to provider method is the best a standard monolithic generator can do.
    MethodBuffer magicClass = buffer.createMethod(
    "public native <T> Class<T> magicClass(Class<T> cls)")
      .setUseJsni(true)
      .println("switch(cls){");
      ;
    for (JClassType type : needMagicClass) {
      // Generate a magic-class for this type...
      RebindResult cls = MagicClassGenerator.execImpl(logger, context, type);
      magicClass //jsni
        .println("case @"+type.getQualifiedSourceName()+"::class: ")
        .indentln("return @"+cls.getResultTypeName()+"::enhanceClass(Ljava/lang/Class;)(cls);");
    }
    // close our method
    magicClass
      .println("default:")
      .indentln("return cls;")
      .println("}");

    
    // next up, packages.  They are compiled into magic classes, so we can avoid duplication here.
    buffer.createMethod(
      "public Package getPackage(Object o)")
      .println("if (o instanceof Class){")
      .indent()
      .println("// Make sure we are using an enhanced class")
      // This allows us to avoid a second giant switch/case statement
      .println("if (o == Class.class){")
      .println("return magicClass((Class)o).getPackage();")
      .println("}")
      .println("return ((Class)o).getPackage();")
      .outdent()
      .println("} else if (o == null){")
      .indentln("throw new NullPointerException(); ")
      .println("} else {")
      .indentln("return o.getClass().getPackage(); ")
      .println("}")
    ;
    
    buffer.createMethod(
      "public Package getPackage(String name, ClassLoader cl)")
      .returnValue(buffer.addImport(GwtReflectJre.class)+".getPackage(name, cl);")
    ;
    
    // Now, let's generate support for arrays!
    // This is a little tougher, since we do allow an annotation to specify other types
    // (so you can keep a given array type without having to annotate the class itself).
    
    MethodBuffer newArraySingle = buffer.createMethod(
        "public native <T> T[] newArray(" +
        "Class<T> classLit, " +
        "int dimension)")
        .setUseJsni(true)
        .println("switch(classLit){")
        .indent()
          .println("case @java.lang.Object::class :")
          .indentln("return @com.google.gwt.lang.Array::createFrom([Ljava/lang/Object;I)" +
              "(@" + fqcn +"::OBJECT_ARRAY, dimension);");
        ;

    MethodBuffer newArrayMulti = buffer.createMethod(
      "public native <T> T[] newArray(" +
      "Class<T> classLit, " +
      "int ... dimensions)")
      .setUseJsni(true)
      .println("switch(classLit){")
    ;
    
    
    
    newArraySingle
      .println("default:")
      .indentln("@"+fqcn+"::throwNoArraySupport(Ljava/lang/Class;)(classLit)")
      .indentln("return null;")
      .println("}");

    newArrayMulti
      .println("default:")
      .indentln("@"+fqcn+"::throwNoArraySupport(Ljava/lang/Class;)(classLit)")
      .indentln("return null;")
      .println("}");

    buffer
      .createMethod("private static void throwNoArraySupport(Class<?> c)")
      .println("throw new NotConfiguredCorrectly(\"" +
        "No array reflection support for \"+ c.getName() + \"; " +
        "ensure this class is annotated with @KeepArrays."+
        (X_Runtime.isDebug()?" This annotation allows you to add support for " +
        		"any class with the alsoKeep() method, that allows you to keep " +
        		"array support for types without directly annotating them (like java.lang classes).":"")+
  		"\");");
    sw.getImports().addImport(NotConfiguredCorrectly.class.getName());


    if (X_Runtime.isDebug()) {
      logger.log(Type.INFO, "Generated reflection support for "+needMagicClass.size()+" classes.");
      logger.log(Type.WARN, sw.toString());
    }

    pw.append(sw.toString());
    context.commit(logger, pw);

    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, resultPackage + "." + resultType);
  }

  @Override
  public long getVersionId() {
    return 1;
  }

}
