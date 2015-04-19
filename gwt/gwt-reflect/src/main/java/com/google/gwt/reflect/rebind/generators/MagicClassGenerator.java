package com.google.gwt.reflect.rebind.generators;

import static com.google.gwt.reflect.client.ConstPool.ArrayConsts.EMPTY_ANNOTATIONS;
import static com.google.gwt.reflect.client.strategy.ReflectionStrategy.ALL_ANNOTATIONS;
import static com.google.gwt.reflect.client.strategy.ReflectionStrategy.COMPILE;
import static com.google.gwt.reflect.client.strategy.ReflectionStrategy.INHERITED;
import static com.google.gwt.reflect.client.strategy.ReflectionStrategy.NONE;
import static com.google.gwt.reflect.client.strategy.ReflectionStrategy.RUNTIME;
import static com.google.gwt.reflect.rebind.ReflectionManifest.getReflectionManifest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.reflect.client.strategy.NewInstanceStrategy;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.client.strategy.UseGwtCreate;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUnit;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.injectors.DeclaredConstructorInjector;
import com.google.gwt.reflect.rebind.injectors.DeclaredFieldInjector;
import com.google.gwt.reflect.rebind.injectors.DeclaredMethodInjector;
import com.google.gwt.reflect.shared.ClassMap;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.reflect.shared.JsMemberPool;
import com.google.gwt.reflect.shared.ReflectUtil;
import com.google.gwt.thirdparty.xapi.dev.source.ClassBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.MethodBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.SourceBuilder;
import com.google.gwt.thirdparty.xapi.source.read.SourceUtil;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ReflectionStrategy
public class MagicClassGenerator {

  public static interface MemberFilter <T extends HasAnnotations> {
    boolean keepMember(T type, boolean isDeclared, boolean isPublic, ReflectionStrategy strategy);
    boolean keepAnnotation(T type, Annotation anno, ReflectionStrategy strategy);
    @SuppressWarnings("rawtypes")
    final MemberFilter DEFAULT_FILTER = new MemberFilter() {
      @Override
      public boolean keepAnnotation(final HasAnnotations type, final Annotation anno, final ReflectionStrategy strategy) {
        final Retention retention = anno.annotationType().getAnnotation(Retention.class);
        if (retention == null) {
          return (strategy.annotationRetention() & COMPILE) == COMPILE;
        }
        switch (retention.value()) {
          case RUNTIME:
            return (strategy.annotationRetention() & RUNTIME) == RUNTIME;
          case CLASS:
            return (strategy.annotationRetention() & COMPILE) == COMPILE;
          case SOURCE:
            // gwt doesn't have the source annos anyway
          default:
            return false;
        }
      };
      @Override
      public boolean keepMember(final HasAnnotations type, final boolean isDeclared, final boolean isPublic, final ReflectionStrategy strategy) {
        return isDeclared ? true : strategy.magicSupertypes() ? isPublic : true;
      };
    };

  }

  private final ConstructorGenerator constructorGenerator = newConstructorGenerator();
  private final MethodGenerator methodGenerator = newMethodGenerator();
  private final FieldGenerator fieldGenerator = newFieldGenerator();

  private final HashMap<String, NewInstanceStrategy> newInstanceStrategies = new HashMap<String, NewInstanceStrategy>();
  private static final ThreadLocal<MagicClassGenerator> GENERATOR = new ThreadLocal<MagicClassGenerator>() {
    @Override
    protected MagicClassGenerator initialValue() {
      return new MagicClassGenerator();
    };
  };
  public static void cleanup() {
    GENERATOR.remove();
  }

  protected ConstructorGenerator newConstructorGenerator() {
    return new DeclaredConstructorInjector();
  }

  protected MethodGenerator newMethodGenerator() {
    return new DeclaredMethodInjector();
  }

  protected FieldGenerator newFieldGenerator() {
    return new DeclaredFieldInjector();
  }

  private boolean logOnce = true;
  private final HashSet<String> done = new HashSet<String>();

