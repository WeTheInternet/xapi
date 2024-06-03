package xapi.dev.model;

import xapi.annotation.model.*;
import xapi.bytecode.impl.BytecodeUtil;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.lang.gen.ApiGeneratorTools;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.In1;
import xapi.fu.X_Fu;
import xapi.fu.itr.SizedIterable;
import xapi.gwt.model.service.ModelServiceGwt;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelMethodType;
import xapi.model.impl.ModelNameUtil;
import xapi.model.impl.ModelUtil;
import xapi.source.api.*;
import xapi.source.impl.*;
import xapi.util.X_Runtime;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;

import static xapi.dev.model.ModelGeneratorGwt.*;
import static xapi.gwt.model.service.ModelServiceGwt.REGISTER_CREATOR_METHOD;
import static xapi.bytecode.impl.BytecodeUtil.binaryToSource;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.Transferable;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.core.ext.typeinfo.*;

@Transferable
public class ModelArtifact extends Artifact<ModelArtifact> {

  private static final long serialVersionUID = -4122808053849540655L;

  final Map<JMethod,Annotation[]> methods = new LinkedHashMap<>();
  String typeName;
  String typeClass;
  final Set<String> toGenerate = new LinkedHashSet<String>();
  final HasModelFields fieldMap = new HasModelFields();

  private boolean reused;

  private String[] properties; // make this lazy

  private Persistent defaultPersistence;

  private Serializable defaultSerialization;
  private final StringTo<In1<ModelField>> listTypes;
  private final StringTo<In1<ModelField>> mapTypes;

  protected ModelArtifact(ApiGeneratorTools<?> tools, final String typeName, final String typeClass) {
    super(StandardLinkerContext.class);
    this.typeName = typeName;
    this.typeClass = typeClass;
    listTypes = initLists(tools);
    mapTypes = initMaps(tools);

  }

  protected StringTo<In1<ModelField>> initMaps(ApiGeneratorTools<?> tools) {
    final StringTo<In1<ModelField>> map = X_Collect.newStringMap(In1.class);
    final In1<ModelField> setMapType = In1.from2(ModelField::setMapType, true);
    tools.allMapTypes()
          .forAll(map::put, setMapType);
    return map;
  }

  protected StringTo<In1<ModelField>> initLists(ApiGeneratorTools<?> tools) {
    final StringTo<In1<ModelField>> map = X_Collect.newStringMap(In1.class);
    final In1<ModelField> setListType = In1.from2(ModelField::setListType, true);
    tools.allListTypes()
          .forAll(map::put, setListType);
    return map;
  }

  @Override
  public int hashCode() {
    return typeName.hashCode();
  }

  @Override
  protected int compareToComparableArtifact(final ModelArtifact o) {
    return typeName.compareTo(o.typeName);
  }

  @Override
  protected Class<ModelArtifact> getComparableArtifactType() {
    return ModelArtifact.class;
  }

  Annotation[] extractAnnotations(final TreeLogger logger, final JClassType type, final JMethod ifaceMethod,
    final JMethod classMethod, final GeneratorContext ctx) {
    final Map<Class<?>,Annotation> unique = new LinkedHashMap<Class<?>,Annotation>();
    // Prefer annotation on classes before interfaces.
    for (final Annotation classAnno : classMethod.getAnnotations()) {
      unique.put(classAnno.annotationType(), classAnno);
    }
    // Transverse supertypes
    JClassType next = type.getSuperclass();
    while (next != null) {
      final JMethod method = next.findMethod(ifaceMethod.getName(), ifaceMethod.getParameterTypes());
      if (method != null) {
        for (final Annotation classAnno : method.getAnnotations()) {
          unique.put(classAnno.annotationType(), classAnno);
        }
      }
      next = next.getSuperclass();
    }
    for (final Annotation ifaceAnno : ifaceMethod.getAnnotations()) {
      unique.put(ifaceAnno.annotationType(), ifaceAnno);
    }
    return unique.values().toArray(new Annotation[unique.size()]);
  }

