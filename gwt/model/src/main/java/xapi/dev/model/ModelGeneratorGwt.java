package xapi.dev.model;

import xapi.annotation.model.DeleterFor;
import xapi.annotation.model.FieldName;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.Serializable;
import xapi.annotation.model.SetterFor;
import xapi.annotation.reflect.Fluent;
import xapi.dev.source.CharBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.model.api.ModelDeserializationContext;
import xapi.model.api.ModelSerializationContext;
import xapi.model.api.ModelSerializer;
import xapi.source.X_Source;
import xapi.source.api.CharIterator;
import xapi.source.api.IsType;
import xapi.util.X_Properties;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ModelGeneratorGwt extends IncrementalGenerator{


  private static final String MODEL_PACKAGE = "xapi.model";
  static final boolean MINIFY = Boolean.parseBoolean(X_Properties.getProperty("xapi.dist", "false"));

  @Override
  public RebindResult generateIncrementally(final TreeLogger logger, final GeneratorContext context, final String typeName)
    throws UnableToCompleteException {
    ModelMagic.initialize();
    return execImpl(logger, context, typeName);
  }

  @Override
  public long getVersionId() {
    return 0;
  }

  static boolean isFluent(final JMethod method) {
    final JClassType iface = method.getReturnType().isClassOrInterface();
    if (iface == null) {
      return false;
    }
    if (
        method.getEnclosingType().isAssignableTo(iface)
        || iface.isAssignableTo(method.getEnclosingType())
        ) {
      // Returning this would be allowed.
      // However, we should guard against methods that may actually want to return a field
      // that is the same type as itself.
      final Fluent fluent = method.getAnnotation(Fluent.class);
      if (fluent != null) {
        return fluent.value();
      }
      // TODO: check if there is a single parameter type which is also compatible,
      // and throw an error telling the user that they must specify @Fluent(true) or @Fluent(false)
      // Because the method signature is ambiguous
      return true;
    }
    return false;
  }

  static String returnType(final JMethod method, final ModelMagic models) {
    // TODO make sure simple name is allowed here.
    return method.getReturnType().getSimpleSourceName();
  }

  static String fieldName(final JMethod method, final ModelMagic models) {
    final FieldName field = method.getAnnotation(FieldName.class);
    if (field != null) {
      return field.debugName().length() > 0
        // TODO && models.isDebugMode
        ? field.debugName() : field.value();
    }
    final GetterFor getter = method.getAnnotation(GetterFor.class);
    if (getter != null) {
      return getter.value();
    }
    final SetterFor setter = method.getAnnotation(SetterFor.class);
    if (setter != null) {
      return setter.value();
    }
    final DeleterFor deleter = method.getAnnotation(DeleterFor.class);
    if (deleter != null) {
      return deleter.value();
    }
    // No annotations, we have to guess.
    final String name = method.getName();
    if (
      name.matches("(get|set|has|put|add|rem)[A-Z].*")
    ) {
      return Character.toLowerCase(name.charAt(3)) + name.substring(4);
    } else if (name.startsWith("is")) {
      return Character.toLowerCase(name.charAt(2)) + name.substring(3);
    } else if (name.startsWith("remove")) {
      return Character.toLowerCase(name.charAt(6)) + name.substring(7);
    }
    return name;
  }

  static boolean canBeSupertype(final com.google.gwt.core.ext.typeinfo.JClassType subtype, final com.google.gwt.core.ext.typeinfo.JClassType supertype) {
    return
      supertype.isInterface() == null &&
      !supertype.isFinal() &&
      supertype.isAssignableTo(subtype);

  }

  static String typeToParameterString(final JType[] parameterTypes) {
    if (parameterTypes.length == 0) {
      return "";
    }
    final StringBuilder b = new StringBuilder()
      .append(parameterTypes[0].getQualifiedSourceName())
      .append(" A");
    assert parameterTypes.length < 26: "Cannot have more than 26 parameters";
    for (int i = 1, m = parameterTypes.length; i < m; i++) {
      b
      .append(", ")
      .append(parameterTypes[i].getQualifiedSourceName())
      .append(" ")
      .append((char)('A'+i))
      ;
    }
    return b.toString();
  }

  static String typeToSignature(final JType[] parameterTypes) {
    if (parameterTypes.length == 0) {
      return "";
    }
    final StringBuilder b = new StringBuilder()
    .append(parameterTypes[0].getJNISignature());
    for (int i = 1, m = parameterTypes.length; i < m; i++) {
      b.append(parameterTypes[i].getJNISignature());
    }
    return b.toString();
  }

  public static boolean allAbstract(final JMethod[] existing) {
    for (final JMethod method : existing) {
      if (!method.isAbstract()) {
        return false;
      }
    }
    return true;
  }

  public static String toSignature(final JMethod method) {
    return method.getName()+"("+typeToSignature(method.getParameterTypes())+")";
  }

  public static RebindResult execImpl(final TreeLogger logger, final GeneratorContext ctx,
    final String typeName) throws UnableToCompleteException {

    final JClassType type = ctx.getTypeOracle().findType(typeName.replace('$', '.'));
    if (type == null) {
      logger.log(Type.ERROR, "Unable to find source for model interface type "+typeName);
      throw new UnableToCompleteException();
    }

    // Step one; see if this model already exists...
    final ModelMagic magic = ModelMagic.active.get();
    final String mangledName = magic.mangleName(type.getQualifiedSourceName(), MINIFY);
    final String fqcn = MODEL_PACKAGE+"."+mangledName;
    if (ctx.isGeneratorResultCachingEnabled() && ctx.tryReuseTypeFromCache(typeName)) {
      return new RebindResult(RebindMode.USE_PARTIAL_CACHED, fqcn);
    }
    if (!type.isAbstract()) {
      // TODO Only generate a provider class if the type itself is not concrete
      return new RebindResult(RebindMode.USE_EXISTING, typeName);
    }
    final PrintWriter pw = ctx.tryCreate(logger, MODEL_PACKAGE, mangledName);
    if (pw == null) {
      // TODO for injectors this type may exist but be stale.
      // Use an @Generated annotation to do a freshness check.
      return new RebindResult(RebindMode.USE_EXISTING, fqcn);
    }
    final ModelArtifact model = magic.getOrMakeModel(logger, ctx, type);
    final boolean isFinal = type.getSubtypes().length == 0;
    final SourceBuilder<ModelMagic> builder = new SourceBuilder<ModelMagic>(
      "public "+(isFinal?"final ":"")+"class "+mangledName
      );
    builder.setPayload(magic);
    builder.setPackage(MODEL_PACKAGE);

    // Step two; transverse type model.
    visitModelStructure(logger, ctx, typeName, type, magic, model, builder);

    // Step three, generate serialization protocols for this model type.
    generateSerializers(logger, ctx, typeName, type, magic, model, builder);

    // Step four, determine the fields we'll need to generate
    model.generateModelClass(logger, builder, ctx, type);
    final String src = builder.toString();
    final Type logLevel = logLevel();
    if (logger.isLoggable(logLevel)) {
      logger.log(logLevel, "Generated model class:\n"+src);
    }
    pw.println(src);
    ctx.commit(logger, pw);
    if (!model.isReused()) {
      ctx.commitArtifact(logger, model);
    }

    return new RebindResult(RebindMode.USE_ALL_NEW, MODEL_PACKAGE + "." + mangledName);
  }

  /**
   * @param logger
   * @param ctx
   * @param typeName
   * @param type
   * @param magic
   * @param model
   * @param builder
   */
  private static void generateSerializers(final TreeLogger logger, final GeneratorContext ctx, final String typeName, final JClassType type,
      final ModelMagic magic, final ModelArtifact model, final SourceBuilder<ModelMagic> builder) {
    final List<JMethod> toClient = new ArrayList<JMethod>();
    final List<JMethod> toServer = new ArrayList<JMethod>();
    for (final Entry<JMethod, Annotation[]> method : model.methods.entrySet()) {
      for (final Annotation anno : method.getValue()) {
        if (anno instanceof Serializable) {
          final Serializable serializable = (Serializable) anno;
          if (serializable.clientToServer().enabled()) {
            toServer.add(method.getKey());
          }
          if (serializable.serverToClient().enabled()) {
            toClient.add(method.getKey());
          }
        }
      }
    }
    if (toClient.isEmpty() && toServer.isEmpty()) {
      // TODO signal to use an empty serializer, ModelSerializer.DO_NOTHING
    }
    final String name = builder.getSimpleName()+"_Serializer";
    final PrintWriter pw = ctx.tryCreate(logger, builder.getPackage(), name);
    if (pw == null) {
      // Assume the result is not stale. Will need to be more careful when injected
      return;
    }

    final SourceBuilder out = new SourceBuilder("public final class "+name);
    out.setPackage(builder.getPackage());
    final String simpleType = out.getClassBuffer().addImport(typeName);
    final String serializerType = out.getClassBuffer().addImport(ModelSerializer.class);
    final String charIterator = out.addImport(CharIterator.class);
    final String serialContext = out.addImport(ModelSerializationContext.class);
    final String charBuffer = out.addImport(CharBuffer.class);
    final String deserialContext = out.addImport(ModelDeserializationContext.class);
    out.getClassBuffer().addInterface(serializerType+"<"+simpleType+">");

    final MethodBuffer modelToString = out.getClassBuffer()
          .createMethod("public " + charBuffer + " modelToString(" + simpleType+" model, " + serialContext + " ctx)");
    final MethodBuffer stringToModel = out.getClassBuffer()
        .createMethod("public " + simpleType + " modelFromString(" + charIterator + " model, " + deserialContext + " ctx)");

    if (toClient.isEmpty()) {
      stringToModel.returnValue("throw new " + out.getClassBuffer().addImport(UnsupportedOperationException.class)+"();");
    } else {
      // Print a deserializer for the model

      stringToModel.returnValue("null");
    }

    if (toServer.isEmpty()) {
      modelToString.returnValue("throw new " + out.getClassBuffer().addImport(UnsupportedOperationException.class)+"();");
    } else {
      // Print a serializer for the model

      modelToString.returnValue("null");
    }
    final String src = out.toString();
    final Type logLevel = logLevel();
    if (logger.isLoggable(logLevel)) {
      logger.log(logLevel, "Generated model serializer:\n"+src);
    }
    pw.append(src);
    ctx.commit(logger, pw);
  }

  private static Type logLevel() {
    return Type.DEBUG;
  }

  /**
   * @param logger
   * @param ctx
   * @param typeName
   * @param type
   * @param magic
   * @param model
   * @param builder
   * @throws UnableToCompleteException
   */
  private static void visitModelStructure(final TreeLogger logger, final GeneratorContext ctx, final String typeName,
      final JClassType type, final ModelMagic magic, final ModelArtifact model, final SourceBuilder<ModelMagic> builder)
      throws UnableToCompleteException {
    if (type.isInterface() == null) {
      // Client has specified their own base type.
      // Let's see if we're expected to generate any methods.
      if (type.isAbstract()) {
        // Find the methods the client didn't bother to implement.
        final Collection<JMethod> interfaceMethods = model.extractMethods(logger, builder, ctx, type);
        for (final JMethod ifaceMethod : interfaceMethods) {
          JMethod classMethod;
          try {
            classMethod = type.getMethod(ifaceMethod.getName(), ifaceMethod.getTypeParameters());
            // If this method exists, we still need to pull off annotations.
            final Annotation[] annos = model.extractAnnotations(logger, type, ifaceMethod, classMethod, ctx);
            model.applyAnnotations(logger, classMethod, annos, ctx);
          } catch (final NotFoundException e) {
            // client didn't bother to implement this method.
            // let's do it for them.
            model.implementMethod(logger, ifaceMethod, ctx);
          }
        }
      } else {
        assert false : "How do you have a non-abstract interface? " + type;
      }
    } else {
      final JClassType root = magic.getRootType(logger, ctx);
      final Set<String> toSigs = getImplementedSignatures(root.getInheritableMethods());
      for (final JMethod method :  model.extractMethods(logger, builder, ctx, type)) {
        if (toSigs.add(ModelGeneratorGwt.toSignature(method))) {
          model.implementMethod(logger, method, ctx);
        }
      }
    }
    builder.getClassBuffer()
        .println("static { ")
        // This is a magic method the compiler uses to record the correct Type.class <--> Type[].class,
        // to enable non-compile-time-literal use of Array.newInstance elsewhere in the code.
        .indentln(builder.addImport(Array.class)+".newInstance(" + type.getSimpleSourceName() + ".class, 0);")
        .println("}");
  }

  static Set<String> getImplementedSignatures(final JMethod[] inheritableMethods) {
    final Set<String> sigs = new LinkedHashSet<String>(inheritableMethods.length);
    for (final JMethod method : inheritableMethods) {
      if (!method.isAbstract()) {
        sigs.add(ModelGeneratorGwt.toSignature(method));
      }
    }
    return sigs;
  }

  public static IsType[] toTypes(final JType[] parameterTypes) {
    int i = parameterTypes.length;
    final IsType[] arr = new IsType[i];
    for (; i-->0;) {
      final JType t = parameterTypes[i];
      arr[i] = X_Source.binaryToSource(t.getQualifiedBinaryName());
    }
    return arr;
  }

}