  public static String generate(final TreeLogger logger, final ReflectionGeneratorContext reflectionCtx, final JClassType type)
      throws UnableToCompleteException {
    return GENERATOR.get().execImpl(logger, reflectionCtx, type);
  }
  public String execImpl(final TreeLogger logger, final ReflectionGeneratorContext reflectionCtx, final JClassType type)
    throws UnableToCompleteException {
    final StandardGeneratorContext context = reflectionCtx.getGeneratorContext();
    final String simpleName = SourceUtil.toSourceName(type.getSimpleSourceName());
    String generatedName = ReflectionUtilJava.generatedMagicClassName(simpleName);
    final String packageName = type.getPackage().getName();
    final String pkg = packageName.length() == 0 ? "" : packageName+".";

    if (!done.add(type.getQualifiedSourceName())) {
      return pkg+generatedName;
    }

    ReflectionManifest manifest;

    PrintWriter printWriter = context.tryCreate(logger, packageName, generatedName);
    int unique = 0;
    String next = generatedName;
    while(printWriter == null){
      next = generatedName+"_"+unique++;
      printWriter = context.tryCreate(logger, packageName, next);
    }
    generatedName = next;


    final UnifyAstView ast = reflectionCtx.getAst();
    final JClassType targetType = type;
    final String clsToEnhance = type.getQualifiedSourceName();
    final JClassType injectionType = getInjectionType(logger, targetType, context);

    manifest = getReflectionManifest(logger, injectionType.getQualifiedSourceName(), context);

    final ReflectionStrategy strategy = manifest.getStrategy();
    final boolean keepHierarchy = strategy.magicSupertypes();
    final boolean keepCodesource = strategy.keepCodeSource();
    final boolean keepPackageName = strategy.keepPackage();
    final boolean keepAnnos = strategy.annotationRetention() > 0 ||
        injectionType.getQualifiedSourceName().equals(Package.class.getName()+".PackageInfoProxy");

    if (logger.isLoggable(Type.TRACE)) {
      logger.log(Type.TRACE,"Writing magic class instance for " + type.getQualifiedSourceName() + " -> " +
          injectionType.getQualifiedSourceName());
    }

    if ((strategy.debug() & ReflectionStrategy.SOURCE) > 0) {
      logger.log(Type.INFO, "Source Dump For " +clsToEnhance+":");
      logger.log(Type.INFO, type.toString());
    }

    final String typeName = injectionType.isPrivate() ? "?" : simpleName;
    final SourceBuilder<Object> classBuilder = new SourceBuilder<Object>(
        "public class "+generatedName +" extends ClassMap"
            + (injectionType.isPrivate()? "" : "<"+simpleName+">"))
      .setPackage(packageName);
    if (!injectionType.isPrivate()){
      classBuilder.getImports().addImports(clsToEnhance);
    }
    classBuilder.getImports()
      .addImports(
          ClassMap.class, JavaScriptObject.class, UnsafeNativeLong.class,
          ReflectUtil.class, Annotation.class,
          Constructor.class,Method.class,Field.class,CodeSource.class)
      .addStatics(GwtReflect.class.getName()+".constId")
     ;


    final ClassBuffer cls = classBuilder.getClassBuffer();

    cls.createMethod("private " +generatedName+"()");

    if (keepHierarchy) {
      injectionType.getNestedTypes();
      JClassType superType = injectionType;
      while (superType != superType.getSuperclass()) {
        superType = superType.getSuperclass();
        if (superType == null) {
          break;
        }
        superType.getNestedTypes();
      }
    }

    // This is the method that fills in all of the extra class data
    final String memberPool = cls.addImport(JsMemberPool.class);
    final String getMemberPool = cls.addImportStatic(JsMemberPool.class, "getMembers");

    final MethodBuffer enhanceMethod = cls.createMethod
      ("private static void doEnhance(final Class<" +typeName+"> toEnhance)")
      .indent()
      .println(generatedName + " classMap = new "+generatedName+"();")
      .println("ReflectUtil.setClassData(toEnhance, classMap);")
      .println("remember(constId(toEnhance), classMap);")
      .println(memberPool + " members = "+getMemberPool+"(toEnhance);")
    ;

    // Print the public method that will conditionally enhance the class in question
    cls.createMethod
    ("public static Class<"+typeName+"> enhanceClass(final Class<" +typeName+"> toEnhance)")
      .println("if (Class.needsEnhance(toEnhance))")
      .indentln("doEnhance(toEnhance);")
      .returnValue("toEnhance");
    ;

    final String returnName = injectionType.isPrivate() ? "Object" : simpleName;
    if (injectionType.isDefaultInstantiable() && !(injectionType instanceof JEnumType)) {
      final NewInstanceStrategy newInst = getNewInstanceStrategy(logger, strategy, injectionType, context);
      implementNewInstance(logger, newInst, cls, clsToEnhance, cls.createMethod("public "+returnName+" newInstance()"), context);
    } else {
      cls
        .createMethod("public "+returnName+" newInstance()")
        .returnValue("throw new "+UnsupportedOperationException.class.getName()+"()");
    }

    // keep any declared methods
    if (generateMethods(logger, manifest, classBuilder, reflectionCtx)) {
      enhanceMethod.println("enhanceMethods(toEnhance);");
    }

    // now, do the fields
    if (generateFields(logger, manifest, classBuilder, reflectionCtx)) {
      enhanceMethod.println("enhanceFields(toEnhance);");
    }

    // constructors...
    if (generateConstructors(logger, manifest, classBuilder, reflectionCtx)) {
      enhanceMethod.println("enhanceConstructors(toEnhance);");
    }

    if (logger.isLoggable(Type.DEBUG)) {
      logger.log(Type.DEBUG, "Keep Annos: "+keepAnnos+ "; type: "+injectionType+" with strategy"+strategy);
    }
    if (keepAnnos) {
      final Annotation[] allAnnotations = extractAnnotations(strategy, injectionType);
      GwtAnnotationGenerator.printAnnotationEnhancer(logger, classBuilder, injectionType, ast.getGeneratorContext(), allAnnotations);
      enhanceMethod.println("enhanceAnnotations(members);");
    }

    if (extractClasses(logger, strategy, classFilter(logger, injectionType), injectionType, manifest)) {
      final MethodBuffer enhanceClasses = cls.createMethod("private static void enhanceClasses" +
        "(final Class<?> cls)")
        .setUseJsni(true)
        .println("var map = cls.@java.lang.Class::classData;")
        ;
      for (final JClassType subclass : manifest.innerClasses.keySet()) {
        enhanceClasses
          .print("map.@com.google.gwt.reflect.shared.ClassMap::addClass(")
          .print("Ljava/lang/Class;Lcom/google/gwt/core/client/JavaScriptObject;)(@")
          .print(subclass.getQualifiedSourceName()+"::class")
          .println(", map.@com.google.gwt.reflect.shared.ClassMap::classes);");
      }
      for (final JClassType iface : injectionType.getImplementedInterfaces()) {
        enhanceClasses
          .print("map.@com.google.gwt.reflect.shared.ClassMap::addClass(")
          .print("Ljava/lang/Class;Lcom/google/gwt/core/client/JavaScriptObject;)(@")
          .print(iface.getQualifiedSourceName()+"::class")
          .println(", map.@com.google.gwt.reflect.shared.ClassMap::ifaces);");
      }
      if (injectionType.getEnclosingType() != null) {
        enhanceClasses
        .print("map.@com.google.gwt.reflect.shared.ClassMap::enclosingClass = @")
        .print(injectionType.getEnclosingType().getQualifiedSourceName()+"::class;");
      }
      enhanceMethod.println("enhanceClasses(toEnhance);");
    }


    if (keepCodesource) {
      logger.log(Type.TRACE, "Processing request to keep codesource for "
          +injectionType.getClass().getName()+" : "+injectionType);
      if (injectionType instanceof JRealClassType) {
        injectLocation(logger, cls, (JRealClassType)injectionType);
      } else {
        logger.log(Type.WARN, "Requested code location for "+injectionType.getQualifiedSourceName()
          +" was not found; expected JRealClassType, got "+injectionType.getClass().getName());
      }
    }

    if (keepPackageName) {
      cls.createMethod("public Package getPackage()")
        .println("return Package.getPackage(\""+packageName+"\");");
    }

    if ((strategy.debug() & ReflectionStrategy.TYPE) > 0) {
      logger.log(Type.INFO, "Class Enhancer source dump for " +clsToEnhance+":");
      logger.log(Type.INFO, classBuilder.toString());
    }

    // Actually write the file
    printWriter.append(classBuilder.toString());
    ConstPoolGenerator.maybeCommit(logger, context);
    context.commit(logger, printWriter);

    if (keepHierarchy) {
      JClassType superType = injectionType.getSuperclass();
      while (superType != null) {
        execImpl(logger, reflectionCtx, superType);
        superType = injectionType.getSuperclass();
      }
    }

    return packageName+"."+generatedName;
  }

