package com.google.gwt.reflect.rebind.generators;

import static com.google.gwt.reflect.rebind.ReflectionUtilJava.qualifiedName;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.model.GeneratedAnnotation;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.reflect.shared.JsMemberPool;
import com.google.gwt.thirdparty.xapi.dev.source.ClassBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.FieldBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.MethodBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.SourceBuilder;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsNamedType;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class GwtAnnotationGenerator {

  private static final Type logLevel = Type.TRACE;

  /**
   * A cache of annotation types that have already been finished (seen during this compile).
   *<p>
   * This map is used to ensure we don't generate the same annotation twice,
   * and deterministically detect when a type has changed or not (i.e., we need to
   * generate and use a new wrapper type.
   * <p>
   * An annotation's method structure (probably) won't change during a single gwt compile,
   * so we can map from a seen annotation class to an instance of a {@link GeneratedAnnotation}.
   * <p>
   * If a type exists in this map, it has already been seen and can be immediately reused.
   * <br>
   * In the event that a cached type with our expected generated class name exists,
   * we can check that the existing class source exactly matched what we generate.
   * <p>
   * If a type is simply being re-used across super dev mode recompiles,
   * we will still have to generate the source for the annotation proxy the first time,
   * but we will only append _n to the class name and generate a new type
   * if indeed the annotation's structure has changed.
   * <p>
   * We will still need to updated the knownInstances method,
   * so we can easily generate the constructor methods to create an instance of an annotation proxy
   * (and reuse existing JMethod calls which create the exact annotation we need to reuse).
   *
   */
  private static Map<Class<? extends Annotation>,GeneratedAnnotation> finished
    = new ConcurrentHashMap<Class<? extends Annotation>,GeneratedAnnotation>();

  /**
   * Called when the {@link UnifyAstListener#destroy()} methods are called.
   *
   */
  public static void cleanup() {
    finished.clear();
  }


  /**
   * Print a method to fill in a {@link JsMemberPool} with the annotations being retained for
   * the supplied type.
   *
   * @param logger -> The {@link TreeLogger}
   * @param sourceBuilder -> The {@link SourceBuilder} used to print source into
   * @param injectionType -> The {@link JClassType} that was are generating annotation proxies for
   * @param ast -> The {@link UnifyAstView} for looking up annotations from JJS AST nodes
   * @param annotations -> The array of {@link Annotation}s to generate
   * @return -> An array of {@link GeneratedAnnotation}s produced from running this generator
   *
   * @throws UnableToCompleteException if any fatal error occurs.
   */
  static GeneratedAnnotation[] printAnnotationEnhancer(
    final TreeLogger logger, final SourceBuilder<?> sourceBuilder,
    final JClassType injectionType, final UnifyAstView ast,
    final Annotation ... annotations
  ) throws UnableToCompleteException {

    final ClassBuffer out = sourceBuilder.getClassBuffer();
    final String memberPool = out.addImport(JsMemberPool.class);
    final MethodBuffer initAnnos = out
      .createMethod("private static void enhanceAnnotations("+memberPool+" pool)");

    if (annotations.length == 0){
      // Short circuit for annotationless types
      return new GeneratedAnnotation[0];
    }

    final List<GeneratedAnnotation> results = new ArrayList<GeneratedAnnotation>();
    for (int i = 0, max = annotations.length; i < max; i++ ) {

      final Annotation annotation = annotations[i];
      // Generate a proxy class
      final GeneratedAnnotation generated = generateAnnotationProxy(logger, annotation, ast);
      // Check for a provider method to create the supplied configuration of an annotation
      IsNamedType providerMethod;
      if (generated.hasProviderMethod(annotation)) {
        // Reuse existing method
        providerMethod = generated.getProviderMethod(annotation);
      } else {
        // Create new factory method.
        providerMethod = generateProvider(logger, sourceBuilder, annotation, generated, ast);
        // Save the factory method for reuse if the exact same annotation is reused
        generated.addProviderMethod(annotation, providerMethod);
      }
      results.add(generated);
      // Use a static import on the factory method
      final String getAnno = out.addImportStatic(providerMethod.getQualifiedName() + "." + providerMethod.getName());
      final boolean isDeclared = injectionType.getAnnotation(annotation.annotationType()) != null;
      // Record the annotation instance in the member pool
      initAnnos.println("pool.addAnnotation("+getAnno+"(), "+isDeclared + ");");
    }// end for

    return results.toArray(new GeneratedAnnotation[results.size()]);
  }

  /**
   * Generates an annotation proxy class which implements the supplied annotation.
   * <p>
   * The created class is immutable, takes all values as constructor arguments, and implements
   * hashCode, equals and toString.  The equals method makes optimizing assumptions that the
   * annotation it will be compared to was also emitted by this generator.
   *
   * @param logger -> The {@link TreeLogger}
   * @param anno -> The {@link Annotation} we are generating a proxy for
   * @param ast -> The {@link UnifyAstView}, necessary to load annotations from JJS AST nodes
   * @return -> A {@link GeneratedAnnotation} instance used to map instances of the given annotation
   *
   * @throws UnableToCompleteException -> If a fatal error occurs
   */
  protected static GeneratedAnnotation generateAnnotationProxy(TreeLogger logger,
    final Annotation anno, final UnifyAstView ast) throws UnableToCompleteException {

    final boolean doLog = logger.isLoggable(logLevel);
    final Class<? extends Annotation> annoType = anno.annotationType();
    final TypeOracle oracle = ast.getTypeOracle();

    // Check to see if the proxy class has already been generated
    GeneratedAnnotation gen = finished.get(annoType);
    if (gen == null) {

      // Step one is to generate a class implementing the annotation.
      // This annotation type will likely have been seen before,
      // but it may have changed (across gwt compiles in super dev mode).
      final String proxyPkg = annoType.getPackage().getName();
      String proxyName = toProxyName(annoType.getCanonicalName(), proxyPkg);
      String proxyFQCN = qualifiedName(proxyPkg, proxyName);

      // Check if the proxy type already exists
      if (doLog) {
        logger = logger.branch(logLevel, "Checking for existing "+proxyFQCN+" on classpath");
      }
      final JClassType exists = oracle.findType(proxyFQCN);
      boolean mustGenerate = exists == null;

      // Now, just because the class exists does not mean it is correct. Lets check its structure
      if (doLog) {
        logger = logger.branch(logLevel,
          mustGenerate ? "No existing type "+ proxyFQCN+" on classpath; " :
          "Checking if existing "+proxyFQCN+" matches "+anno);
      }
      if (!mustGenerate) {
        // If a type exists, make sure the method patterns match.
        final JClassType annoJClass = oracle.findType(anno.annotationType().getCanonicalName());
        mustGenerate = typeMatches(logger, exists, annoJClass);
      }

      if (mustGenerate) {
        // Create a proxy type matching our annotation

        final StandardGeneratorContext context = ast.getGeneratorContext();
        // Step one is to get a printwriter from the gwt generator context
        PrintWriter pw = context.tryCreate(logger, proxyPkg, proxyName);
        int inc=-1;
        if (pw == null) {
          // null means name's taken.  Increment and try again.
          while (inc < 100) {
            if (doLog) {
              logger.log(logLevel, "PrintWriter for "+proxyFQCN+" not available.  Incrementing name");
            }
            final String attempt = proxyName+"_"+(++inc);
            pw = context.tryCreate(logger, proxyPkg, attempt);
            if (pw != null) {
              proxyName = attempt;
              proxyFQCN = proxyFQCN+"_"+inc;
              break;
            }
          throw new UnableToCompleteException();
          }
        }
        if (doLog) {
          logger.log(logLevel, "Generating new annotation proxy "+proxyFQCN+".");
        }

        // We've got our class name, now create the proxy class implementation
        gen = new GeneratedAnnotation(anno, proxyFQCN);
        final SourceBuilder<GeneratedAnnotation> sw = new SourceBuilder<GeneratedAnnotation>(
          "public class "+proxyName
          ).setPackage(proxyPkg);
        sw.setPayload(gen);// allow the source builder to access GeneratedAnnotation
        // cache this type _before_ we start generating,
        // as it is possible to recurse into the same type more than once
        // when generating annotations that have other annotations as members.
        finished.put(annoType, gen);

        // create this annotation proxy, and any proxies needed in its fields.
        generateProxy(logger, anno, sw.getClassBuffer(), proxyPkg, proxyName);

        // maybe log our generated contents
        final String src = sw.toString();
        if (logger.isLoggable(Type.DEBUG)) {
          logger.log(Type.DEBUG, "Debug dump of generated annotation:\n"+src);
        }

        // Actually save the file
        pw.println(src);
        context.commit(logger, pw);

        assert inc < 100 : "Generator context cannot create a printwriter; " +
          "check that your tmp / -out directory is not full.";
      } else {
        gen = new GeneratedAnnotation(anno, exists.getQualifiedSourceName());
        finished.put(annoType, gen);
      }
      gen.setProxyClassType(exists);
    }
    return gen;
  }

  private static String toProxyName(final String canonicalName, final String pkg) {
    return canonicalName.replace(pkg+".", "").replace('.', '_')+"Proxy";
  }

  static String toUniqueName(final Class<?> cls) {
    return cls.getCanonicalName().replace('.', '_');
  }

  public static IsNamedType findExisting(final TreeLogger logger, final Annotation anno, final UnifyAstView ast) throws UnableToCompleteException {
    final GeneratedAnnotation gen = finished.get(anno.annotationType());
    if (gen != null) {
      final TypeOracle oracle = ast.getTypeOracle();
      final JClassType existing = oracle.findType(gen.getProxyName());
      final JClassType original = oracle.findType(anno.annotationType().getCanonicalName());
      if (typeMatches(logger, existing, original)) {
        return gen.getProviderMethod(anno);
      }
    }
    return null;
  }

  public static IsNamedType generateAnnotationProvider(final TreeLogger logger, final SourceBuilder<?> out
    , final Annotation anno, final UnifyAstView ast) throws UnableToCompleteException {
    final GeneratedAnnotation gen = generateAnnotationProxy(logger, anno, ast);
    final IsNamedType provider = generateProvider(logger, out, anno, gen, ast);
    if (logger.isLoggable(logLevel)) {
      logger.log(logLevel, "Generating annotation proxy "+provider.getQualifiedMemberName()+" for "+gen.getAnnoName());
    }
    return provider;
  }
  private static IsNamedType generateProvider(final TreeLogger logger, final SourceBuilder<?> out
    , final Annotation anno, final GeneratedAnnotation gen, final UnifyAstView ast) throws UnableToCompleteException {
    final GeneratorContext context = ast.getGeneratorContext();
    if (gen.hasProviderMethod(anno)) {
      return gen.getProviderMethod(anno);
    }

    final String method = gen.getMethodName(anno);
    // Cache the method name we're going to use, before we use it.
    final IsNamedType type = new IsNamedType(method, out.getQualifiedName());
    gen.addProviderMethod(anno, type);

    final String proxyName = anno.annotationType().getSimpleName()+"Proxy";
    final ConstPoolGenerator constGenerator = null;
    out.getImports().addImport(anno.annotationType().getCanonicalName()+"Proxy");
    final MethodBuffer mb = out.getClassBuffer()
      .createMethod("public static "+proxyName+" "+method+"()")
      .print("return new "+proxyName+"(");

    final Method[] methods = ReflectionUtilJava.getMethods(anno);
    final int len = methods.length;
    for (int i = 0; i < len; i ++ ) {
      final Method m = methods[i];
      final Class<?> returnType = m.getReturnType();
      Object value;
      try {
        value = m.invoke(anno);
      } catch (final Exception e) {
        logger.log(Type.ERROR, "Error generating annotation proxy provider method." +
          "\nCould not invoke "+m+" on "+anno, e);
        throw new UnableToCompleteException();
      }
      if (i > 0) {
        mb.print(", ");
      }
      if (Annotation.class.isAssignableFrom(returnType)) {
        final Annotation asAnno = (Annotation)value;
        final GeneratedAnnotation result = generateAnnotationProxy(logger, asAnno, ast);
        final IsNamedType provider = generateProvider(logger, out, asAnno, result, ast);
        final String methodName = mb.addImportStatic(provider.getQualifiedMemberName());
        mb.print(methodName+"()");
        mb.addImport(asAnno.annotationType().getCanonicalName()+"Proxy");
      } else if (returnType.isArray()){
        if (Annotation.class.isAssignableFrom(returnType.getComponentType())) {
          mb.println("new "+returnType.getComponentType().getCanonicalName()+"[]{");
          for (int ind = 0, length = GwtReflect.arrayLength(value); ind < length; ind++ ) {
            if (ind > 0) {
              mb.print(", ");
            }
            final Annotation asAnno = (Annotation)GwtReflect.arrayGet(value, ind);
            final GeneratedAnnotation result = generateAnnotationProxy(logger, asAnno, ast);
            final IsNamedType provider = generateProvider(logger, out, asAnno, result, ast);
            mb.addImport(asAnno.annotationType().getCanonicalName()+"Proxy");
            mb.print(provider.getQualifiedName()+"."+provider.getName()+"()");
          }
          mb.println("}");
        } else {
          // TODO generate ConstPool array references
          mb.print(ReflectionUtilJava.sourceName(value));
        }
      } else if (value instanceof Long){
        // Longs must, unfortunately, be generated as ConstPool references in order for == comparisons
        // to function correctly

        mb.print(ReflectionUtilJava.sourceName(value));
      } else {
        // Enums and primitives, we will simply generate a reference to the fields,
        // as they will all be safe to perform == comparisons in javascript
        mb.print(ReflectionUtilJava.sourceName(value));
      }
    }

    mb.println(");");

    if (constGenerator != null) {
      constGenerator.commit(logger, context);
    }

    return type;
  }


  private static void generateProxy(final TreeLogger logger, final Annotation anno,
  final ClassBuffer cw, final String proxyPkg, final String proxyName) {
    assert proxyPkg.equals(anno.annotationType().getPackage().getName());
    assert proxyName.equals(anno.annotationType().getCanonicalName().replace(
        anno.annotationType().getPackage().getName()+".","").replace('.','_')+"Proxy");
    cw.addImport(Annotation.class);
    cw.addInterface(anno.annotationType());
    final MethodBuffer ctor = cw.createConstructor(Modifier.PUBLIC);
    // All public methods, include those from Object
    final Method[] methods = anno.annotationType().getMethods();
    final Object[] defaults = new Object[methods.length];
    final Map<String, Method> valueMethods = new HashMap<String, Method>();
    for (int i = 0, m = methods.length; i < m; i++) {
      final Method method = methods[i];
      final String clsName = method.getDeclaringClass().getName();
      if (clsName.equals("java.lang.Object")) {
        continue;
      }
      if (!clsName.equals("java.lang.annotation.Annotation")) {
        // A method the client has declared
        final Class<?> returnType = method.getReturnType();
        final String simpleName = method.getName();
        final String paramName = ReflectionUtilJava.toSourceName(method.getGenericReturnType())
          +" "+simpleName;
        final Object defaultValue = defaults[i] = method.getDefaultValue();
        valueMethods.put(simpleName, method);

        final FieldBuffer field = cw.createField(returnType, method.getName())
          .setExactName(true)
          .makePrivate()
          .makeFinal()
          ;
        field.addGetter(Modifier.PUBLIC);

        ctor.addParameters(paramName);
        ctor.println("this."+simpleName+" = "+simpleName+";");

        if (defaultValue != null) {
          if (returnType.isPrimitive()) {

          } else {
            assert returnType.isAssignableFrom(defaultValue.getClass())
              : "Return type "+returnType.getName()+" is not assignable from "+defaultValue.getClass().getName();
          }
        }

      }

    }// end for loop

    // Now, generate the basic Object methods, equals, toString, hashCode and Annotation.annotationType.
    final String shortName = cw.addImport(anno.annotationType());
    cw
      .createMethod("public final Class<? extends Annotation> annotationType()")
      .returnValue(shortName+".class")
    ;

    final MethodBuffer
      equals = cw.createMethod("public final boolean equals(Object other)"),
      hashCode = cw.createMethod("public final int hashCode()"),
      toString = cw.createMethod("public final String toString()")
    ;

    // Prepare the equals method
    if (valueMethods.isEmpty()) {
      equals.returnValue("other instanceof "+shortName);
    } else {
      equals
        .println("if (other == this) { return true; }")
        .println("if (!(other instanceof "+shortName+")) { return false; }")
        .println(shortName+" o = ("+shortName+")other;")
        .println("return ")
        .indent()
      ;
    }

    // Prepare the toString method
    toString
      .print("return \"@"+anno.annotationType().getCanonicalName())
    ;
    if (!valueMethods.isEmpty()) {
      toString.print("(");
    }
    toString.indent().println("\" +");

    // Prepare the hashCode method
    hashCode
      .println("int hash = 37;")
    ;

    for (final Method method : valueMethods.values()) {
      if (Annotation.class.isAssignableFrom(method.getReturnType())) {
        equals.println("this."+method.getName()+"().equals(o."+method.getName()+"()) && ");
      } else if (method.getReturnType().isArray()){
        final String arrays = equals.addImport(Arrays.class);
        equals.println(arrays+".equals(this."+method.getName()+"(), o."+method.getName()+"()) && ");
      } else {
        equals.println("this."+method.getName()+"() == o."+method.getName()+"() && ");
      }
      toString.println(method.getName()+"() + \", \" + ");
      final Class<?> type = method.getReturnType();
      if (type.isPrimitive()) {
        if (type == float.class || type == double.class) {
          hashCode.println("hash = hash + (int)(hash * ("+method.getName()+"()));");
        } else if (type == boolean.class){
          hashCode.println("hash += "+method.getName()+"() ? 1231 : 1237;");
        } else if (type == long.class){
          hashCode.println("long "+method.getName()+ " = " + method.getName()+"();");
          hashCode.println("hash = hash + (int)(("+ method.getName()+ " ^ ("+method.getName()+" >>> 32)));");
        } else if (type == int.class){
          hashCode.println("hash = hash ^ "+method.getName()+"();");
        } else {
          // char, byte, short
          hashCode.println("hash = hash ^ (int)"+method.getName()+"();");
        }
      } else {
        // TODO include proper support for array types
        hashCode.println("hash = hash ^ "+method.getName()+"().hashCode();");
      }
    }

    // Finish off the methods.

    // Equals method we will finish off with true, so each method can do
    // this.method() == o.method() &&, and we can just finish it off with a true
    if (!valueMethods.isEmpty()) {
      equals.println("true;");
    }

    hashCode.returnValue("hash");

    toString.print("\"");
    if (!valueMethods.isEmpty()) {
      toString.print(")");
    }
    toString.println("\";");
  }


  private static boolean typeMatches(final TreeLogger logger,
      final JClassType exists, final JClassType anno) throws UnableToCompleteException {
    final boolean doLog = logger.isLoggable(logLevel);
    if (doLog) {
      logger.log(logLevel, "Checking if annotation "+anno.getClass().getName()+" equals "+exists.getQualifiedSourceName());
      logger.log(logLevel, anno.getClass().getName()+": "+ anno.toString());
      logger.log(logLevel, exists.getQualifiedSourceName()+": "+ exists.toString());
    }
    try {
      final JMethod[] annoMethods = anno.getMethods();
      // Filter and map existing types.
      final Map<String, JMethod> existingMethods = new LinkedHashMap<String,JMethod>();
      for (final JMethod existingMethod : exists.getMethods()) {
        if (existingMethod.isPublic() && existingMethod.getEnclosingType() == exists) {
          existingMethods.put(existingMethod.getName(), existingMethod);
        }
      }
      // Now, our annotation methods must match our declared methods.
      for (final JMethod m : annoMethods) {
        final JMethod existing = existingMethods.get(m.getName());
        if (existing == null) {
          return false;
        }
        if (!m.getName().equals(existing.getName())) {
          if (doLog) {
            logger.log(logLevel, "Annotations don't match for " +anno.getName()+ "; "+
              m.getName() +" != "+existing.getName());
          }
          return false;
        }
        final JParameter[] existingParams = existing.getParameters();
        final JParameter[] annoParams = m.getParameters();
        if (existingParams.length != annoParams.length) {
          if (doLog) {
            logger.log(logLevel, "Annotations don't match for " +anno.getName()+ "; "+
              "parameters for "+ m.getName() +" have changed.");
          }
          return false;
        }
        for (int i = existingParams.length; i --> 0; ) {
          final JParameter existingParam = existingParams[i];
          final JParameter annoParam = annoParams[i];
          if (annoParam == null) {
            return false;
          }
          if (!existingParam.getType().getQualifiedSourceName()
            .equals(annoParam.getType().getQualifiedSourceName())) {
            if (doLog) {
              logger.log(logLevel, "Annotations don't match for " +
                anno.getName()+ "." + m.getName()+"(); "+
                "parameter "+ existingParam.getName() +" type has changed " +
                "from " +existingParam.getType().getQualifiedSourceName()+" to " +
                annoParam.getName()+".");
            }
            return false;
          }
        }
      }
      logger.log(logLevel, "Annotations match for " +
        anno.getName()+ "; reusing type.");
      return true;
    } catch (final Exception e) {
      logger.log(Type.ERROR, "Error encountering comparing annotation class to generated proxy;");
      logger.log(Type.ERROR, anno.getClass().getName() +" or "+exists.getName()+" is causing this error.", e);
      throw new UnableToCompleteException();
    }
  }
}