  void implementMethod(final TreeLogger logger, final JMethod ifaceMethod, final GeneratorContext ctx) {
    if (!ModelMagic.shouldIgnore(ifaceMethod)) {
      logger.log(logLevel(), "Implementing model method "+ifaceMethod.getJsniSignature());
      toGenerate.add(xapi.dev.model.ModelGeneratorGwt.toSignature(ifaceMethod));
      applyAnnotations(logger, ifaceMethod, ifaceMethod.getAnnotations(), ctx);
    }
  }

  private Type logLevel() {
    return Type.TRACE;
  }

  void applyAnnotations(final TreeLogger logger, final JMethod method, final Annotation[] annos, final GeneratorContext ctx) {
    if (!ModelMagic.shouldIgnore(method)) {
      methods.put(method, annos);
    }

  }

  Collection<JMethod> extractMethods(final TreeLogger logger, ModelMagic models, final GeneratorContext ctx, final JClassType type) throws UnableToCompleteException {
    assert type.isInterface() != null;
    final Map<String,JMethod> uniqueMethods = new LinkedHashMap<String,JMethod>();
    JMethod[] existing = type.getInheritableMethods();
    final Set<? extends JClassType> hierarchy = type.getFlattenedSupertypeHierarchy();
    if (type.isInterface() != null || allAbstract(existing)) {
      // still an interface; grab our root type.
      existing = models.getRootType(logger, ctx).getInheritableMethods();
    }
    for (final JMethod method : existing) {
      if (isAllowed(method) && !method.isAbstract()) {
        uniqueMethods.put(toSignature(method), method);
      }
    }
    final boolean debug = logger.isLoggable(Type.DEBUG);
    for (final JClassType next : hierarchy) {
      if (next.isInterface() != null) {
        for (final JMethod method : next.getMethods()) {
          if (isAllowed(method)) {
            final String sig = toSignature(method);
            if (!uniqueMethods.containsKey(sig)) {
              uniqueMethods.put(sig, method);
            }
          }
        }
      } else {
        for (final JMethod method : next.getMethods()) {
          if (isAllowed(method)) {
            final String sig = toSignature(method);
            if (uniqueMethods.containsKey(sig)) {
              if (debug) {
                logger.log(Type.WARN, "Found multiple model methods for " + type.getName() + "::" + sig +
                  "; preferring method from " + uniqueMethods.get(sig).getEnclosingType().getName());
              }
            } else {
              uniqueMethods.put(sig, method);
            }
          }
        }
      }
    }
    return uniqueMethods.values();
  }

  private boolean isAllowed(JMethod method) {
    if (method.isDefaultMethod()) {
      return false;
    }
    return !ModelMagic.shouldIgnore(method);
  }