  /**
   * Returns the array of annotations to support for the given type.
   * <p>
   * If the reflection strategy is set to {@link ReflectionStrategy#NONE}, then an empty array is returned.
   * <p>
   * If the reflection strategy includes {@link ReflectionStrategy#COMPILE} or {@link ReflectionStrategy#RUNTIME},
   * then annotations with those retention policies are included.
   * <p>
   * If the reflection strategy includes {@link ReflectionStrategy#INHERITED}, then the supertype chain will
   * be searched for default annotation values.
   */
  private Annotation[] extractAnnotations(final ReflectionStrategy strategy,
      final JClassType injectionType) {

    // First, short-circuit for the simple options.
    switch (strategy.annotationRetention()) {
      case NONE:
        return EMPTY_ANNOTATIONS;
      case ALL_ANNOTATIONS:
        return injectionType.getAnnotations();
      case COMPILE | RUNTIME:
        return injectionType.getDeclaredAnnotations();
    }

    // If we get here, then we are including some combination of compile, runtime and inherited.
    // So, we must filter the available results
    final boolean inherit = (strategy.annotationRetention() & INHERITED) == INHERITED;
    final boolean runtime = (strategy.annotationRetention() & RUNTIME) == RUNTIME;
    final boolean compile = (strategy.annotationRetention() & COMPILE) == COMPILE;
    final List<Annotation> supported = new ArrayList<Annotation>();
    for (final Annotation anno : inherit ? injectionType.getAnnotations() : injectionType.getDeclaredAnnotations()) {
      final Retention retention = anno.annotationType().getAnnotation(Retention.class);
      if (retention == null) {
        if (compile) {
          supported.add(anno);
        }
      } else {
        switch (retention.value()) {
          case RUNTIME:
            if (runtime) {
              supported.add(anno);
            }
            break;
          case CLASS:
            if (compile) {
              supported.add(anno);
            }
            break;
          default: // Ignore source annotations; we don't have access to them anyway
        }
      }
    }

    return supported.toArray(new Annotation[supported.size()]);
  }

