package com.google.gwt.reflect.rebind.generators;

import static com.google.gwt.reflect.rebind.ReflectionUtilJava.qualifiedName;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.model.GeneratedAnnotation;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.reflect.shared.JsMemberPool;
import com.google.gwt.reflect.shared.ReflectUtil;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This generator is used to create class instances of annotations.  Since GWT does not have any sort
 * of Proxy object support, we just create a class which implements the annotation interface.
 * <p>
 * Because we control the structure and nature of these annotations, we are able to make certain optimizations,
 * for example, all arrays can be generated via {@link ConstPoolGenerator}, so that two different annotations
 * which share an array of the same values will actually reference a singleton constant.
 * <p>
 * This same optimization is made for annotations themselves; every possible configuration of an annotation
 * will only ever be instantiated once; different annotations with the same value will actually reference
 * the same instance.  This allows us to greatly speed up the .equals() and hashCode() methods, as
 * referential equality will suffice.  This is not only faster, it's also significantly less runtime code.
 * <p>
 * If, for any reason, you wish to have interopability with annotation instances produced by any means
 * other than this generator, you may want to set the configuration property gwt.reflect.optimize.annotations to false.
 * This will cause this generator to implement far more verbose hashCode(), equals() and toString() methods,
 * which will work correctly with annotation proxy instances generated elsewhere.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class GwtAnnotationGenerator {

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

  private static final Type logLevel = Type.TRACE;

  /**
   * Called when the {@link UnifyAstListener#destroy()} methods are called.
   */
  public static void cleanup() {
    finished.clear();
  }

  /**
   * Search for an existing generated annotation implementation factory method.  Will return null if there
   * is no existing provider method for the exact annotation specified.
   *
   * @param logger -> A {@link TreeLogger} in case errors need to be logged
   * @param anno -> The {@link Annotation} to search for
   * @param oracle -> A {@link TypeOracle} so we can lookup the proxy type to be sure it is not stale
   * @return -> A {@link IsNamedType} for the provider class and method name, or null if no provider exists.
   *
   * @throws UnableToCompleteException -> If anything went wrong.
   */
  public static IsNamedType findExisting(final TreeLogger logger, final Annotation anno, final TypeOracle oracle) throws UnableToCompleteException {
    final GeneratedAnnotation gen = finished.get(anno.annotationType());
    if (gen != null) {
      final JClassType existing = oracle.findType(gen.getProxyName());
      if (typeMatches(logger, existing, anno)) {
        return gen.getProviderMethod(anno);
      }
    }
    return null;
  }

  /**
   * Given a supplied {@link Annotation}, ensures that a proxy class is generated, and a supplier method
   * is created to act as a factory for any annotation with the exact configuration as the one supplied.
   *
   * @param logger -> A {@link TreeLogger}, for logging. :-)
   * @param out -> A {@link SourceBuilder} where we should put the annotation's factory method
   * @param anno -> The {@link Annotation} to generate a proxy and provider for.
   * @param ast
   * @return
   * @throws UnableToCompleteException
   */
  public static IsNamedType generateAnnotationProvider(final TreeLogger logger, final SourceBuilder<?> out
    , final Annotation anno, final GeneratorContext ctx) throws UnableToCompleteException {
    final GeneratedAnnotation gen = generateAnnotationProxy(logger, anno, ctx);
    final IsNamedType provider = generateProviderMethod(logger, out, anno, gen, ctx);
    if (logger.isLoggable(logLevel)) {
      logger.log(logLevel, "Generating annotation proxy "+provider.getQualifiedMemberName()+" for "+gen.getAnnoName());
    }
    return provider;
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
   * @param ctx -> A {@link GeneratorContext}, so we can generate new source files
   * @return -> A {@link GeneratedAnnotation} instance used to map instances of the given annotation
   *
   * @throws UnableToCompleteException -> If a fatal error occurs
   */
  protected static GeneratedAnnotation generateAnnotationProxy(TreeLogger logger,
    final Annotation anno, final GeneratorContext ctx) throws UnableToCompleteException {

    final boolean doLog = logger.isLoggable(logLevel);
    final Class<? extends Annotation> annoType = anno.annotationType();
    final TypeOracle oracle = ctx.getTypeOracle();

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
        mustGenerate = typeMatches(logger, exists, anno);
      }

      if (mustGenerate) {
        // Create a proxy type matching our annotation

        // Step one is to get a printwriter from the gwt generator context
        PrintWriter pw = ctx.tryCreate(logger, proxyPkg, proxyName);
        int inc=-1;
        if (pw == null) {
          // null means name's taken.  Increment and try again.
          while (inc < 100) {
            if (doLog) {
              logger.log(logLevel, "PrintWriter for "+proxyFQCN+" not available.  Incrementing name");
            }
            final String attempt = proxyName+"_"+(++inc);
            pw = ctx.tryCreate(logger, proxyPkg, attempt);
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
        gen = new GeneratedAnnotation(anno, proxyPkg, proxyName);
        final SourceBuilder<GeneratedAnnotation> sw = new SourceBuilder<GeneratedAnnotation>(
          "public class "+proxyName
          ).setPackage(proxyPkg);
        sw.setPayload(gen);// allow the source builder to access GeneratedAnnotation
        // cache this type _before_ we start generating,
        // as it is possible to recurse into the same type more than once
        // when generating annotations that have other annotations as members.
        finished.put(annoType, gen);

        // create this annotation proxy, and any proxies needed in its fields.
        final boolean simplifiedMethods = canSimplifyMethods(ctx.getPropertyOracle());
        generateProxy(logger, anno, sw.getClassBuffer(), simplifiedMethods);

        // maybe log our generated contents
        final String src = sw.toString();
        if (logger.isLoggable(Type.DEBUG)) {
          logger.log(Type.DEBUG, "Debug dump of generated annotation:\n"+src);
        }

        // Actually save the file
        pw.println(src);
        ctx.commit(logger, pw);

        assert inc < 100 : "Generator context cannot create a printwriter; " +
          "check that your tmp / -out directory is not full.";
      } else {
        gen = new GeneratedAnnotation(anno, exists.getPackage().getName(), exists.getSimpleSourceName());
        finished.put(annoType, gen);
      }
      gen.setProxyClassType(exists);
    }
    return gen;
  }

  /**
   * Print a method to fill in a {@link JsMemberPool} with the annotations being retained for
   * the supplied type.
   *
   * @param logger -> The {@link TreeLogger}
   * @param sourceBuilder -> The {@link SourceBuilder} used to print source into
   * @param injectionType -> The {@link JClassType} that was are generating annotation proxies for
   * @param ctx -> A {@link GeneratorContext} so we can generate new source files
   * @param annotations -> The array of {@link Annotation}s to generate
   * @return -> An array of {@link GeneratedAnnotation}s produced from running this generator
   *
   * @throws UnableToCompleteException if any fatal error occurs.
   */
  static GeneratedAnnotation[] printAnnotationEnhancer(
    final TreeLogger logger, final SourceBuilder<?> sourceBuilder,
    final JClassType injectionType, final GeneratorContext ctx,
    final Annotation ... annotations
  ) throws UnableToCompleteException {

    // We want to print a method to enhance a JsMemberPool with annotation instances
    final ClassBuffer out = sourceBuilder.getClassBuffer();
    final String memberPool = out.addImport(JsMemberPool.class);
    final MethodBuffer initAnnos = out
      .createMethod("private static void enhanceAnnotations("+memberPool+" pool)");

    if (annotations.length == 0){
      // Short circuit for annotationless types
      return new GeneratedAnnotation[0];
    }

    final ConstPoolGenerator constGenerator = ConstPoolGenerator.getGenerator();

    final List<GeneratedAnnotation> results = new ArrayList<GeneratedAnnotation>();
    for (int i = 0, max = annotations.length; i < max; i++ ) {

      final Annotation annotation = annotations[i];
      // Generate a proxy class
      final GeneratedAnnotation generated = generateAnnotationProxy(logger, annotation, ctx);
      results.add(generated);
      final IsNamedType result = constGenerator.rememberAnnotation(logger, ctx, annotation);
      final String getAnno = out.addImportStatic(result.getQualifiedMemberName());
      final boolean isDeclared = injectionType.getAnnotation(annotation.annotationType()) != null;
      // Record the annotation instance in the member pool
      initAnnos.println("pool.addAnnotation("+getAnno+", "+isDeclared + ");");
    }// end for

    return results.toArray(new GeneratedAnnotation[results.size()]);
  }

  /**
   * @param props -> The {@link PropertyOracle} to query for the "gwt.reflect.optimize.annotations" property
   * @return -> true unless the configuration property is explicitly set to false
   */
  private static boolean canSimplifyMethods(final PropertyOracle props) {
    ConfigurationProperty prop;
    try {
      prop = props.getConfigurationProperty("gwt.reflect.optimize.annotations");
    } catch (final BadPropertyValueException e) {
      return true;
    }
    if (prop == null) {
      return true;
    }
    final List<String> values = prop.getValues();
    if (values == null || values.isEmpty()) {
      return true;
    }
    return !"false".equals(values.get(0));

  }

  /**
   * Generates a provider method which will invoke new MyAnnotationProxy(...args...).  This method will
   * be public and static, and attached to the supplied SourceBuilder.  A reference to the class and method
   * will be stored so that if future code needs to create the same annotation, the generated method here
   * will be reused, instead of recreated.
   *
   * @param logger -> The {@link TreeLogger} so we can log meaningful information
   * @param sourceBuilder -> The {@link SourceBuilder} where we will add the factory method
   * @param anno -> The {@link Annotation} to generate
   * @param gen -> The {@link GeneratedAnnotation} where we will store a reference to the generated method
   * @param ctx -> The {@link GeneratorContext} in case we need to create new files.
   * @return -> An {@link IsNamedType} pointing to the class and method name where the annotation factory exists
   *
   * @throws UnableToCompleteException -> If a fatal error occurs.
   */
  private static IsNamedType generateProviderMethod(final TreeLogger logger, final SourceBuilder<?> sourceBuilder
    , final Annotation anno, final GeneratedAnnotation gen, final GeneratorContext ctx) throws UnableToCompleteException {

    // First, short circuit if a factory method exist
    if (gen.hasProviderMethod(anno)) {
      return gen.getProviderMethod(anno);
    }

    final String method = gen.getMethodName(anno);
    // Cache the method name we're going to use
    final IsNamedType type = new IsNamedType(sourceBuilder.getQualifiedName(), method);
    gen.addProviderMethod(anno, type);

    // Create the factory method and print "return new ProxyName("
    final String proxyName = gen.getProxySimpleName();
    final MethodBuffer mb = sourceBuilder.getClassBuffer()
      .createMethod("public static "+proxyName+" "+method+"()")
      .print("return new "+sourceBuilder.getImports().addImport(gen.getProxyName())+"(");

    // Now, loop through the methods, extract the constants from the annotation, and print them.
    final Method[] methods = ReflectionUtilJava.getMethods(anno);
    final ConstPoolGenerator constGenerator = ConstPoolGenerator.getGenerator();
    final int len = methods.length;
    for (int i = 0; i < len; i ++ ) {
      // Extract the value of the annotation constant
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

      // Now, lets print the value
      if (i > 0) {
        mb.print(", ");
      }

      if (Annotation.class.isAssignableFrom(returnType)) {
        // Annotation values may need to generate a proxy and a provider method themselves.
        final Annotation asAnno = (Annotation)value;
        final IsNamedType result = constGenerator.rememberAnnotation(logger, ctx, asAnno);
        mb.print(mb.addImportStatic(result.getQualifiedMemberName()));
      } else if (returnType.isArray()){
        // For arrays, we will let the constGenerator print a reference to the array constant
        if (returnType.getComponentType().isPrimitive()) {
          constGenerator.arrayOfPrimitives(logger, mb, value);
        } else {
          constGenerator.arrayOfObjects(logger, ctx, mb, (Object[])value);
        }
      } else if (value instanceof Class){
        final String result = constGenerator.rememberClass((Class<?>)value);
        mb.print(mb.addImportStatic(result));
      } else if (value instanceof Long){
        // Longs must, unfortunately, be generated as ConstPool references in order for js == comparisons
        // to function correctly
        final String result = constGenerator.rememberLong(logger, ((Long)value).longValue());
        mb.print(mb.addImportStatic(result));
      } else {
        // Enums and primitives, we will simply generate a reference to the fields,
        // as they will all be safe to perform == comparisons in javascript
        mb.print(ReflectionUtilJava.sourceName(value));
      }
    }

    mb.println(");");

    return type;
  }

  /**
   * Generates an annotation proxy class.  This class will be immutable and take constructor arguments
   * for each field, and will generate a class that is optimized based on how we generate instances of
   * the proxy.  Because all annotations are stored as constants, we will be able to perform reference
   * equality instead of an in-depth equality check.
   *
   * @param logger -> A {@link TreeLogger} to print meaningful logs
   * @param anno -> The {@link Annotation} to generate a proxy for
   * @param cw -> The {@link ClassBuffer} in which to print the proxy annotation
   * @param simplifiedMethods -> Whether we can generate simplified methods that can make assumptions
   * about how all annotations are created.  If all annotations are sourced from this generator, it
   * is perfectly safe to perform direct referential equality operations ( use == instead of deep equals() ).
   */
  private static void generateProxy(final TreeLogger logger, final Annotation anno,
      final ClassBuffer cw, final boolean simplifiedMethods) {

    // Inherit the anotation interface
    cw.addInterface(anno.annotationType());
    cw.addInterface(Annotation.class);
    final String annotation = cw.addImport(Annotation.class);
    // All methods from the annotation type only
    final Method[] methods = ReflectionUtilJava.getMethods(anno);
    // Prepare the constructor
    final MethodBuffer ctor = cw.createConstructor(Modifier.PUBLIC);
    // Loop through the methods and add each one to the constructor
    for (int i = 0, m = methods.length; i < m; i++) {
      final Method method = methods[i];
      final String simpleName = method.getName();
      // We simplify the returnType so we have more readable code.
      final String returnType = cw.addImport(ReflectionUtilJava.toSourceName(method.getGenericReturnType(), cw));
      final String paramName = returnType + " " + simpleName;

      // Add a private final field, without adding any get/set prefixes
      final FieldBuffer field = cw.createField(returnType, method.getName())
        .setExactName(true)
        .makePrivate()
        .makeFinal()
        ;
      if (method.getReturnType().isArray()) {
        // Arrays must be cloned on get() to avoid mutation
        // The clone method here only exists in our super-sourced copy of Array
        final String clone = cw.addImportStatic("java.lang.reflect.Array.clone");
        cw.createMethod("public final "+returnType+" "+field.getName()+"()")
          .returnValue(clone+"("+field.getName()+")");
      } else {
        // Plain beans can be returned safely
        field.addGetter(Modifier.PUBLIC).makeFinal();
      }

      // Add the field to the constructor as well
      ctor.addParameters(paramName);
      // And print the assignment
      ctor.println("this."+simpleName+" = "+simpleName+";");

    }// end for loop

    // Now, generate the Annotation.annotationType() method
    final String shortName = cw.addImport(anno.annotationType());
    cw
      .createMethod("public final Class<? extends " + annotation + "> annotationType()")
      .returnValue(shortName+".class")
    ;

    // Finally, the basic object methods
    if (!simplifiedMethods) {
      // When we simplify, object identity hashCode is good enough (and much faster)
      printHashCodeMethod(logger, cw, methods);
    }
    printEqualsMethod(logger, cw, shortName, methods, simplifiedMethods);
    printToStringMethod(logger, cw, anno, methods, simplifiedMethods);

  }


  /**
   * Generates a toString method for an annotation.  If in optimized mode, this will defer to the
   * {@link ReflectUtil#nativeToString(Object)} method, which just does basic inspection of the javascript
   * object (an ok message with minimal code size).  When in non-optimized mode, the generated method will
   * produce a String representation fit to copy-paste directly into java source code.
   *
   * @param logger -> The {@link TreeLogger} for logging important messages
   * @param cw -> The {@link ClassBuffer} where we will be generating the method
   * @param anno -> The {@link Annotation} we are generating
   * @param methods -> An array of {@link Method}s to generate within the toString() method
   * @param simplifiedMethods -> If true, generate an optimized format (minimal code)
   */
  private static void printToStringMethod(final TreeLogger logger, final ClassBuffer cw, final Annotation anno, final Method[] methods, final boolean simplifiedMethods) {

    // Prepare the toString method
    final MethodBuffer toString = cw
      .createMethod("public final String toString()");
    toString
      .print("return \"@"+anno.annotationType().getCanonicalName())
    ;
    if (simplifiedMethods) {
      final String nativeToString = cw.addImportStatic(ReflectUtil.class, "nativeToString");
      toString.println("(\" + ").indentln(nativeToString +"(this) + \")\";");
      return;
    }
    if (methods.length > 0) {
      // Open the annotation definition
      toString.print("(");
    }
    // We will end each line with a +, for consistency
    toString.indent().println("\" +");

    for (int i = 0; i < methods.length; i ++ ) {
      final Method method = methods[i];
      toString.print("\""+method.getName()+" = \" + ");
      final Class<?> type = method.getReturnType();
      if (type.isArray()) {
        // For arrays, we will defer to a set of Array.join methods in our supersource java.lang.reflect.Array
        final String join = toString.addImportStatic("java.lang.reflect.Array.join");
        toString.print("\"{\" + "+join+"("+method.getName()+") + \"}");
      } else if (type == Class.class){
        // For classes, we want java-compatible canonical names
        toString.print(method.getName()+".getCanonicalName() + \".class");
      } else if (Enum.class.isAssignableFrom(type)){
        // Enums, we want fully qualified references
        toString.print(method.getName()+".getDeclaringClass().getCanonicalName() + \".\" + "+method.getName()+".name() + \"");
      } else if (type == String.class){
        // Strings we want to do runtime escaping
        final String escape = toString.addImportStatic(GwtReflect.class, "escape");
        toString.print("\"\\\"\" + "+escape+"("+method.getName()+") + \"\\\"");
      } else if (type == char.class){
        // char we want to surround with single quotes
        toString.print("'"+method.getName()+"' + \"");
      } else if (type == long.class){
        // Longs we want the trailng L
        toString.print(method.getName()+" + \"L");
      } else {
        // Other primitives we will just print as-is
        toString.print(method.getName()+" + \"");
      }
      // Tack on a , if we are not the last entry
      if (i < methods.length - 1) {
        toString.print(", ");
      }
      // Finish the line with a close quotation mark and a +
      toString.println("\" + ");
    }

    // Close ", ) and ;
    toString.print("\"");
    if (methods.length > 0) {
      toString.print(")");
    }
    toString.println("\";");
  }

  /**
   * Prints a hashCode method; only invoked if we are not in optimization mode.  When we are in optimize mode,
   * we will simply rely on the object-identity hashCode, as there will only ever be one instance for every
   * possible configuration of any annotation.
   *
   * @param logger -> A {@link TreeLogger}, for logging.
   * @param cw -> The {@link ClassBuffer} to add the .hashCode() method to
   * @param methods -> An array of {@link Method}s to generate hashCode implementations for.
   */
  private static void printHashCodeMethod(final TreeLogger logger, final ClassBuffer cw,
      final Method[] methods) {

    final MethodBuffer hashCode = cw.createMethod("public final int hashCode()");

    // Prepare the hashCode method
    hashCode
      .println("int hash = 37;")
    ;

    for (final Method method : methods) {
      final Class<?> type = method.getReturnType();
      if (type.isPrimitive()) {
        // For primitives, lets just do something quick 'n dirty
        if (type == float.class || type == double.class) {
          hashCode.println("hash = hash + (int)(hash * ("+method.getName()+"));");
        } else if (type == boolean.class){
          hashCode.println("hash += "+method.getName()+" ? 1231 : 1237;");
        } else if (type == long.class){
          hashCode.println("hash = hash + (int)(("+ method.getName()+ " ^ ("+method.getName()+" >>> 32)));");
        } else if (type == int.class){
          hashCode.println("hash = hash ^ "+method.getName()+";");
        } else {
          // char, byte, short
          hashCode.println("hash = hash ^ (int)"+method.getName()+";");
        }
      } else {
        // It's okay to hash any other objects here, even arrays, as we can be sure our arrays will
        // be singletons that are reused across different instances.
        hashCode.println("hash = hash ^ "+method.getName()+".hashCode();");
      }
    }

    hashCode.returnValue("hash");
  }

  /**
   * Creates an equals() method.  When running in simplified mode, we do a pure == reference equality
   * check, as we can be sure that every instance of a given configuration of an annotation will resolve
   * to the same singleton instance.  When we are not running in simplified mode, this will generate a
   * comprehensive equality method that will use Arrays.equals() for arrays.
   *
   * @param logger -> The {@link TreeLogger} for logging messages
   * @param cw -> The {@link ClassBuffer} in which to generate the equals() method
   * @param shortName -> The simple name of the annotation type
   * @param methods -> The array of {@link Method}s to support in the equality method
   * @param simplifiedMethods -> true if the method should use only reference == equality.
   */
  private static void printEqualsMethod(final TreeLogger logger, final ClassBuffer cw,
      final String shortName, final Method[] methods, final boolean simplifiedMethods) {

    final MethodBuffer equals = cw.createMethod("public final boolean equals(Object other)");

    // Prepare the equals method
    if (methods.length == 0) {
      equals.returnValue("other instanceof "+shortName);
      return;
    }
    if (simplifiedMethods){
      equals.returnValue("other == this");
      return;
    }
    equals
      .println("if (other == this) { return true; }")
      .println("if (!(other instanceof "+shortName+")) { return false; }")
      .println(shortName+" o = ("+shortName+")other;")
      .println("return ")
      .indent()
    ;

    for (int i = 0; i < methods.length; i ++ ) {
      if (i > 0) {
        equals.print(" && ");
      }
      final Method method = methods[i];
      final Class<?> type = method.getReturnType();
      if (type.isArray()){
        final String arrays = equals.addImport(Arrays.class);
        equals.println(arrays+".equals(this."+method.getName()+"(), o."+method.getName()+"())");
      } else if (type.isPrimitive()){
        equals.println("this."+method.getName()+"() == o."+method.getName()+"()");
      } else {
        equals.println("this."+method.getName()+"().equals(o."+method.getName()+"())");
      }
    }
    equals.println(";");
  }

  /**
   * Computes the simple name of the proxy type, which is:
   * <p>
   * canonicalName.replace(pkg+".", "").replace('.', '_')+"Proxy";
   *
   * @param canonicalName -> The fully qualified name of the original annotation
   * @param pkg -> The package name of the original annotation
   * @return OriginalAnnotationNameProxy
   */
  private static String toProxyName(final String canonicalName, final String pkg) {
    return canonicalName.replace(pkg+".", "").replace('.', '_')+"Proxy";
  }

  /**
   * Check if a given annotation type and the generated proxy class are still in sync.
   * Note that, due to https://code.google.com/p/google-web-toolkit/issues/detail?id=9174 ,
   * if the structure of the annotation changes, we will currently be unable to actually
   * pick up those changes.  Though it is possible for us to use JJS AST nodes to detect
   * when the annotation changes, we will not be able to actually load those changes until
   * we create JAnnotationType and JAnnotationInstance types to access this information
   * from the supplied source files run through the internal eclipse compiler, rather than
   * relying on the presence of .class files from running javac.
   * <p>
   * This method will still work for production compiles, as the jvm will be loaded fresh
   * each time, and we will be able to avoid stale values in the unit cache.  However,
   * super dev mode will still suffer from the fact that the class loader will not unload
   * the original structure of an annotation (thus making it impossible to pick up changes).
   *
   * @param logger -> The {@link TreeLogger} for logging
   * @param fromCache -> The {@link JClassType} of the proxy, if it exists
   * @param fromSource -> The {@link Annotation} instance that sourced the proxy class
   * @return -> true if the proxy class can be reused
   *
   * @throws UnableToCompleteException if any fatal error occurs
   */
  private static boolean typeMatches(final TreeLogger logger,
      final JClassType fromCache, final Annotation fromSource) throws UnableToCompleteException {

    final boolean doLog = logger.isLoggable(logLevel);
    if (doLog) {
      logger.log(logLevel, "Checking if annotation "+fromSource.annotationType().getName()+" equals "+fromCache.getQualifiedSourceName());
      logger.log(logLevel, fromSource.annotationType().getName()+": "+ fromSource.toString());
      logger.log(logLevel, fromCache.getQualifiedSourceName()+": "+ fromCache.toString());
    }

    // Use a try block here so we can log some helpful information if anything goes awry
    try {
      final Method[] annoMethods = fromSource.annotationType().getMethods();
      // Filter and map existing types.
      final Map<String, JMethod> existingMethods = new LinkedHashMap<String,JMethod>();
      for (final JMethod existingMethod : fromCache.getMethods()) {
        if (existingMethod.isPublic() && existingMethod.getEnclosingType() == fromCache) {
          existingMethods.put(existingMethod.getName(), existingMethod);
        }
      }
      // Now, our annotation methods must match our declared methods.
      for (final Method m : annoMethods) {
        final JMethod existing = existingMethods.get(m.getName());
        if (existing == null) {
          return false;
        }
        // Check method names
        if (!m.getName().equals(existing.getName())) {
          if (doLog) {
            logger.log(logLevel, "Annotations don't match for " +fromSource.annotationType().getName()+ "; "+
              m.getName() +" != "+existing.getName());
          }
          return false;
        }

        // Check parameters; first, check the size of parameters (though, really, this should always be 0)
        final JParameter[] existingParams = existing.getParameters();
        final Class<?>[] annoParams = m.getParameterTypes();
        if (existingParams.length != annoParams.length) {
          if (doLog) {
            logger.log(logLevel, "Annotations don't match for " +fromSource.annotationType().getName()+ "; "+
              "parameters for "+ m.getName() +" have changed.");
          }
          return false;
        }
        // Now check the parameter types
        for (int i = existingParams.length; i --> 0; ) {
          final JParameter existingParam = existingParams[i];
          final Class<?> annoParam = annoParams[i];
          if (annoParam == null) {
            return false;
          }
          if (!existingParam.getType().getQualifiedSourceName()
            .equals(annoParam.getCanonicalName())) {
            if (doLog) {
              logger.log(logLevel, "Annotations don't match for " +
                fromSource.annotationType().getName()+ "." + m.getName()+"(); "+
                "parameter "+ existingParam.getName() +" type has changed " +
                "from " +existingParam.getType().getQualifiedSourceName()+" to " +
                annoParam.getName()+".");
            }
            return false;
          }
        }
      }

      // Optionally print some debug information
      if (doLog) {
        logger.log(logLevel, "Annotations proxy match for " +
            fromSource.annotationType().getName()+ "; reusing type.");
      }

      // If we get here, then the proxy type is correct
      return true;
    } catch (final Exception e) {
      // If a failure occurs, let the user know which type caused an exception during comparison
      logger.log(Type.ERROR, "Error encountering comparing annotation class to generated proxy;");
      logger.log(Type.ERROR, fromSource.getClass().getName() +" or "+fromCache.getName()+" is causing this error.", e);
      throw new UnableToCompleteException();
    }
  }
}