  public void generateModelClass(final TreeLogger logger, final SourceBuilder<ModelMagic> builder, final GeneratorContext ctx,
    final JClassType type) throws UnableToCompleteException {

    final ModelMagic models = builder.getPayload();
    final ModelGenerator generator = new ModelGenerator(builder);
    // Step one; determine if we already have a concrete type or not.
    // TODO if JClassType is final, we need to wrap it using a delegate model.
    JClassType concrete;
    final JClassType root = models.getRootType(logger, ctx);
    final String modelType = type.getQualifiedSourceName();
    final IsType sourceType = toTypeBounds(true, type)[0];

    if (sourceType.ifParameterized().isPresent()) {
      final IsParameterizedType parameterized = sourceType.ifParameterized().get();
      for (IsTypeArgument arg : parameterized.getBounds()) {
        StringBuilder b = new StringBuilder();
        serializeParameter(arg, b, builder);
        builder.getClassBuffer().addGenerics(b.toString());
      }
    }

    if (type.isInterface() != null) {
      builder.addInterfaces(type.getParameterizedQualifiedSourceName());
    } // TODO: move the other setSuperType to here... for now, we only use interfaces
//    Map<String, String> parameterized = new LinkedHashMap<>();
//    for (JClassType t : type.getFlattenedSupertypeHierarchy()) {
//      if (t.isInterface() != null) {
////        if (t != type) {
//
//          builder.addInterfaces(t.getParameterizedQualifiedSourceName());
////        }
//      }
//    }

    if (type.isInterface() == null && type != root) {
      concrete = type;
      generator.setSuperClass(modelType);
    } else {
        // We have an interface on our hands; search for an existing or
        // buildable model.
        // search for a supertype to inherit. Anything that extends Model,
        // does not implement any method we do implement, preferred type
        // being the one that implements the most interfaces possible
        final JClassType model;
        try {
          model = ctx.getTypeOracle().getType(Model.class.getName());
        } catch (final NotFoundException e) {
          logger.log(Type.ERROR, "Cannot load " + Model.class.getName() + "; " +
            "make sure you have xapi-model-gwt:sources.jar on classpath.");
          throw new UnableToCompleteException();
        }
        assert model != null : "Could not find " + Model.class.getCanonicalName() + " on the classpath";
        concrete = model;
        for (final JClassType supertype : concrete.getFlattenedSupertypeHierarchy()) {
          // Only interfaces explicitly extending Model become concrete.
          if (canBeSupertype(type, supertype)) {
            // LATER: prefer the concrete type with the most methods in common (check before set).
            concrete = supertype;
          }
        }
        if (concrete == model) {
          concrete = models.getRootType(logger, ctx);
          generator.setSuperClass(concrete.getQualifiedSourceName());
        } else {
          // We have to make sure this concrete supertype is created.
          if (!models.hasModel(concrete.getQualifiedSourceName())) {
            // Concrete type is not cached.  Build it now.
            final RebindResult result = ModelGeneratorGwt.execImpl(logger, ctx, concrete.getQualifiedSourceName());
            generator.setSuperClass(result.getResultTypeName());
          }
        }
      }

    //This should turn into properly annotated JsInterop types
    generator.createFactory(type.getQualifiedSourceName());

    fieldMap.setDefaultSerializable(type.getAnnotation(Serializable.class));

    final ClassBuffer out = builder.getClassBuffer();

    final String register = out.addImportStatic(ModelServiceGwt.class, REGISTER_CREATOR_METHOD);

    final String typeName = guessTypeName(type);

    out.createField(String.class, "MODEL_TYPE")
      .makePublic()
      .makeStatic()
      .makeFinal()
      .setInitializer(register+"("+
          modelType+".class, "
          + "\""  + typeName + "\", "
          + out.getSimpleName()+".class, "
          + out.getSimpleName()+"::newInstance"
      + ");"
      );

    out.createMethod("public static String register()")
      .println("return MODEL_TYPE;");

    final MethodBuffer getPropertyType = out
        .createMethod("public Class<?> getPropertyType(String name)")
        .println("switch (name) {")
        .indent();
    final MethodBuffer getPropertyNames = out
        .createMethod("public String[] getPropertyNames()")
        .println("return new String[]{")
        .indent();

    final JClassType modelInterface = ctx.getTypeOracle().findType(Model.class.getPackage().getName(), Model.class.getSimpleName());
    final Set<String> propertyNames = new LinkedHashSet<String>();
    final Set<JType> interestingTypes = new LinkedHashSet<JType>();
    final IsModel modelAnno = type.getAnnotation(IsModel.class);
    String idField = modelAnno == null ? "id" : modelAnno.key().value();
    Class idClass = modelAnno == null ? String.class : modelAnno.key().keyType();
    for (final JMethod method : methods.keySet()) {
      if (ModelMagic.shouldIgnore(method)) {
        continue;
      }
      final String propName = ModelMethodType.deducePropertyName(method.getName(), idField,
          method.getAnnotation(GetterFor.class), method.getAnnotation(SetterFor.class), method.getAnnotation(DeleterFor.class));
      propertyNames.add(propName);
      if (!toGenerate.contains(toSignature(method))) {
        logger.log(logLevel(), "Skipping method defined in supertype: "+method.getJsniSignature());
        continue;
      }

      final Annotation[] annos = methods.get(method);
      final String methodName = method.getName();
      final String returnType = getLoadableTypeName(method.getReturnType());
      final String params = typeToParameterString(method.getParameterTypes());

      final String simpleReturnType = out.addImport(method.getReturnType().getQualifiedSourceName());
      IsType returns = binaryToSource(method.getReturnType().getQualifiedBinaryName());
//      List<IsType> generics = getGenerics(method.getReturnType());
//      if (generics.size() > 0) {
      IsTypeArgument[] generics;
      if (method.getReturnType().isParameterized() != null) {
        // fixup the returns...
        returns = new ImmutableParameterizedType(null, returns, toTypeBounds(false, method.getReturnType().isParameterized().getTypeArgs()));
        generics = toTypeBounds(true, method.getReturnType().isParameterized().getTypeArgs());
      } else {
        generics = new IsTypeArgument[0];
      }
      final IsType[] parameters = toTypes(method.getParameterTypes());

      if (isInteresting(method.getReturnType(), modelInterface)) {
        interestingTypes.add(method.getReturnType());
      }

      final GetterFor getter = method.getAnnotation(GetterFor.class);
      if (getter != null) {
        String name = getter.value();
        if (name.length() == 0) {
          name = ModelNameUtil.stripGetter(method.getName());
        }
        final ModelField field = fieldMap.getOrMakeField(name);
        field.setType(returnType);
        assert parameters.length == 0 : "A getter method cannot have parameters. " +
        		"Generated code requires using getter methods without args.  You provided "+method.getJsniSignature();
        grabAnnotations(logger, models, fieldMap, method, annos, type);
        field.addGetter(sourceType, returns, name, methodName, method.getAnnotations(), generics);

        getPropertyType.println("case \""+propName+"\":")
          .indentln("return "+out.addImport(method.getReturnType().getQualifiedSourceName())+".class;");

        continue;
      }
      final SetterFor setter = method.getAnnotation(SetterFor.class);
      if (setter != null) {
        String name = setter.value();
        if (name.length() == 0) {
          name = ModelNameUtil.stripSetter(method.getName());
        }
        grabAnnotations(logger, models, fieldMap, method, annos, type);
        continue;
      }

      if (method.getAnnotation(DeleterFor.class) != null) {
        implementAction(logger,generator, method, models, annos);
        continue;
      }

      // No annotation.  We have to guess the type.

      final boolean isVoid = method.getReturnType().isPrimitive() == JPrimitiveType.VOID;
      final boolean isGetter = methodName.startsWith("get")
        || methodName.startsWith("is")
        || methodName.startsWith("has");
      boolean isSetter, isAction;
      if (isGetter) {
        assert !isVoid : "Cannot have a void return type with method name "+
            methodName+"; getter prefixes get(), is() and has() must return a type.";
        isSetter = false;
        isAction = false;

        getPropertyType.println("case \""+propName+"\":")
          .indentln("return "+out.addImport(method.getReturnType().getQualifiedSourceName())+".class;");

      } else {
        isSetter = methodName.startsWith("set")
          || methodName.startsWith("add")
          || methodName.startsWith("put")
          || methodName.startsWith("rem")
          || methodName.startsWith("remove");
        if (isSetter) {
          isAction = false;
        } else {
          isAction = true;
        }
      }

      if (isVoid) {
        // definitely a setter / action method.
        if (isSetter) {
          final MethodBuffer mb = generator.createMethod(simpleReturnType, methodName, params);
          implementSetter(logger, mb, method, models, fieldMap, annos);
        } else  if (isAction) {
          implementAction(logger,generator, method, models, annos);
        } else {
          final MethodBuffer mb = generator.createMethod(simpleReturnType, methodName, params);
          implementException(logger, mb, method);
        }
      } else {
        if (isGetter) {
          final String name = ModelNameUtil.stripGetter(method.getName());
          final ModelField field = fieldMap.getOrMakeField(name);
          field.setType(returnType);
          field.addGetter(sourceType, returns, name, methodName, method.getAnnotations(), generics);
          grabAnnotations(logger, models, fieldMap, method, annos, type);
        } else if (isSetter) {
          final MethodBuffer mb = generator.createMethod(simpleReturnType, methodName, params);
          implementSetter(logger, mb, method, models, fieldMap, annos);
        } else if (isAction){
          implementAction(logger,generator, method, models, annos);
        } else {
          final MethodBuffer mb = generator.createMethod(simpleReturnType, methodName, params);
          implementException(logger, mb, method);
        }
      }
    }
    for (final String propName : propertyNames) {
      getPropertyNames.println("\""+propName+"\",");
    }
    if (properties == null) {
      this.properties = propertyNames.toArray(new String[propertyNames.size()]);
    }
    getPropertyNames.outdent().println("};");
    getPropertyType
      .outdent()
      .println("}")
      .println("return super.getPropertyType(name);");

    // The type we are currently generating is not considered interesting, as we have already seen it.
    implementInterestingTypes(type, out, modelInterface, interestingTypes);

    generator.generateModel(sourceType, BytecodeUtil.toType(builder.getPackage(), builder.getClassBuffer().getSimpleName()), fieldMap);
  }