  private boolean generateMethods(final TreeLogger logger,
      final ReflectionManifest manifest,
      final SourceBuilder<Object> classBuilder,
      final ReflectionGeneratorContext ctx
    ) throws UnableToCompleteException {
    final Collection<ReflectionUnit<JMethod>> methods = manifest.getMethods();
    if (methods.size() > 0) {
      final MethodBuffer initMethod =
          classBuilder.getClassBuffer().createMethod("public static void enhanceMethods(Class<?> cls)")
              .addAnnotation(UnsafeNativeLong.class);
        initMethod
          .println("JsMemberPool map = JsMemberPool.getMembers(cls);")
          .addImport("com.google.gwt.reflect.shared.JsMemberPool");
        final GeneratorContext context = ctx.getGeneratorContext();
        final TypeOracle oracle = context.getTypeOracle();
        for (final ReflectionUnit<JMethod> unit : methods) {
          final JMethod method = unit.getNode();
          final String methodFactoryName = MethodGenerator.getMethodFactoryName(method.getEnclosingType(), method.getName(), method.getParameters());
          JClassType existing;

          String factory;
          synchronized (GENERATOR) {
            existing = oracle.findType(method.getEnclosingType().getPackage().getName(), methodFactoryName);
            if (existing == null) {
              factory = methodGenerator.generateMethodFactory(logger, ctx, method, methodFactoryName, manifest);
            } else {
              factory = existing.getQualifiedSourceName();
            }
          }
          factory = initMethod.addImport(factory);
          initMethod.println("map.addMethod("+factory+".instantiate());");
        }
        return true;
    }
    return false;
  }

