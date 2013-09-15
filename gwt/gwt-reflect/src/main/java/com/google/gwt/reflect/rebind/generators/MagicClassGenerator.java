package com.google.gwt.reflect.rebind.generators;

import static com.google.gwt.reflect.rebind.ReflectionManifest.getReflectionManifest;

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

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.source.read.SourceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
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
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.reflect.client.ClassMap;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.JsMemberPool;
import com.google.gwt.reflect.client.MemberMap;
import com.google.gwt.reflect.client.strategy.NewInstanceStrategy;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.client.strategy.UseGwtCreate;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilType;
import com.google.gwt.reflect.rebind.ReflectionUnit;

@ReflectionStrategy()
public class MagicClassGenerator extends IncrementalGenerator {

  public static interface MemberFilter <T extends HasAnnotations> {
    boolean keepMember(T type, boolean isDeclared, boolean isPublic, ReflectionStrategy strategy);
    boolean keepAnnotation(T type, Annotation anno, ReflectionStrategy strategy);
    @SuppressWarnings("rawtypes")
    final MemberFilter DEFAULT_FILTER = new MemberFilter() {
      public boolean keepAnnotation(HasAnnotations type, Annotation anno, ReflectionStrategy strategy) {
        Retention retention = anno.annotationType().getAnnotation(Retention.class);
        if (retention == null)
          return (strategy.annotationRetention() & ReflectionStrategy.COMPILE) > 0;
        switch (retention.value()) {
          case RUNTIME:
            return (strategy.annotationRetention() & ReflectionStrategy.RUNTIME) > 0;
          case CLASS:
            return (strategy.annotationRetention() & ReflectionStrategy.COMPILE) > 0;
          case SOURCE:
            // gwt doesn't have the source annos anyway
          default:
            return false;
        }
      };
      public boolean keepMember(HasAnnotations type, boolean isDeclared, boolean isPublic, ReflectionStrategy strategy) {
        return isDeclared ? true : strategy.magicSupertypes() ? isPublic : true;
      };
    };

  }

  protected class ManifestMap extends MemberGenerator {}

  private final ManifestMap manifests = newManifestMap();
  
  private final HashMap<String, NewInstanceStrategy> newInstanceStrategies = new HashMap<String, NewInstanceStrategy>();
  private static final ThreadLocal<HashSet<String>> finished = new ThreadLocal<HashSet<String>>() {
    @Override
    protected java.util.HashSet<String> initialValue() {
      return new HashSet<String>();
    };
  };
  public static void cleanup() {
    finished.remove();
  }

  protected ManifestMap newManifestMap() {
    return new ManifestMap();
  }

  private boolean logOnce = true;