  private void serializeParameter(
      IsTypeArgument arg,
      StringBuilder b,
      SourceBuilder<ModelMagic> builder
  ) {

    String argName = arg.getQualifiedName();
    if (argName.indexOf('.') != -1) {
      argName = builder.addImport(argName);
    }
    b.append(argName);
    if (arg instanceof HasBounds) {
      final SizedIterable<? extends IsTypeArgument> subBounds = ((HasBounds) arg).getBounds();
      if (subBounds.isNotEmpty()) {
        b.append(" extends ");
        boolean first = true;
        for (IsTypeArgument subBound : subBounds) {
          if (first) {
            first = false;
          } else {
            b.append(" & ");
          }
          serializeParameter(subBound, b, builder);
        }
      }
    }
  }

  private List<IsType> getGenerics(JType type) {
    final JParameterizedType genericType = type.isParameterized();
    List<IsType> generics = new ArrayList<>();
    if (genericType != null) {
      for (JClassType parameter : genericType.getTypeArgs()) {
        IsType raw = binaryToSource(parameter.getQualifiedBinaryName());
        final JParameterizedType param = parameter.isParameterized();
        if (param != null) {
          // nested parameter...
          raw = raw.withTypeBounds(toTypeBounds(false, param.getTypeArgs()));
        }
        // leaving this here, in case there
        generics.add(raw);
      }
    }
    return generics;
  }