  private boolean generateConstructors(final TreeLogger logger,
      final ReflectionManifest manifest, final SourceBuilder<Object> classBuilder,
      final ReflectionGeneratorContext context) throws UnableToCompleteException {
    final Collection<ReflectionUnit<JConstructor>> ctors = manifest.getConstructors();
    if (ctors.size() > 0) {
      final MethodBuffer initMethod = classBuilder.getClassBuffer()
          .createMethod("public static void enhanceConstructors(Class<?> cls)")
          .println("JsMemberPool map = JsMemberPool.getMembers(cls);")
          .addImports("com.google.gwt.reflect.shared.JsMemberPool");
      final TypeOracle oracle = context.getTypeOracle();
      for (final ReflectionUnit<JConstructor> unit : ctors) {
        final JConstructor ctor = unit.getNode();
        final String ctorFactoryName = ConstructorGenerator.getConstructorFactoryName(ctor.getEnclosingType(), ctor.getParameters());
        JClassType existing;
        String factory;
        existing = oracle.findType(ctor.getEnclosingType().getPackage().getName(), ctorFactoryName);
        if (existing == null) {
          factory = constructorGenerator.generateConstructorFactory(logger, context, ctor, ctorFactoryName, manifest);
        } else {
          factory = existing.getQualifiedSourceName();
        }
        factory = initMethod.addImport(factory);
        initMethod.println("map.addConstructor("+factory+".instantiate());");
      }
      return true;
    }
    return false;
  }

  private boolean generateFields(final TreeLogger logger,
      final ReflectionManifest manifest, final SourceBuilder<Object> classBuilder, final ReflectionGeneratorContext ctx) throws UnableToCompleteException {
    final Collection<ReflectionUnit<JField>> fields = manifest.getFields();
    if (fields.size() > 0) {
      final MethodBuffer initMethod = classBuilder.getClassBuffer()
          .createMethod("public static void enhanceFields(Class<?> cls)")
          .println("JsMemberPool map = JsMemberPool.getMembers(cls);")
          .addImports(JsMemberPool.class);
      final TypeOracle oracle = ctx.getTypeOracle();
      for (final ReflectionUnit<JField> unit : fields) {
        final JField field = unit.getNode();
        final String fieldFactoryName = FieldGenerator.getFieldFactoryName(field.getEnclosingType(), field.getName());
        JClassType existing;
        String factory;
        synchronized(GENERATOR) {
          existing = oracle.findType(field.getEnclosingType().getPackage().getName(), fieldFactoryName);
          if (existing == null) {
            factory = fieldGenerator.generateFieldFactory(logger, ctx, field, fieldFactoryName, manifest);
          } else {
            factory = existing.getQualifiedSourceName();
          }
        }
        factory = initMethod.addImport(factory);
        initMethod.println("map.addField("+factory+".instantiate());");
      }
      return true;
    }
    return false;
  }

  protected JClassType getInjectionType(final TreeLogger logger, final JClassType targetType, final GeneratorContext context) {
    return targetType;
  }

