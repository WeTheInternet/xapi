package xapi.dev.reflect;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.annotation.reflect.KeepAnnotation;
import xapi.annotation.reflect.KeepClass;
import xapi.annotation.reflect.KeepConstructor;
import xapi.annotation.reflect.KeepField;
import xapi.annotation.reflect.KeepMethod;
import xapi.annotation.reflect.NewInstanceStrategy;
import xapi.dev.generators.AbstractInjectionGenerator;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.ImportSection;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.util.CurrentGwtPlatform;
import xapi.inject.X_Inject;
import xapi.platform.Platform;
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
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.reflect.client.ClassMap;
import com.google.gwt.reflect.client.MemberMap;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;

public class MagicClassGenerator extends IncrementalGenerator {

  protected static class ReflectionManifest {
    KeepClass cls;
    KeepMethod method;
    KeepField field;
    KeepConstructor constructor;
    KeepAnnotation anno;
    Map<JMethod, Annotation[]> methods = new LinkedHashMap<JMethod, Annotation[]>();
    Map<JField, Annotation[]> fields = new LinkedHashMap<JField, Annotation[]>();
    Map<JConstructor, Annotation[]> constructors = new LinkedHashMap<JConstructor, Annotation[]>();
    Map<JClassType, Annotation[]> innerClasses = new LinkedHashMap<JClassType, Annotation[]>();
    public ReflectionManifest cleanCopy() {
      ReflectionManifest copy = new ReflectionManifest();
      copy.cls = cls;
      copy.method = method;
      copy.field = field;
      copy.constructor = constructor;
      copy.anno = anno;
      return copy;
    }
  }

  @SuppressWarnings("serial")
  private static class ManifestMap <K extends HasAnnotations> extends HashMap<K,ReflectionManifest> {
    @Override
    public synchronized ReflectionManifest get(Object key) {
      ReflectionManifest value = super.get(key);
      if (value == null) {
        value = new ReflectionManifest();
        @SuppressWarnings("unchecked") // private class
        K type = (K)key;
        value.cls = type.getAnnotation(KeepClass.class);
        value.method = type.getAnnotation(KeepMethod.class);
        value.field = type.getAnnotation(KeepField.class);
        value.constructor = type.getAnnotation(KeepConstructor.class);
        value.anno = type.getAnnotation(KeepAnnotation.class);
        put(type, value);
      }
      return value;
    }
  }


  private static final ManifestMap<JPackage> packages = new ManifestMap<JPackage>();