  private IsTypeArgument[] toTypeBounds(boolean declaration, JType ... typeArgs) {
    final IsTypeArgument[] result = new IsTypeArgument[typeArgs.length];
    for (int i = 0; i < typeArgs.length; i++) {
      final JWildcardType wildcard = typeArgs[i].isWildcard();
      if (wildcard != null) {
        boolean noLower = X_Fu.isEmpty(wildcard.getLowerBounds());
        if (isEmptyWildcard(wildcard.getUpperBounds())) {
          if (noLower) {
            // plain ?
            result[i] = new ImmutableWildcardArgument(null);
          } else {
            final IsTypeArgument[] lowers = toTypeBounds(declaration, wildcard.getLowerBounds());
            result[i] = new ImmutableWildcardArgument(null, true, lowers);
          }
        } else {
          // There are upper bounds
            final IsTypeArgument[] uppers = toTypeBounds(declaration, wildcard.getUpperBounds());
          result[i] = new ImmutableWildcardArgument(null, uppers);
        }
      } else {
        final JTypeParameter typeParam = typeArgs[i].isTypeParameter();
        if (typeParam == null) {
          // Check for array type...
          final JArrayType arrayType = typeArgs[i].isArray();
          if (arrayType == null) {
            final JClassType arg = typeArgs[i].isClassOrInterface();

            // The argument is a class type... check if it's parameterized or not.
            final JParameterizedType params = arg.isParameterized();
            final String simpleName = enclosingName(arg);
            final String pkg = arg.getPackage().getName();
            IsType raw = new ImmutableType(pkg, simpleName);
            if (params == null) {
              // Try for a generic type...
              final JGenericType generic = arg.isGenericType();
              if (generic == null) {
                // nope, raw type...
                result[i] = new ImmutableClassArgument(null, raw, 0);
              } else {
                final JTypeParameter[] typeParams = generic.getTypeParameters();
                final IsTypeArgument[] bounds = toTypeBounds(declaration, typeParams);
                result[i] = new ImmutableParameterizedType(null, raw, bounds);
              }
            } else {
              // child is a parameterized type as well... recurse
              final IsTypeArgument[] args = toTypeBounds(declaration, params.getTypeArgs());
              result[i] = new ImmutableClassArgument(null, raw, 0, args);
            }

          } else {
            IsTypeArgument component = toTypeBounds(declaration, arrayType.getComponentType())[0];
            final IsType array = component.getArrayType();
            result[i] = new ImmutableClassArgument(null, array, 0);
          }
        } else {
          // we have a type parameter.  Convert to just use the name ?
          // ugh.  should check where this type parameter is defined, and act accordingly
          final IsType raw = new ImmutableType("", typeParam.getName());
          final IsTypeArgument[] bounds = declaration ? toTypeBounds(declaration, typeParam.getBounds()) : new IsTypeArgument[0];
          result[i] = new ImmutableClassArgument(new ImmutableTypeParameter(typeParam.getName()), raw, 0, bounds);
        }
      }
    }
    return result;
  }