  protected NewInstanceStrategy getNewInstanceStrategy(final TreeLogger logger, final ReflectionStrategy strategy,
    final JClassType injectionType, final GeneratorContext context) throws UnableToCompleteException {
    final Class<? extends NewInstanceStrategy> newInst = strategy.newInstanceStrategy();
    final String name = newInst.getName();
    NewInstanceStrategy template;
    if (newInstanceStrategies.containsKey(name)) {
      template = newInstanceStrategies.get(newInst.getName());
    } else {
      try {
        template = newInst.newInstance();
      } catch (final Exception e) {
        logger.log(Type.ERROR, "Unable to create new instance of "+name, e);
        template = null;
      }
      newInstanceStrategies.put(name, template);
    }
    if (template == null) {
      logger.log(Type.ERROR, "Ensure New Instance Strategy " +name+
        " is on the classpath and has a zero-arg public constructor");
      throw new UnableToCompleteException();
    }
    return template;
  }

  protected void injectLocation(final TreeLogger logger, final ClassBuffer cls, final JRealClassType injectionType) {
    String location;
    try {
      location = (String)injectionType.getClass().getMethod("getLocation").invoke(injectionType);
    } catch (final Exception e) {
      if (logOnce) {
        logOnce  = false;
        logger.log(Type.ERROR, "Unable to call "+injectionType.getClass().getName()+".getLocation on "+injectionType.getJNISignature());
        logger.log(Type.ERROR, "Ensure that you have the jar/artifact net.wetheinter:gwt-reflect before gwt-dev on your classpath.");
        logger.log(Type.TRACE, "The artifact net.wetheinter:com.google.gwt.thirdparty.xapi-gwt-api contains a class, ClasspathFixer, which can help you.");
        logger.log(Type.TRACE, "For unit tests, com.google.gwt.thirdparty.xapi-gwt-test overrides JUnitShell to fix the classpath for you.", e);
      }
      return;
    }
    cls.addImports(ProtectionDomain.class);
    cls
      .println("private ProtectionDomain domain;")
      .createMethod("public ProtectionDomain getProtectionDomain()")
        .println("if (domain == null) ")
        .indentln("domain = new ProtectionDomain(\"" +location+"\");")
        .println("return domain;");
  }

  @SuppressWarnings("unchecked")
  private MemberFilter<JClassType> classFilter(final TreeLogger logger, final JClassType injectionType) {
    return MemberFilter.DEFAULT_FILTER;
  }

  protected void implementNewInstance(final TreeLogger logger, final NewInstanceStrategy strategy, final ClassBuffer cls, final String clsToEnhance, final MethodBuffer newInstance, final GeneratorContext context) {
    if (UseGwtCreate.class.isAssignableFrom(strategy.getClass())) {
      cls.addImport(GWT.class);
    }
    newInstance.println(strategy.generate(clsToEnhance));
  }

  private static boolean extractClasses(final TreeLogger logger, final ReflectionStrategy strategy, final MemberFilter<JClassType> keepClass, final JClassType injectionType,
    final ReflectionManifest manifest) {
    final Set<String> seen = new HashSet<String>();
    final Set<? extends JClassType> allTypes = injectionType.getFlattenedSupertypeHierarchy();

    for(JClassType nextClass : allTypes) {
      for (final JClassType cls : nextClass.getNestedTypes()) {
        if (keepClass.keepMember(cls, cls.getEnclosingType() == injectionType, cls.isPublic(), strategy)){
          // do not include overridden methods
          // TODO check for covariance?
          if (seen.add(cls.getQualifiedSourceName())) {
            final Annotation[] annos;
            // only keep annotations annotated with KeepAnnotation.
            final List<Annotation> keepers = new ArrayList<Annotation>();
            for (final Annotation anno : cls.getAnnotations()) {
              if (keepClass.keepAnnotation(cls, anno, strategy)) {
                keepers.add(anno);
              }
            }
            annos = keepers.toArray(new Annotation[keepers.size()]);
            manifest.innerClasses.put(cls, annos);
          }
        }
      }
      nextClass = nextClass.getSuperclass();
    }
    return true;
  }

}