  public static RebindResult execImpl(TreeLogger logger, GeneratorContext context, JClassType type)
    throws UnableToCompleteException {


    // perform annotation and type discovery.
    // we all package-level, class level, and method/field level declarations,
    // which we now get to merge to find out how much to implement

    JPackage pkg = type.getPackage();
    ReflectionManifest manifest;
    KeepClass defaultClass;
    KeepClass keepClass;
    KeepConstructor keepConstructor;
    KeepMethod keepMethod;
    KeepField keepField;
    KeepAnnotation keepAnnotation;
    String classDebug = "";
    NewInstanceStrategy newInst = null;
    boolean keepCodesource = false;
    boolean keepHierarchy = false;
    boolean keepPackageName = true;

    synchronized (packages){

      manifest = packages.get(pkg).cleanCopy();

      defaultClass = manifest.cls;
      keepClass = type.getAnnotation(KeepClass.class);
      keepConstructor = type.getAnnotation(KeepConstructor.class);
      keepMethod = type.getAnnotation(KeepMethod.class);
      keepField = type.getAnnotation(KeepField.class);
      keepAnnotation = type.getAnnotation(KeepAnnotation.class);

      if (defaultClass != null) {
        keepCodesource = defaultClass.keepCodeSource();
        keepHierarchy = defaultClass.keepHierarchy();
        keepPackageName = defaultClass.keepPackage();
        classDebug = defaultClass.debugData();
        newInst = defaultClass.newInstance();
        keepClass = defaultClass;
        if (keepConstructor == null)
          keepConstructor = manifest.constructor;
        if (keepMethod == null)
          keepMethod = manifest.method;
        if (keepField == null)
          keepField = manifest.field;
        if (keepAnnotation == null)
          keepAnnotation = manifest.anno;
      }
      if (keepClass != null) {
        classDebug = keepClass.debugData();
        newInst = keepClass.newInstance();
        keepCodesource = keepClass.keepCodeSource();
        keepPackageName = keepClass.keepPackage();
        keepHierarchy = keepClass.keepHierarchy();
      }
    }


    JClassType injectionType = null;
    JClassType targetType = type;
    String packageName = type.getPackage().getName();
    String simpleName = SourceUtil.toSourceName(type.getSimpleSourceName());
    String generatedName = ReflectionUtilJava.generatedMagicClassName(simpleName);
    String clsToEnhance = SourceUtil.toSourceName(type.getQualifiedSourceName());


    //if the magic class implements newInstance()
    if (newInst != null) {
      //check for singleton or instance level injection.
      switch(newInst) {
      case X_INSTANCE:
        Set<Class<? extends Annotation>> platforms = CurrentGwtPlatform.getPlatforms(context);
        // TODO call into existing generator framework
        InstanceOverride winningInstance = null;
        for (JClassType subtype : type.getSubtypes()) {
          if (injectionType == null) {
            //if we haven't picked a type yet, let's look for defaults
            InstanceDefault def = subtype.getAnnotation(InstanceDefault.class);
            if (def != null) {
              boolean useType = true;
              for (Annotation anno : subtype.getAnnotations()) {
                if (anno.annotationType().getAnnotation(Platform.class) != null) {
                  if (platforms.contains(anno.annotationType())) {
                    useType = true;
                    break;
                  } else {
                    useType = false;
                  }
                }
              }
              if (useType) {
                injectionType = subtype;
              }
            }
          }

          InstanceOverride override = subtype.getAnnotation(InstanceOverride.class);
          if (override != null) {
            boolean useType = true;
            logger.log(Type.TRACE, "Got magic class subtype " + subtype + " : ");
            for (Annotation anno : subtype.getAnnotations()) {
              if (anno.annotationType().getAnnotation(Platform.class)!=null) {
                if (platforms.contains(anno.annotationType())) {
                  useType = true;
                  break;
                } else {
                  useType = false;
                }
              }
            }
            if (!useType)
              continue;
            if (winningInstance != null) {
              if (winningInstance.priority() > override.priority()) continue;
            }
            winningInstance = override;
            injectionType = subtype;

          }
        }
        break;
      case X_SINGLETON:
        platforms = CurrentGwtPlatform.getPlatforms(context);
        SingletonOverride winningSingleton = null;
        for (JClassType subtype : type.getSubtypes()) {

          if (injectionType == null) {
            // Only check defaults if we haven't yet accepted an override.
            // Note that in the case of two valid default types, this will
            // undeterministically chose either of them.

            SingletonDefault def = subtype.getAnnotation(SingletonDefault.class);
            if (def != null) {
              boolean useType = true;
              for (Annotation anno : subtype.getAnnotations()) {
                if (anno.annotationType().getAnnotation(Platform.class) != null) {
                  if (platforms.contains(anno.annotationType())) {
                    useType = true;
                    break;
                  } else {
                    useType = false;
                  }
                }
              }
              if (useType) {
                injectionType = subtype;
              }
            }
          }

          SingletonOverride override = subtype.getAnnotation(SingletonOverride.class);
          if (override != null) {
            logger.log(Type.TRACE, "Got magic class subtype " + subtype+ " - prodMode: " + context.isProdMode());
            boolean useType = true;
            for (Annotation anno : subtype.getAnnotations()) {
              if (anno.annotationType().getAnnotation(Platform.class)!=null) {
                if (platforms.contains(anno.annotationType())) {
                  useType = true;
                  break;
                } else {
                  useType = false;
                }
              }
            }
            if (!useType)
              continue;
            if (winningSingleton != null) {
              if (winningSingleton.priority() > override.priority()) continue;
            }
            winningSingleton = override;
            injectionType = subtype;
          }
        }
        if (injectionType == null) {
          injectionType = targetType;// no matches, resort to instantiate the class sent.
        }
        //Only call ensureProviderClass if injectMethod is X_Inject.singleton
        AbstractInjectionGenerator.
        ensureProviderClass(logger, packageName, type.getSimpleSourceName(), type.getQualifiedSourceName(),
          SourceUtil.toSourceName(injectionType.getQualifiedSourceName()), context);
        default:
      }
    }
    //in case there was no injection target, just use the original Type as injectionType
    if (injectionType == null) {
      injectionType = targetType;// no matches, resort to instantiate the class sent.
    }


    PrintWriter printWriter = context.tryCreate(logger, packageName, generatedName);
    int unique = 0;
    String next = generatedName;
    while(printWriter == null){
      next = generatedName+"_"+unique++;
      printWriter = context.tryCreate(logger, packageName, next);
    }
    generatedName = next;

    if (logger.isLoggable(Type.TRACE))
      logger.log(Type.TRACE,"Writing magic class instance for " + type.getQualifiedSourceName() + " -> " +
          injectionType.getQualifiedSourceName());

    SourceBuilder<Object> classBuilder = new SourceBuilder<Object>(
        "public class "+generatedName +" extends ClassMap<"+simpleName+">")
      .setPackage(packageName);

    ImportSection imports = classBuilder.getImports()
    .addImports(clsToEnhance)
    .addImports(
        ClassMap.class, JavaScriptObject.class, UnsafeNativeLong.class,
        MemberMap.class);

    if (newInst != null)
      imports.addImport(Constructor.class);
    if (keepMethod != null)
      imports.addImport(Method.class);
    if (keepField != null)
      imports.addImport(Field.class);
    if (keepCodesource)
      imports.addImport(CodeSource.class);


    ClassBuffer cls = classBuilder.getClassBuffer();

    cls.createMethod("private " +generatedName+"()");

    if (keepHierarchy) {
      injectionType.getNestedTypes();
    }

    // This is the method that fills in all of the extra class data
    MethodBuffer enhanceMethod = cls.createMethod
      ("public static Class<"+simpleName+"> enhanceClass(Class<" +simpleName+"> toEnhance)")
      .println("if (Class.needsEnhance(toEnhance)) {")
      .indent()
      .println("MemberMap.setClassData(toEnhance, new "+generatedName+"());")
    ;
      if (injectionType.getEnclosingType() != null) {
        if (newInst != null) {
          logger.log(Type.WARN, "Warning: attempting to implement newInstance() on an inner class: " +
          		injectionType+"! Please use @KeepClass(newInstance()==NONE) on all inner classes.");
        }
      } else if (newInst != null) {
        //since we are generating newInstance(), we can make up any sort of provider for the given method.
        //options are: GWT.create, X_Inject.instance/singleton, or new Type();
        MethodBuffer newInstance = cls.createMethod("public " +simpleName+" newInstance()");
        switch (newInst) {
        case NONE:
          newInstance.println("throw new UnsupportedOperationException(\"newInstance not supported for \"" +
          		"+ \"" + clsToEnhance + "\");");
          break;
        case GWT_CREATE:
          cls.addImports(GWT.class);
          newInstance.println("return GWT.create("+clsToEnhance+".class);");
          break;
        case NEW:
          newInstance.println("return new "+clsToEnhance+"();");
          break;
        case X_INSTANCE:
          cls.addImports(X_Inject.class);
          newInstance.println("return X_Inject.instance("+clsToEnhance+".class"+");");
          break;
        case X_SINGLETON:
          cls.addImports(X_Inject.class);
          newInstance.println("return X_Inject.singleton("+clsToEnhance+".class"+");");
          break;
        default:
          break;
        }
      }

      // check type for methods we want to keep
      extractMethods(logger, keepMethod, injectionType, manifest);
      if (keepMethod != null || manifest.methods.size()>0) {
//        GwtMethodGenerator.generateMethods(logger, classBuilder, context, injectionType, manifest);
        enhanceMethod.println("enhanceMethods(toEnhance);");
      }

      // check type for constructors we want to keep
      extractConstructors(logger, keepConstructor, injectionType, manifest);
      if (keepConstructor != null || manifest.constructors.size()>0) {
//        GwtConstructorGenerator.generateConstructors(logger, classBuilder, context, injectionType, manifest.constructors);
        enhanceMethod.println("enhanceConstructors(toEnhance);");
      }

      // now, do the fields
      extractFields(logger, keepField, injectionType, manifest);
      if (keepField != null || manifest.fields.size()>0) {
//        if (GwtFieldGenerator.generateFields(logger, classBuilder, context, injectionType, manifest.fields)){
          enhanceMethod.println("enhanceFields(toEnhance);");
//        }
      }

      enhanceMethod
        .outdent()
        .println("}")
        .println("return toEnhance;");

      if (keepCodesource) {
        if (injectionType instanceof JRealClassType) {
          String location = ((JRealClassType)injectionType).getLocation();
          cls.addImports(ProtectionDomain.class);
          cls
            .println("private ProtectionDomain domain;")
            .createMethod("public ProtectionDomain getProtectionDomain()")
              .println("if (domain == null) ")
              .indentln("domain = new ProtectionDomain(\"" +location+"\");")
              .println("return domain;");
        } else {
          logger.log(Type.WARN, "Requested code location for "+injectionType.getQualifiedSourceName()
            +" was not found; expected JRealClassType, got "+injectionType.getClass().getName());
        }
      }

      if (keepPackageName) {
        cls.createMethod("public Package getPackage()")
          .println("return Package.getPackage(\""+packageName+"\");");
      }

      if (classDebug.length() > 0) {
        logger.log(Type.INFO, classDebug);
        logger.log(Type.INFO, "Source Dump For " +clsToEnhance+":");
        logger.log(Type.INFO, classBuilder.toString());
      }

      // Actually write the file
      printWriter.append(classBuilder.toString());
      context.commit(logger, printWriter);

    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, packageName+"."+generatedName);

  }

  private static void extractConstructors(TreeLogger logger, KeepConstructor keepCtor, JClassType injectionType,
      ReflectionManifest manifest) {
    boolean keepCtors = keepCtor != null;
    boolean keepAnnos = manifest.anno != null;
    Set<String> seen = new HashSet<String>();
    Set<? extends JClassType> allTypes = injectionType.getFlattenedSupertypeHierarchy();

    for(JClassType nextClass : allTypes) {
      for (JConstructor ctor : nextClass.getConstructors()) {
        if (keepCtors || ctor.getAnnotation(KeepConstructor.class) != null){
          // do not include overridden constructors
          if (seen.add(ctor.getJsniSignature())) {
            final Annotation[] annos;
            if (keepAnnos || ctor.getAnnotation(KeepAnnotation.class) != null) {
              annos = ctor.getAnnotations();
            } else {
              // only keep annotations annotated with KeepAnnotation.
              final List<Annotation> keepers = new ArrayList<Annotation>();
              for (Annotation anno : ctor.getAnnotations()) {
                if (anno.annotationType().getAnnotation(KeepAnnotation.class) != null)
                  keepers.add(anno);
              }
              annos = keepers.toArray(new Annotation[keepers.size()]);
            }
            manifest.constructors.put(ctor, annos);
          }
        }
      }
      nextClass = nextClass.getSuperclass();
    }
  }

  private static void extractFields(TreeLogger logger, KeepField keepField, JClassType injectionType,
      ReflectionManifest manifest) {
    boolean keepFields = keepField != null;
    boolean keepAnnos = manifest.anno != null;
    Set<String> seen = new HashSet<String>();
    Set<? extends JClassType> allTypes = injectionType.getFlattenedSupertypeHierarchy();

    for(JClassType nextClass : allTypes) {
      if (nextClass.getQualifiedSourceName().equals("java.lang.Object")) {
        // gwt puts some intrinsic fields in Object we can't access
        continue;
      }
      for (JField field : nextClass.getFields()) {
        if (keepFields || field.getAnnotation(KeepField.class) != null){
          // do not include overridden constructors
          if (seen.add(field.getName())) {
            final Annotation[] annos;
            if (keepAnnos || field.getAnnotation(KeepAnnotation.class) != null) {
              annos = field.getAnnotations();
            } else {
              // only keep annotations annotated with KeepAnnotation.
              final List<Annotation> keepers = new ArrayList<Annotation>();
              for (Annotation anno : field.getAnnotations()) {
                if (anno.annotationType().getAnnotation(KeepAnnotation.class) != null)
                  keepers.add(anno);
              }
              annos = keepers.toArray(new Annotation[keepers.size()]);
            }
            manifest.fields.put(field, annos);
          }
        }
      }
      nextClass = nextClass.getSuperclass();
    }
  }

  private static void extractMethods(TreeLogger logger, KeepMethod keepMethod, JClassType injectionType,
    ReflectionManifest manifest) {
    boolean keepMethods = keepMethod != null;
    boolean keepAnnos = manifest.anno != null;
    Set<String> seen = new HashSet<String>();
    Set<? extends JClassType> allTypes = injectionType.getFlattenedSupertypeHierarchy();

    for(JClassType nextClass : allTypes) {
      for (JMethod method : nextClass.getMethods()) {
        if (keepMethods || method.getAnnotation(KeepMethod.class) != null){
          // do not include overridden methods
          if (seen.add(method.getJsniSignature())) {
            final Annotation[] annos;
            if (keepAnnos || method.getAnnotation(KeepAnnotation.class) != null) {
              annos = method.getAnnotations();
            } else {
              // only keep annotations annotated with KeepAnnotation.
              final List<Annotation> keepers = new ArrayList<Annotation>();
              for (Annotation anno : method.getAnnotations()) {
                if (anno.annotationType().getAnnotation(KeepAnnotation.class) != null)
                  keepers.add(anno);
              }
              annos = keepers.toArray(new Annotation[keepers.size()]);
            }
            manifest.methods.put(method, annos);
          }
        }
      }
      nextClass = nextClass.getSuperclass();
    }
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