  private boolean isEmptyWildcard(JClassType[] upperBounds) {
    if (X_Fu.isEmpty(upperBounds)) {
      return true;
    }
    return upperBounds.length == 1 && upperBounds[0].getQualifiedSourceName().equals(Object.class.getName());
  }

  private String enclosingName(JClassType arg) {
    final JClassType enclosing = arg.getEnclosingType();
    if (enclosing == null) {
      return arg.getSimpleSourceName();
    }
    String enc = enclosingName(arg.getEnclosingType());
    return enc + "." + arg.getSimpleSourceName();
  }

  /**
   * @param type
   * @return
   */
  private String guessTypeName(final JClassType type) {
    final IsModel model = type.getAnnotation(IsModel.class);
    if (model != null && model.modelType().length() > 0) {
      return model.modelType();
    }
    return ModelUtil.guessModelType(type.getSimpleSourceName());
  }

  /**
   * Print magic method initialization for all interesting types;
   * <p>
   * Currently, this will call Array.newInstance(Component.class, 0) for all array types,
   * thus initializing the model Class's ability to instantiate the arrays it requires,
   * plus calls X_Model.register(DependentModel.class) on all model types used as fields of
   * the current model type; thus ensuring all children will be fully de/serializable.
   *
   */
  private void implementInterestingTypes(final JClassType type, final ClassBuffer out, final JClassType modelInterface,
      final Set<JType> interestingTypes) {
    interestingTypes.remove(type);
    if (interestingTypes.isEmpty()) {
      return;
    }
    out
      .outdent()
      .println("// Initialize our referenced array / model types")
      .println("static {")
      .indent();
    for (JType interestingType : interestingTypes) {
      if (interestingType.isArray() != null) {
        // Print a primer for runtime array reflection.
        final JType componentType = interestingType.isArray().getComponentType();
        final String array = out.addImport(Array.class);
        String component = out.addImport(componentType.getQualifiedSourceName());
        interestingType = componentType;
        while (interestingType.isArray() != null) {
          interestingType = interestingType.isArray().getComponentType();
          component += "[]";
        }

        out.println(array+".newInstance("+component+".class, 0);");
      }
      final JClassType asClass = interestingType.isClassOrInterface();
      if (asClass != null) {
        if (asClass.isAssignableTo(modelInterface)) {
          // We have a model type; so long as it is not the same type that we are currently generating,
          // we want to print X_Model.register() for the given type, so we can inherit all model types
          // that are referenced as fields of the current model type.
          if (!type.getName().equals(asClass.getName())) {
            final String referencedModel = out.addImport(asClass.getQualifiedSourceName());
            final String X_Model = out.addImport(X_Model.class);
            out.println(X_Model+".register("+referencedModel+".class);");
          }
        }
      }
    }
    out.outdent().println("}");
  }

  /**
   * @param returnType
   * @param modelInterface
   * @return
   */
  private boolean isInteresting(final JType returnType, final JClassType modelInterface) {
    if (returnType.isArray() != null) {
      // All arrays are interesting, as we must prime java.lang.reflect.Array for runtime support
      return true;
    }
    final JClassType asClass = returnType.isClassOrInterface();
    if (asClass == null) {
      return false;
    }
    // All model classes are interesting.
    return asClass.isAssignableTo(modelInterface);
  }

  /**
   * @param returnType
   * @return
   */
  private String getLoadableTypeName(JType returnType) {
    if (returnType.isArray() != null) {
      final StringBuilder b = new StringBuilder();
      while(returnType.isArray() != null) {
        b.append("[");
        returnType = returnType.isArray().getComponentType();
      }
      return b
            .append("L")
            .append(returnType.getQualifiedSourceName())
            .append(";")
            .toString();
    }
    return returnType.getQualifiedSourceName();
  }

