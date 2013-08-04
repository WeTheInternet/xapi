package com.google.gwt.reflect.rebind.generators;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.source.read.JavaModel.IsNamedType;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.reflect.client.GwtReflect;


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
   * An instance of a GeneratedAnnotation is used to cache two important bits of data we need:
   * <br>
   * 1) Mappings from a given instance of an annotation to a static noArg constructor method
   * <br>
   * 2) The class type of the annotation proxy, so we can verify generated source matches
   * the known source of the generated type name we want to use.
   * <p>
   * We cache these objects statically during the UnifyAst phase of the gwt compile;
   * This allows us to detect when we have already generated a proxy type for a given
   * annotation class, and more importantly, to detect if it changes, and rename
   * our proxy class accordingly.
   *
   * @author james.nelson
   *
   */
  public static class GeneratedAnnotation {
    public GeneratedAnnotation(Annotation anno, String proxyName) {
      this.anno = anno;
      this.proxyName = proxyName;
    }
    final String proxyName;
    final Annotation anno;

    /**
     * The latest generated annotation provider
     */
    IsNamedType latest;

    /**
     * A map from a configured instance of an annotation to a public static factory method.
     * <p>
     * These methods will be generated ad-hoc, and reused instead of regenerated.
     * <p>
     * They will also be placed into new classes, so we don't accidentally
     * reuse a method that will have to load an unrelated class full of dependencies.
     */
    final Map<Annotation, IsNamedType> knownInstances = new HashMap<Annotation, IsNamedType>();

    /**
     * The actual source type of the annotation proxy.
     * <p>
     * Whenever an annotation class is first seen, we will have to generate the source
     * for its proxy class (once per gwt compile).  Then, when we ask gwt for the
     * PrintWriter to save this class, it may return null because a class w/ that name
     * already exists.  So, then we would try to look that type up in the TypeOracle,
     * and then use it's .toSource() method to check what we have generated.
     * <p>
     * If our new source is different, the annotation has changed across compiles
     * (common for super dev mode), so we must update our proxy class.
     */
    JClassType proxy;

    public String getAnnoName() {
      return anno.annotationType().getCanonicalName();
    }
    
    public String providerPackage() {
      return latest.getPackage();
    }

    public String providerClass() {
      return latest.getSimpleName();
    }
    
    public String providerMethod() {
      return latest.getName();
    }

    public String providerQualifiedName() {
      return latest.getQualifiedName();
    }

  }

  /**
   * Called when the {@link UnifyAstListener#destroy()} methods are called.
   *
   */
  public static void cleanup() {
    finished.clear();
  }


  /**
   * @throws UnableToCompleteException
   */
  static GeneratedAnnotation[] generateAnnotations(
    TreeLogger logger, SourceBuilder<?> out, GeneratorContext context, Annotation ... annotations
  ) throws UnableToCompleteException {

    final MethodBuffer initAnnos = out.getClassBuffer()
      .createMethod("private static void enhanceAnnotations" +
          "(final Class<?> cls)");

    if (annotations.length == 0)
      return new GeneratedAnnotation[0];

    initAnnos
      .setUseJsni(true)
      .addAnnotation(UnsafeNativeLong.class)
      .println("var map = cls.@java.lang.Class::annotations;")
      .println("if (map) return;")
      .println("map = cls.@java.lang.Class::annotations = {};");

    Map<GeneratedAnnotation, IsNamedType> results = new LinkedHashMap<GeneratedAnnotation, IsNamedType>();
    for (int i = 0, max = annotations.length; i < max; i++ ) {
      Annotation anno = annotations[i];
      GeneratedAnnotation gen = generateAnnotation(logger, context, anno);
      IsNamedType providerMethod;
      if (gen.knownInstances.containsKey(anno)) {
        // Reuse existing method
        providerMethod = gen.knownInstances.get(anno);
      } else {
        // Create new factory method.
        providerMethod = generateProvider(logger, out, anno, gen, context);
        initAnnos.addImport(providerMethod.getQualifiedName());
      }
      results.put(gen, providerMethod);
      gen.knownInstances.put(anno, providerMethod);
      String annoCls = anno.annotationType().getCanonicalName();
      out.getImports().addImport(annoCls+"Proxy");
      initAnnos
        .println("var key" +i + " = @"+annoCls+"::class.@java.lang.Class::getName()();")
        .println("map[key" +i + "] = function(){")
        .indent()
        .print("var anno = @")
        .print(providerMethod.getQualifiedName())
        .print("::")
        .print(providerMethod.getName())
        .println("()();")
        .println("map[key"+i+"] = function() { return anno; };")
        .println("map[key"+i+"].pub = true;")
        .println("return anno;")
        .outdent()
        .println("};")
        .println("map[key" +i + "].pub = true;");
    }// end for

    // We don't set the providers for a given annotation type until after our loop;
    // because annotation can contain other annotations, this method can be
    // called recursively, AND we are running on multiple annotations at once,
    // the only way to avoid overwriting the .latest field is to set it just before return;
    for (Entry<GeneratedAnnotation,IsNamedType> anno : results.entrySet()) {
      anno.getKey().latest = anno.getValue();
    }
    return results.keySet().toArray(new GeneratedAnnotation[results.size()]);
  }

  private static GeneratedAnnotation generateAnnotation(TreeLogger logger, GeneratorContext context,
    Annotation anno) throws UnableToCompleteException {
    final TypeOracle oracle = context.getTypeOracle();
    final boolean doLog = logger.isLoggable(logLevel);
    Class<? extends Annotation> annoType = anno.annotationType();

    GeneratedAnnotation gen = finished.get(annoType);
    if (gen == null) {

      // Step one is to generate a class implementing the annotation.
      // This annotation type will likely have been seen before,
      // but it may have changed (across gwt compiles in super dev mode).
      String proxyPkg = annoType.getPackage().getName();
      String proxyName = annoType.getName().replace(proxyPkg+".", "").replace('.', '_')+"Proxy";
      String proxyFQCN = (proxyPkg.length()==0 ? "" : proxyPkg + ".")+ proxyName;
      if (doLog)
        logger = logger.branch(logLevel, "Checking for existing "+proxyFQCN+" on classpath");
      JClassType exists = oracle.findType(proxyFQCN);
      boolean mustGenerate = exists == null;

      // Now, just because the class exists does not mean it is correct.
      if (doLog)
        logger = logger.branch(logLevel,
          mustGenerate ? "No existing type "+ proxyFQCN+" on classpath; " :
          "Checking if existing "+proxyFQCN+" matches "+anno);
      if (!mustGenerate) {
        // If a type exists, make sure the method patterns match.
        mustGenerate = typeMatches(logger, anno, exists);
      }

      if (mustGenerate) {
        // Create a proxy type matching our annotation

        // Step one is to get a printwriter from the gwt generator context
        PrintWriter pw = context.tryCreate(logger, proxyPkg, proxyName);
        int inc=-1;
        if (pw == null) {
          // null means name's taken.  Increment and try again.
          while (inc < 100) {
            if (doLog)
              logger.log(logLevel, "PrintWriter for "+proxyFQCN+" not available.  Incrementing name");
            String attempt = proxyName+"_"+(++inc);
            pw = context.tryCreate(logger, proxyPkg, attempt);
            if (pw != null) {
              proxyName = attempt;
              proxyFQCN = proxyFQCN+"_"+inc;
              break;
            }
          }
        }
        if (doLog)
          logger.log(logLevel, "Generating new annotation proxy "+proxyFQCN+".");

        // We've got our class name, now create the proxy class implementation
        gen = new GeneratedAnnotation(anno, proxyFQCN);
        SourceBuilder<GeneratedAnnotation> sw = new SourceBuilder<GeneratedAnnotation>(
          "public class "+proxyName
          ).setPackage(proxyPkg);
        sw.setPayload(gen);// allow the source builder to access GeneratedAnnotation
        // cache this type _before_ we start generating,
        // as it is possible / likely to recurse into the same type more than once.
        finished.put(annoType, gen);

        // create this annotation proxy, and any proxies needed in its fields.
        generateProxy(logger, anno, sw, proxyPkg, proxyName);

        // maybe log our generated contents
        String src = sw.toString();
        if (logger.isLoggable(Type.DEBUG))
          logger.log(Type.DEBUG, "Debug dump of generated annotation:\n"+src);

        // Actually save the file
        pw.println(src);
        context.commit(logger, pw);

        assert inc < 100 : "Generator context cannot create a printwriter; " +
          "check that your tmp / -out directory is not full.";
      } else {
        gen = new GeneratedAnnotation(anno, exists.getQualifiedSourceName());
        finished.put(annoType, gen);
      }
      gen.proxy = exists;
    }
    return gen;
  }


  public static GeneratedAnnotation generateAnnotationProvider(TreeLogger logger, SourceBuilder<?> out
    , Annotation anno, GeneratorContext context) throws UnableToCompleteException {
    GeneratedAnnotation gen = generateAnnotation(logger, context, anno);
    IsNamedType provider = generateProvider(logger, out, anno, gen, context);
    gen.latest = provider;
    if (logger.isLoggable(logLevel)) {
      logger.log(logLevel, "Generating annotation proxy "+gen.providerQualifiedName()+" for "+gen.getAnnoName());
    }
    return gen;
  }
  private static IsNamedType generateProvider(TreeLogger logger, SourceBuilder<?> out
    , Annotation anno, GeneratedAnnotation gen, GeneratorContext context) throws UnableToCompleteException {
    if (gen.knownInstances.containsKey(anno))
      return gen.knownInstances.get(anno);

    String method = anno.annotationType().getCanonicalName().replace('.', '_') +
      gen.knownInstances.size();
    // Cache the method name we're going to use, before we use it.
    IsNamedType type = new IsNamedType(method, out.getQualifiedName());
    gen.knownInstances.put(anno, type);

    String proxyName = anno.annotationType().getSimpleName()+"Proxy";
    out.getImports().addImport(anno.annotationType().getCanonicalName()+"Proxy");
    MethodBuffer mb = out.getClassBuffer()
      .createMethod("public static "+proxyName+" "+method+"()")
      .print("return new "+proxyName+"(");

      Method[] methods = ReflectionGeneratorUtil.getMethods(anno);
      int len = methods.length;
      for (int i = 0; i < len; i ++ ) {
        Method m = methods[i];
        Class<?> returnType = m.getReturnType();
        Object value;
        try {
          value = m.invoke(anno);
        } catch (Exception e) {
          logger.log(Type.ERROR, "Error generating annotation proxy provider method." +
            "\nCould not invoke "+m+" on "+anno, e);
          throw new UnableToCompleteException();
        }
        if (i > 0)
          mb.print(", ");
        if (Annotation.class.isAssignableFrom(returnType)) {
          Annotation asAnno = (Annotation)value;
          GeneratedAnnotation result = generateAnnotation(logger, context, asAnno);
          IsNamedType provider = generateProvider(logger, out, asAnno, result, context);
          mb.print(provider.getQualifiedName()+"."+provider.getName()+"()");
          mb.addImport(asAnno.annotationType().getCanonicalName()+"Proxy");
        } else if (returnType.isArray() && Annotation.class.isAssignableFrom(returnType.getComponentType())) {
          mb.println("new "+returnType.getComponentType().getCanonicalName()+"[]{");
          for (int ind = 0, length = GwtReflect.arrayLength(value); ind < length; ind++ ) {
            if (ind > 0)
              mb.print(", ");
            Annotation asAnno = (Annotation)GwtReflect.arrayGet(value, ind);
            GeneratedAnnotation result = generateAnnotation(logger, context, asAnno);
            IsNamedType provider = generateProvider(logger, out, asAnno, result, context);
            mb.addImport(asAnno.annotationType().getCanonicalName()+"Proxy");
            mb.print(provider.getQualifiedName()+"."+provider.getName()+"()");
          }
          mb.println("}");
        } else {
          // any other type, we can just generate raw source for now.
          mb.print(ReflectionGeneratorUtil.sourceName(value));
        }
      }

    mb.println(");");
    return type;
  }


  private static void generateProxy(TreeLogger logger, Annotation anno,
  SourceBuilder<GeneratedAnnotation> sw, String proxyPkg, String proxyName) {

    ClassBuffer cw = sw.getClassBuffer();
    cw.addImport(Annotation.class);
    cw.addInterface(anno.annotationType());
    MethodBuffer ctor = cw.createConstructor(Modifier.PUBLIC);
    // All public methods, include those from Object
    Method[] methods = anno.annotationType().getMethods();
    Object[] defaults = new Object[methods.length];
    for (int i = 0, m = methods.length; i < m; i++) {
      Method method = methods[i];
      String clsName = method.getDeclaringClass().getName();
      if (clsName.equals("java.lang.Object"))
        continue;
      if (clsName.equals("java.lang.annotation.Annotation")) {
        String name = method.getName();
        if (name.equals("equals")) {
          // TODO copy basic structure from AbstractAnnotation
        } else if (name.equals("hashCode")) {

        } else if (name.equals("toString")) {

        } else if (name.equals("annotationType")) {
          cw
          .createMethod("public final Class<? extends Annotation> annotationType()")
          .returnValue(anno.annotationType().getCanonicalName()+".class")
          ;
        }
      } else {
        // A method the client has declared
        Class<?> returnType = (Class<?>)method.getReturnType();
        String simpleName = method.getName();
        String paramName = ReflectionGeneratorUtil.toSourceName(method.getGenericReturnType())
          +" "+simpleName;
        Object defaultValue = defaults[i] = method.getDefaultValue();

        FieldBuffer field = cw.createField(returnType, method.getName())
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
  }


  private static boolean typeMatches(TreeLogger logger, Annotation anno, JClassType exists) throws UnableToCompleteException {
    final boolean doLog = logger.isLoggable(logLevel);
    if (doLog) {
      logger.log(logLevel, "Checking if annotation "+anno.getClass().getName()+" equals "+exists.getQualifiedSourceName());
      logger.log(logLevel, anno.getClass().getName()+": "+ anno.toString());
      logger.log(logLevel, exists.getQualifiedSourceName()+": "+ exists.toString());
    }
    try {
      Method[] annoMethods = anno.annotationType().getDeclaredMethods();
      // Filter and map existing types.
      Map<String, JMethod> existingMethods = new LinkedHashMap<String,JMethod>();
      for (JMethod existingMethod : exists.getMethods()) {
        if (existingMethod.isPublic() && existingMethod.getEnclosingType() == exists) {
          existingMethods.put(existingMethod.getName(), existingMethod);
        }
      }
      // Now, our annotation methods must match our declared methods.
      for (Method m : annoMethods) {
        JMethod existing = existingMethods.get(m.getName());
        if (!m.getName().equals(existing.getName())) {
          if (doLog) {
            logger.log(logLevel, "Annotations don't match for " +anno.annotationType().getName()+ "; "+
              m.getName() +" != "+existing.getName());
          }
          return false;
        }
        JParameter[] existingParams = existing.getParameters();
        Class<?>[] annoParams = m.getParameterTypes();
        if (existingParams.length != annoParams.length) {
          if (doLog) {
            logger.log(logLevel, "Annotations don't match for " +anno.annotationType().getName()+ "; "+
              "parameters for "+ m.getName() +" have changed.");
          }
          return false;
        }
        for (int i = existingParams.length; i --> 0; ) {
          JParameter existingParam = existingParams[i];
          Class<?> annoParam = annoParams[i];
          if (!existingParam.getType().getQualifiedSourceName()
            .equals(annoParam.getCanonicalName())) {
            if (doLog) {
              logger.log(logLevel, "Annotations don't match for " +
                anno.annotationType().getName()+ "." + m.getName()+"(); "+
                "parameter "+ existingParam.getName() +" type has changed " +
                "from " +existingParam.getType().getQualifiedSourceName()+" to " +
                annoParam.getCanonicalName()+".");
            }
            return false;
          }
        }
      }
      logger.log(logLevel, "Annotations match for " +
        anno.annotationType().getName()+ "; reusing type.");
      return true;
    } catch (Exception e) {
      logger.log(Type.ERROR, "Error encountering comparing annotation class to generated proxy;");
      logger.log(Type.ERROR, anno.getClass().getName() +" or "+exists.getName()+" is causing this error.", e);
      throw new UnableToCompleteException();
    }
  }
}
