package xapi.dev.model;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.DeleterFor;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.Persistent;
import xapi.annotation.model.Serializable;
import xapi.annotation.model.ServerToClient;
import xapi.annotation.model.SetterFor;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.except.NotConfiguredCorrectly;
import xapi.model.api.Model;
import xapi.source.X_Source;
import xapi.source.api.IsType;
import xapi.util.X_Runtime;

public class ModelArtifact extends Artifact<ModelArtifact> {

  private static final long serialVersionUID = -4122808053849540655L;

  final Map<JMethod,Annotation[]> methods = new LinkedHashMap<JMethod,Annotation[]>();
  final String typeName;
  final Set<String> toGenerate = new HashSet<String>();

  protected ModelArtifact(final String typeName) {
    super(StandardLinkerContext.class);
    this.typeName = typeName;
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
    logger.log(Type.INFO, "Implementing model method "+ifaceMethod.getJsniSignature());
    toGenerate.add(xapi.dev.model.ModelGeneratorGwt.toSignature(ifaceMethod));
    applyAnnotations(logger, ifaceMethod, ifaceMethod.getAnnotations(), ctx);
  }

  void applyAnnotations(final TreeLogger logger, final JMethod method, final Annotation[] annos, final GeneratorContext ctx) {
    methods.put(method, annos);

  }