  private void implementException(final TreeLogger logger, final MethodBuffer mb, final JMethod method) {
    logger.log(Type.WARN, "Unable to implement model method for " +
      method.getJsniSignature() + "; " +
      "inserting a runtime exception." +
          " Either annotate the method with an xapi.annotation.model, " +
          " or ensure it conforms to javabean naming conventions get___, set___");
    mb.println("throw new RuntimeException(\"");
    mb.println("Model method "+method.getEnclosingType().getSimpleSourceName()+"."+ method.getName()+" not annotated correctly");
    mb.println("\");");
  }

  private void grabAnnotations(final TreeLogger logger, final ModelMagic models, final HasModelFields fields, final JMethod method,  final Annotation[] annos, final JClassType type) {

    GetterFor getter = method.getAnnotation(GetterFor.class);
    ClientToServer c2s = method.getAnnotation(ClientToServer.class);
    ServerToClient s2c = method.getAnnotation(ServerToClient.class);
    Persistent persist = method.getAnnotation(Persistent.class);
    Serializable serial = method.getAnnotation(Serializable.class);
    Named name = method.getAnnotation(Named.class);

    for (final Annotation annotation : annos) {
      if (getter == null && annotation.annotationType() == GetterFor.class) {
        getter = (GetterFor)annotation;
      } else if (serial == null && annotation.annotationType() == Serializable.class) {
        serial = (Serializable)annotation;
        if (c2s == null) {
          c2s = serial.clientToServer();
        }
        if (s2c == null) {
          s2c = serial.serverToClient();
        }
      } else if (persist == null && annotation.annotationType() == Persistent.class) {
        persist = (Persistent)annotation;
      } else if (c2s == null && annotation.annotationType() == ClientToServer.class) {
        c2s = (ClientToServer)annotation;
      } else if (s2c == null && annotation.annotationType() == ServerToClient.class) {
        s2c = (ServerToClient)annotation;
      } else if (name == null && annotation.annotationType() == Named.class) {
        name = (Named)annotation;
      }
    }

    String fieldName;
    if (name == null) {
      fieldName = fieldName(method, models);
    } else {
      fieldName = name.value();
      if (X_Runtime.isDebug()) {
        logger.log(logLevel(), "Named method "+method.getJsniSignature()+" "
          +fieldName+", from @Named attribute.  Heuristic name: "+fieldName(method, models));
      }
    }
    if ("".equals(fieldName)) {
      fieldName = method.getName();
    }
    final boolean exists = fields.fields.containsKey(fieldName);
    final ModelField field = fields.getOrMakeField(fieldName);
    if (!exists) {
      field.setPersistent(defaultPersistence);
      field.setSerializable(defaultSerialization);
    }


    if (field.getPersistent() == null) {
      field.setPersistent(persist);
    } else {
      assert persist == null || persist == defaultPersistence ||
          (
              persist.strategy() == field.getPersistent().strategy() &&
              persist.patchable() == field.getPersistent().patchable()
          ):
      "Model annotation mismatch! Field "+field.getName()+" of type " + type.getQualifiedSourceName() +
      " contained multiple @Persistent annotations which did not match.  " +
      "You may have to override an annotated supertype method with the correct " +
      "@Persistent annotation.";
    }

    if (field.getSerializable() == null) {
      field.setSerializable(serial);
    } else {
//      assert serial == null || serial == defaultSerialization ||
//        ( // this block is all assert, so it will compile out of production.
//          serial.clientToServer() == field.getSerializable().clientToServer() &&
//          serial.serverToClient() == field.getSerializable().serverToClient() &&
//          serial.obfuscated() == field.getSerializable().obfuscated()
//          ) : "Model annotation mismatch! Field "+field.getName()+" contained " +
//          		"multiple @Serializable annotations which did not match.  You may " +
//          		"have to override an annotated supertype method with the correct " +
//          		"@Serializable annotation.";
    }
    if (field.getServerToClient() == null) {
      field.setServerToClient(s2c);
    } else {
      assert s2c == null || s2c.enabled() == field.getServerToClient().enabled()
        : "Model annotation mismatch! Field "+field.getName()+" was marked as " +
        "both " +"serverToClient" + " enabled and disabled.  Please correct this ambiguity;" +
        " your model is now undeterministic and may break unexpectedly.";
    }
    if (field.getClientToServer() == null) {
      field.setClientToServer(c2s);
    } else {
      assert c2s == null || c2s.enabled() == field.getClientToServer().enabled()
        : "Model annotation mismatch! Field "+field.getName()+" was marked as " +
        		"both " +"clientToServer" +" enabled and disabled.  Please correct this ambiguity;" +
        		" your model is now undeterministic and may break unexpectedly.";
    }

    final String simpleType = method.getReturnType().getErasedType().getSimpleSourceName();
    listTypes.use(simpleType, callback->{
      if (callback == null) {
        field.setListType(true);
      } else {
        callback.in(field);
      }
    });
    mapTypes.use(simpleType, callback->{
      if (callback == null) {
        field.setMapType(true);
      } else {
        callback.in(field);
      }
    });
  }