  public RebindResult execImpl(TreeLogger logger, GeneratorContext context, JClassType type)
    throws UnableToCompleteException {

    String simpleName = SourceUtil.toSourceName(type.getSimpleSourceName());
    String generatedName = ReflectionUtilJava.generatedMagicClassName(simpleName);
    String packageName = type.getPackage().getName();
    String pkg = packageName.length() == 0 ? "" : packageName+".";

    HashSet<String> done = finished.get();
    if (!done.add(type.getQualifiedSourceName()))
      return new RebindResult(RebindMode.USE_ALL_CACHED, pkg+generatedName);

    ReflectionManifest manifest;

    PrintWriter printWriter = context.tryCreate(logger, packageName, generatedName);
    int unique = 0;
    String next = generatedName;
    while(printWriter == null){
      next = generatedName+"_"+unique++;
      printWriter = context.tryCreate(logger, packageName, next);
    }
    generatedName = next;


    JClassType targetType = type;
    String clsToEnhance = type.getQualifiedSourceName();
    JClassType injectionType = getInjectionType(logger, targetType, context);

    manifest = getReflectionManifest(logger, injectionType.getQualifiedSourceName(), (StandardGeneratorContext)context);
    
    ReflectionStrategy strategy = manifest.getStrategy();
    boolean keepHierarchy = strategy.magicSupertypes();
    boolean keepCodesource = strategy.keepCodeSource();
    boolean keepPackageName = strategy.keepPackage();
    boolean keepAnnos = strategy.annotationRetention() > 0;

    if (logger.isLoggable(Type.TRACE))
      logger.log(Type.TRACE,"Writing magic class instance for " + type.getQualifiedSourceName() + " -> " +
          injectionType.getQualifiedSourceName());

    if ((strategy.debug() & ReflectionStrategy.SOURCE) > 0) {
      logger.log(Type.INFO, "Source Dump For " +clsToEnhance+":");
      logger.log(Type.INFO, type.toString());
    }
    
    String typeName = injectionType.isPrivate() ? "?" : simpleName;
    SourceBuilder<Object> classBuilder = new SourceBuilder<Object>(
        "public class "+generatedName +" extends ClassMap"
            + (injectionType.isPrivate()? "" : "<"+simpleName+">"))
      .setPackage(packageName);
    if (!injectionType.isPrivate()){
      classBuilder.getImports().addImports(clsToEnhance);
    }
    classBuilder.getImports()
      .addImports(
          ClassMap.class, JavaScriptObject.class, UnsafeNativeLong.class,
          MemberMap.class, Annotation.class,
          Constructor.class,Method.class,Field.class,CodeSource.class)
      .addStatics(ConstPool.class.getName()+".constId")
     ;


    ClassBuffer cls = classBuilder.getClassBuffer();

    cls.createMethod("private " +generatedName+"()");

    if (keepHierarchy) {
      injectionType.getNestedTypes();
      JClassType superType = injectionType;
      while (superType != superType.getSuperclass()) {
        superType = superType.getSuperclass();
        superType.getNestedTypes();
      }
    }
    // This is the method that fills in all of the extra class data
    MethodBuffer enhanceMethod = cls.createMethod
      ("public static Class<"+typeName+"> enhanceClass(final Class<" +typeName+"> toEnhance)")
      .println("if (Class.needsEnhance(toEnhance)) {")
      .indent()
      .println(generatedName + " classMap = new "+generatedName+"();")
      .println("MemberMap.setClassData(toEnhance, classMap);")
      .println("remember(constId(toEnhance), classMap);")
    ;
    String returnName = injectionType.isPrivate() ? "Object" : simpleName;
    if (injectionType.isDefaultInstantiable() && !(injectionType instanceof JEnumType)) {
      NewInstanceStrategy newInst = getNewInstanceStrategy(logger, strategy, injectionType, context);
      implementNewInstance(logger, newInst, cls, clsToEnhance, cls.createMethod("public "+returnName+" newInstance()"), context);
    } else {
      cls
        .createMethod("public "+returnName+" newInstance()")
        .returnValue("throw new "+UnsupportedOperationException.class.getName()+"()");
    }

    // keep any declared methods
    if (generateMethods(logger, manifest, classBuilder, context)) {
      enhanceMethod.println("enhanceMethods(toEnhance);");
    }

    // now, do the fields
    if (generateFields(logger, manifest, classBuilder, context)) {
      enhanceMethod.println("enhanceFields(toEnhance);");
    }

    // constructors...
    if (generateConstructors(logger, manifest, classBuilder, context)) {
      enhanceMethod.println("enhanceConstructors(toEnhance);");
    }

    if (keepAnnos) {
      if (GwtAnnotationGenerator.generateAnnotations(
        logger, classBuilder, context, injectionType.getAnnotations()).length > 0) {
        enhanceMethod.println("enhanceAnnotations(toEnhance);");
      }
    }
    
    if (extractClasses(logger, strategy, classFilter(logger, injectionType), injectionType, manifest)) {
      MethodBuffer enhanceClasses = cls.createMethod("private static void enhanceClasses" +
        "(final Class<?> cls)")
        .setUseJsni(true)
        .println("var map = cls.@java.lang.Class::classData;")
        ;
      for (JClassType subclass : manifest.innerClasses.keySet()) {
        enhanceClasses
          .print("map.@com.google.gwt.reflect.client.ClassMap::addClass(")
          .print("Ljava/lang/Class;Lcom/google/gwt/core/client/JavaScriptObject;)(@")
          .print(subclass.getQualifiedSourceName()+"::class")
          .println(", map.@com.google.gwt.reflect.client.ClassMap::classes);");
      }
      for (JClassType iface : injectionType.getImplementedInterfaces()) {
        enhanceClasses
          .print("map.@com.google.gwt.reflect.client.ClassMap::addClass(")
          .print("Ljava/lang/Class;Lcom/google/gwt/core/client/JavaScriptObject;)(@")
          .print(iface.getQualifiedSourceName()+"::class")
          .println(", map.@com.google.gwt.reflect.client.ClassMap::ifaces);");
      }
      enhanceMethod.println("enhanceClasses(toEnhance);");
    }

    enhanceMethod
      .outdent()
      .println("}")
      .returnValue("toEnhance");

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
    context.commit(logger, printWriter);

    if (keepHierarchy) {
      JClassType superType = injectionType.getSuperclass();
      while (superType != null) {
        execImpl(logger, context, superType);
        superType = injectionType.getSuperclass();
      }
    }

    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, packageName+"."+generatedName);

  }


  private boolean generateMethods(TreeLogger logger,
      ReflectionManifest manifest, SourceBuilder<Object> classBuilder, GeneratorContext context) throws UnableToCompleteException {
    Collection<ReflectionUnit<JMethod>> methods = manifest.getMethods();
    if (methods.size() > 0) {
      MethodBuffer initMethod =
          classBuilder.getClassBuffer().createMethod("public static void enhanceMethods(Class<?> cls)")
              .addAnnotation(UnsafeNativeLong.class);
        initMethod
          .println("JsMemberPool map = ConstPool.getMembers(cls);")
          .addImports(JsMemberPool.class, ConstPool.class);
        TypeOracle oracle = context.getTypeOracle();
        for (ReflectionUnit<JMethod> unit : methods) {
          JMethod method = unit.getNode();
          String methodFactoryName = MemberGenerator.getMethodFactoryName(method.getEnclosingType(), method.getName(), method.getParameters());
          JClassType existing;

          String factory;
          synchronized (finished) {
            existing = oracle.findType(method.getEnclosingType().getPackage().getName(), methodFactoryName);
            if (existing == null)
              factory = manifests.generateMethodFactory(logger, context, method, methodFactoryName, manifest);
            else {
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

  private boolean generateConstructors(TreeLogger logger,
      ReflectionManifest manifest, SourceBuilder<Object> classBuilder, GeneratorContext context) throws UnableToCompleteException {
    Collection<ReflectionUnit<JConstructor>> ctors = manifest.getConstructors();
    if (ctors.size() > 0) {
      MethodBuffer initMethod = classBuilder.getClassBuffer()
          .createMethod("public static void enhanceConstructors(Class<?> cls)")
          .println("JsMemberPool map = ConstPool.getMembers(cls);")
          .addImports(JsMemberPool.class, ConstPool.class);
      TypeOracle oracle = context.getTypeOracle();
      for (ReflectionUnit<JConstructor> unit : ctors) {
        JConstructor ctor = unit.getNode();
        String ctorFactoryName = MemberGenerator.getConstructorFactoryName(ctor.getEnclosingType(), ctor.getParameters());
        JClassType existing;
        String factory;
        synchronized(finished) {
          existing = oracle.findType(ctor.getEnclosingType().getPackage().getName(), ctorFactoryName);
          if (existing == null)
            factory = manifests.generateConstructorFactory(logger, context, ctor, ctorFactoryName, manifest);
          else {
            factory = existing.getQualifiedSourceName();
          }
        }
        factory = initMethod.addImport(factory);
        initMethod.println("map.addConstructor("+factory+".instantiate());");
      }
      return true;
    }
    return false;
  }
  
  private boolean generateFields(TreeLogger logger,
      ReflectionManifest manifest, SourceBuilder<Object> classBuilder, GeneratorContext context) throws UnableToCompleteException {
    Collection<ReflectionUnit<JField>> fields = manifest.getFields();
    if (fields.size() > 0) {
      MethodBuffer initMethod = classBuilder.getClassBuffer()
          .createMethod("public static void enhanceFields(Class<?> cls)")
          .println("JsMemberPool map = ConstPool.getMembers(cls);")
          .addImports(JsMemberPool.class, ConstPool.class);
      TypeOracle oracle = context.getTypeOracle();
      for (ReflectionUnit<JField> unit : fields) {
        JField field = unit.getNode();
        String fieldFactoryName = MemberGenerator.getFieldFactoryName(field.getEnclosingType(), field.getName());
        JClassType existing;
        String factory;
        synchronized(finished) {
          existing = oracle.findType(field.getEnclosingType().getPackage().getName(), fieldFactoryName);
          if (existing == null)
            factory = manifests.generateFieldFactory(logger, context, field, fieldFactoryName, manifest);
          else {
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

  protected JClassType getInjectionType(TreeLogger logger, JClassType targetType, GeneratorContext context) {
    return targetType;
  }

  protected NewInstanceStrategy getNewInstanceStrategy(TreeLogger logger, ReflectionStrategy strategy,
    JClassType injectionType, GeneratorContext context) throws UnableToCompleteException {
    Class<? extends NewInstanceStrategy> newInst = strategy.newInstanceStrategy();
    String name = newInst.getName();
    NewInstanceStrategy template;
    if (newInstanceStrategies.containsKey(name)) {
      template = newInstanceStrategies.get(newInst.getName());
    } else {
      try {
        template = newInst.newInstance();
      } catch (Exception e) {
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

  protected void injectLocation(TreeLogger logger, ClassBuffer cls, JRealClassType injectionType) {
    String location;
    try {
      location = (String)injectionType.getClass().getMethod("getLocation").invoke(injectionType);
    } catch (Exception e) {
      if (logOnce) {
        logOnce  = false;
        logger.log(Type.ERROR, "Unable to call "+injectionType.getClass().getName()+".getLocation on "+injectionType.getJNISignature());
        logger.log(Type.ERROR, "Ensure that you have the jar/artifact net.wetheinter:gwt-reflect before gwt-dev on your classpath.");
        logger.log(Type.ERROR, "The artifact net.wetheinter:xapi-gwt-api contains a class, ClasspathFixer, which can help you.");
        logger.log(Type.ERROR, "For unit tests, xapi-gwt-test overrides JUnitShell to fix the classpath for you.", e);
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
  private MemberFilter<JClassType> classFilter(TreeLogger logger, JClassType injectionType) {
    return MemberFilter.DEFAULT_FILTER;
  }

  @SuppressWarnings("unchecked")
  protected MemberFilter<JConstructor> constructorFilter(TreeLogger logger, JClassType injectionType) {
    return MemberFilter.DEFAULT_FILTER;
  }

  protected void implementNewInstance(TreeLogger logger, NewInstanceStrategy strategy, ClassBuffer cls, String clsToEnhance, MethodBuffer newInstance, GeneratorContext context) {
    if (UseGwtCreate.class.isAssignableFrom(strategy.getClass()))
      cls.addImport(GWT.class);
    newInstance.println(strategy.generate(clsToEnhance));
  }

  private static boolean extractConstructors(TreeLogger logger, ReflectionStrategy strategy, MemberFilter<JConstructor> keepCtor, JClassType injectionType,
      ReflectionManifest manifest) {
    Set<String> seen = new HashSet<String>();
    Set<? extends JClassType> allTypes = injectionType.getFlattenedSupertypeHierarchy();

    for(JClassType nextClass : allTypes) {
      for (JConstructor ctor : nextClass.getConstructors()) {
        if (keepCtor.keepMember(ctor, ctor.getEnclosingType() == injectionType, ctor.isPublic(), strategy)){
          // do not include overridden constructors
          if (seen.add(ReflectionUtilType.toJsniClassLits(ctor.getParameterTypes()))) {
            final Annotation[] annos;
            final List<Annotation> keepers = new ArrayList<Annotation>();
            for (Annotation anno : ctor.getAnnotations()) {
              if (keepCtor.keepAnnotation(ctor, anno, strategy))
                keepers.add(anno);
            }
            annos = keepers.toArray(new Annotation[keepers.size()]);
            manifest.constructors.put(ctor, annos);
          }
        }
      }
      nextClass = nextClass.getSuperclass();
    }
    return true;
  }

  private static boolean extractClasses(TreeLogger logger, ReflectionStrategy strategy, MemberFilter<JClassType> keepClass, JClassType injectionType,
    ReflectionManifest manifest) {
    Set<String> seen = new HashSet<String>();
    Set<? extends JClassType> allTypes = injectionType.getFlattenedSupertypeHierarchy();

    for(JClassType nextClass : allTypes) {
      for (JClassType cls : nextClass.getNestedTypes()) {
        if (keepClass.keepMember(cls, cls.getEnclosingType() == injectionType, cls.isPublic(), strategy)){
          // do not include overridden methods
          // TODO check for covariance?
          if (seen.add(cls.getQualifiedSourceName())) {
            final Annotation[] annos;
            // only keep annotations annotated with KeepAnnotation.
            final List<Annotation> keepers = new ArrayList<Annotation>();
            for (Annotation anno : cls.getAnnotations()) {
              if (keepClass.keepAnnotation(cls, anno, strategy))
                keepers.add(anno);
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

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext context, String typeName)
    throws UnableToCompleteException {
    TypeOracle oracle = context.getTypeOracle();
    logger.log(Type.TRACE, "Generating magic class for " + typeName);
    try {
      return execImpl(logger, context, oracle.getType(SourceUtil.toSourceName(typeName)));
    } catch (NotFoundException e) {
      logger.log(Type.ERROR, "Could not find class for " + typeName, e);
    }
    throw new UnableToCompleteException();
  }

  @Override
  public long getVersionId() {
    return 0;
  }

}