  Collection<JMethod> extractMethods(final TreeLogger logger, final SourceBuilder<ModelMagic> sb, final GeneratorContext ctx, final JClassType type) throws UnableToCompleteException {
    assert type.isInterface() != null;
    final ModelMagic models = sb.getPayload();
    final Map<String,JMethod> uniqueMethods = new LinkedHashMap<String,JMethod>();
    JMethod[] existing = type.getInheritableMethods();
    final Set<? extends JClassType> hierarchy = type.getFlattenedSupertypeHierarchy();
    if (ModelGeneratorGwt.allAbstract(existing)) {
      // still an interface; grab our root type.
      existing = models.getRootType(logger, ctx).getInheritableMethods();
    }
    for (final JMethod method : existing) {
      if (!method.isAbstract()) {
        uniqueMethods.put(xapi.dev.model.ModelGeneratorGwt.toSignature(method), method);
      }
    }
    final boolean debug = logger.isLoggable(Type.DEBUG);
    for (final JClassType next : hierarchy) {
      if (next.isInterface() != null) {
        sb.getClassBuffer().addInterfaces(next.getQualifiedSourceName());
        for (final JMethod method : next.getMethods()) {
          final String sig = ModelGeneratorGwt.toSignature(method);
          if (!uniqueMethods.containsKey(sig)) {
            uniqueMethods.put(sig, method);
          }
        }
      } else {
        for (final JMethod method : next.getMethods()) {
          final String sig = ModelGeneratorGwt.toSignature(method);
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
    return uniqueMethods.values();
  }

  public void build(final TreeLogger logger, final SourceBuilder<ModelMagic> builder, final GeneratorContext ctx,
    final JClassType type) throws UnableToCompleteException {

    final ModelMagic models = builder.getPayload();
    final ModelGenerator generator = new ModelGenerator(builder);
    // Step one; determine if we already have a concrete type or not.
    // TODO if JClassType is final, we need to wrap it using a delegate model.
    JClassType concrete;
//    ClassBuffer cb = builder.getClassBuffer();
    final JClassType root = models.getRootType(logger, ctx);
    if (type.isInterface() == null && type != root) {
      concrete = type;
      generator.setSuperClass(type.getQualifiedSourceName());
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
            "make sure you have xapi-gwt-model:sources.jar on classpath.");
          throw new UnableToCompleteException();
        }
        concrete = model;
        for (final JClassType supertype : concrete.getFlattenedSupertypeHierarchy()) {
          // Only interfaces explicitly extending Model become concrete.
          if (ModelGeneratorGwt.canBeSupertype(type, supertype)) {
            // prefer the concrete type with the most methods in common.
            concrete = supertype;
          }
        }
        if (concrete == null || concrete == model) {
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

    //This will probably become jsni, if we can avoid jso interface sickness...
    generator.createFactory(type.getQualifiedSourceName());

    final HasModelFields fieldMap = new HasModelFields();
    fieldMap.setDefaultSerializable(type.getAnnotation(Serializable.class));

    for (final JMethod method : methods.keySet()) {
        if (!toGenerate.contains(ModelGeneratorGwt.toSignature(method))) {
          logger.log(Type.TRACE, "Skipping method defined in supertype: "+method.getJsniSignature());
          continue;
        }


      final Annotation[] annos = methods.get(method);
      final String methodName = method.getName();
      String returnType = method.getReturnType().getQualifiedSourceName();
      final String params = ModelGeneratorGwt.typeToParameterString(method.getParameterTypes());

      // TODO: check imports if we are safe to use simple name.
      returnType = method.getReturnType().getSimpleSourceName();
      final IsType returns = X_Source.binaryToSource(method.getReturnType().getQualifiedBinaryName());
      final IsType[] parameters = ModelGeneratorGwt.toTypes(method.getParameterTypes());

      final GetterFor getter = method.getAnnotation(GetterFor.class);
      if (getter != null) {
        String name = getter.value();
        if (name.length() == 0) {
          name = ModelUtil.stripGetter(method.getName());
        }
        final ModelField field = fieldMap.getOrMakeField(name);
        field.setType(returnType);
        assert parameters.length == 0 : "A getter method cannot have parameters. " +
        		"Generated code requires using getter methods without args.  You provided "+method.getJsniSignature();
        grabAnnotations(logger, models, fieldMap, method, annos, type);
        field.addGetter(returns, methodName);
        continue;
      }
      final SetterFor setter = method.getAnnotation(SetterFor.class);
      if (setter != null) {
        String name = setter.value();
        if (name.length() == 0) {
          name = ModelUtil.stripSetter(method.getName());
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
          final MethodBuffer mb = generator.createMethod(returnType, methodName, params);
          implementSetter(logger, mb, method, models, fieldMap, annos);
        } else  if (isAction) {
          implementAction(logger,generator, method, models, annos);
        } else {
          final MethodBuffer mb = generator.createMethod(returnType, methodName, params);
          implementException(logger, mb, method);
        }
      } else {
        if (isGetter) {
          final String name = ModelUtil.stripGetter(method.getName());
          final ModelField field = fieldMap.getOrMakeField(name);
          field.setType(returnType);
          field.addGetter(returns, methodName);
          grabAnnotations(logger, models, fieldMap, method, annos, type);
        } else if (isSetter) {
          final MethodBuffer mb = generator.createMethod(returnType, methodName, params);
          implementSetter(logger, mb, method, models, fieldMap, annos);
        } else if (isAction){
          implementAction(logger,generator, method, models, annos);
        } else {
          final MethodBuffer mb = generator.createMethod(returnType, methodName, params);
          implementException(logger, mb, method);
        }
      }
    }
    generator.generateModel(X_Source.toType(builder.getPackage(), builder.getClassBuffer().getSimpleName()), fieldMap);
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
      fieldName = ModelGeneratorGwt.fieldName(method, models);
    } else {
      fieldName = name.value();
      if (X_Runtime.isDebug()) {
        logger.log(Type.TRACE, "Named method "+method.getJsniSignature()+" "
          +fieldName+", from @Named attribute.  Heuristic name: "+ModelGeneratorGwt.fieldName(method, models));
      }
    }
    if ("".equals(fieldName)) {
      fieldName = method.getName();
    }
    final ModelField field = fields.getOrMakeField(fieldName);


    if (field.getPersistent() == null) {
      field.setPersistent(persist);
    } else {
      assert persist == null || (persist.patchable() == field.getPersistent().patchable()):
      "Model annotation mismatch! Field "+field.getName()+" of type " + type.getQualifiedSourceName() +
      " contained multiple @Persistent annotations which did not match.  " +
      "You may have to override an annotated supertype method with the correct " +
      "@Persistent annotation.";
    }

    if (field.getSerializable() == null) {
      field.setSerializable(serial);
    } else {
//      assert serial == null ||
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
      X_Source.binaryToSource(method.getReturnType().getQualifiedBinaryName())
      , name,
      ModelGeneratorGwt.toTypes(method.getParameterTypes())
      ).fluent = isFluent;
    if (!isFluent && !isVoid) {
      mb.println("var value = getProperty(\""+name+"\");");
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

}