  private void implementAction(final TreeLogger logger,final ModelGenerator generator, final JMethod method, final ModelMagic models, final Annotation[] annos) {
    final boolean fluent = ModelGeneratorGwt.isFluent(method);
    final JPrimitiveType primitive = method.getReturnType().isPrimitive();
    if (primitive != JPrimitiveType.VOID) {
      if (!fluent) {
        //non-fluent, non-void return type is not an action
        //TODO change this!
//        implementGetter(logger, mb, method, models, annos, method.getReturnType().getSimpleSourceName());
        logger.log(Type.ERROR, "No getter for "+method.getJsniSignature()+"; " +
        		"If your type does not use javabean getField() naming conventions, " +
        		"then you MUST annotate a getter field with @GetterField");
      }
      return;
    }
    final MethodBuffer mb = generator.createMethod(method.getReturnType().getQualifiedSourceName(),
      method.getName(), ModelGeneratorGwt.typeToParameterString(method.getParameterTypes()));

    if (method.getName().equals("clear")) {
      //implement clear
    }

    if (fluent) {
      mb.println("return this;");
    }
  }

  private void implementSetter(final TreeLogger logger, final MethodBuffer mb,
    final JMethod method, final ModelMagic manifest, final HasModelFields models, final Annotation[] annos) {


    final JPrimitiveType primitive = method.getReturnType().isPrimitive();
    final boolean isVoid = primitive == JPrimitiveType.VOID;
    final boolean isFluent = ModelGeneratorGwt.isFluent(method);
    final String name = ModelGeneratorGwt.fieldName(method, manifest);
    final ModelField field = models.getOrMakeField(name);
    field.addSetter(
      BytecodeUtil.binaryToSource(method.getReturnType().getQualifiedBinaryName()),
      name, method.getName(),
      method.getAnnotations(),
      ModelGeneratorGwt.toTypes(method.getParameterTypes())
      ).fluent = isFluent;
    if (!isFluent && !isVoid) {
      mb.println(mb.addImport(field.getType())+" value = getProperty(\""+name+"\");");
    }
    final JParameter[] params = method.getParameters();
    if (params.length != 1) {
      throw new NotConfiguredCorrectly("A setter method, "+method.getJsniSignature()+" must have exactly one parameter");
    }
    mb.println("setProperty(\""+name+"\", A);");
    if (!isVoid) {
      mb.println("return "+(isFluent ? "this;" : "value;"));
    }
  }

  public void setReused() {
    this.reused = true;
  }

  /**
   * @return -> reused
   */
  public boolean isReused() {
    return reused;
  }

  public String getTypeName() {
    return typeName;
  }

  public String getTypeClass() {
    return typeClass;
  }

  /**
   * @return -> properties
   */
  public String[] getProperties() {
    return properties;
  }

  /**
   * @param logger
   * @param type
   */
  public void applyDefaultAnnotations(final TreeLogger logger, final com.google.gwt.core.ext.typeinfo.JClassType type) {
    final IsModel isModel = type.getAnnotation(IsModel.class);
    if (isModel != null) {
      typeName = isModel.modelType();
      defaultPersistence = isModel.persistence();
      defaultSerialization = isModel.serializable();
      if (isModel.propertyOrder().length > 0) {
        properties = isModel.propertyOrder();
      }
    }
  }

}
