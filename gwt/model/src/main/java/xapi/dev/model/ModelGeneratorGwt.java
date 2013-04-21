package xapi.dev.model;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import xapi.annotation.model.DeleterFor;
import xapi.annotation.model.FieldName;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.SetterFor;
import xapi.dev.source.SourceBuilder;
import xapi.source.X_Source;
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

public class ModelGeneratorGwt extends IncrementalGenerator{


  private static final String MODEL_PACKAGE = "xapi.model";
  static final boolean MINIFY = Boolean.parseBoolean(X_Properties.getProperty("xapi.dist", "false"));

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext context, String typeName)
    throws UnableToCompleteException {
    ModelMagic.initialize();
    return execImpl(logger, context, typeName);
  }

  @Override
  public long getVersionId() {
    return 0;
  }

  static boolean isFluent(JMethod method) {
    JClassType iface = method.getReturnType().isClassOrInterface();
    return iface == null ? false :
      method.getEnclosingType().isAssignableTo(method.getReturnType().isClassOrInterface());
  }

  static String returnType(JMethod method, ModelMagic models) {
    // TODO make sure simple name is allowed here.
    return method.getReturnType().getSimpleSourceName();
  }

  static String fieldName(JMethod method, ModelMagic models) {
    FieldName field = method.getAnnotation(FieldName.class);
    if (field != null)
      return field.debugName().length() > 0
        // TODO && models.isDebugMode
        ? field.debugName() : field.value();
    GetterFor getter = method.getAnnotation(GetterFor.class);
    if (getter != null)
      return getter.value();
    SetterFor setter = method.getAnnotation(SetterFor.class);
    if (setter != null)
      return setter.value();
    DeleterFor deleter = method.getAnnotation(DeleterFor.class);
    if (deleter != null)
      return deleter.value();
    // No annotations, we have to guess.
    String name = method.getName();
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

  static boolean canBeSupertype(com.google.gwt.core.ext.typeinfo.JClassType subtype, com.google.gwt.core.ext.typeinfo.JClassType supertype) {
    return
      supertype.isInterface() == null &&
      !supertype.isFinal() &&
      supertype.isAssignableTo(subtype);

  }

  static String typeToParameterString(JType[] parameterTypes) {
    if (parameterTypes.length == 0) return "";
    StringBuilder b = new StringBuilder()
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

  static String typeToSignature(JType[] parameterTypes) {
    if (parameterTypes.length == 0) return "";
    StringBuilder b = new StringBuilder()
    .append(parameterTypes[0].getJNISignature());
    for (int i = 1, m = parameterTypes.length; i < m; i++) {
      b.append(parameterTypes[i].getJNISignature());
    }
    return b.toString();
  }

  public static boolean allAbstract(JMethod[] existing) {
    for (JMethod method : existing)
      if (!method.isAbstract())
        return false;
    return true;
  }

  public static String toSignature(JMethod method) {
    return method.getName()+"("+ModelGeneratorGwt.typeToSignature(method.getParameterTypes())+")";
  }

  public static RebindResult execImpl(TreeLogger logger, GeneratorContext ctx,
    String typeName) throws UnableToCompleteException {

    JClassType type = ctx.getTypeOracle().findType(typeName.replace('$', '.'));
    if (type == null) {
      logger.log(Type.ERROR, "Unable to find source for model interface type "+typeName);
      throw new UnableToCompleteException();
    }

    // Step one; see if this model already exists...
    ModelMagic magic = ModelMagic.active.get();
    String mangledName = magic.mangleName(type.getQualifiedSourceName(), MINIFY);
    String fqcn = MODEL_PACKAGE+"."+mangledName;
    if (ctx.isGeneratorResultCachingEnabled() && ctx.tryReuseTypeFromCache(typeName)) {
      return new RebindResult(RebindMode.USE_PARTIAL_CACHED, fqcn);
    }
    PrintWriter pw = ctx.tryCreate(logger, MODEL_PACKAGE, mangledName);
    if (pw == null) {
      return new RebindResult(RebindMode.USE_EXISTING, fqcn);
    }
    ModelArtifact model = magic.getOrMakeModel(logger, ctx, type);
    boolean isFinal = type.getSubtypes().length == 0;
    SourceBuilder<ModelMagic> builder = new SourceBuilder<ModelMagic>(
      "public "+(isFinal?"final ":"")+"class "+mangledName
      );
    builder.setPayload(magic);
    builder.setPackage(MODEL_PACKAGE);

    // Step two; transverse type model.
    if (type.isInterface() == null) {
      // Client has specified their own base type.
      // Let's see if we're expected to generate any methods.
      if (type.isAbstract()) {
        // Find the methods the client didn't bother to implement.
        Collection<JMethod> interfaceMethods = model.extractMethods(logger, builder, ctx, type);
        for (JMethod ifaceMethod : interfaceMethods) {
          JMethod classMethod;
          try {
            classMethod = type.getMethod(ifaceMethod.getName(), ifaceMethod.getTypeParameters());
            // If this method exists, we still need to pull off annotations.
            Annotation[] annos = model.extractAnnotations(logger, type, ifaceMethod, classMethod, ctx);
            model.applyAnnotations(logger, classMethod, annos, ctx);
          } catch (NotFoundException e) {
            // client didn't bother to implement this method.
            // let's do it for them.
            model.implementMethod(logger, ifaceMethod, ctx);
          }
        }
      } else {
        // Type is concrete.  Just return requested type.
        // TODO(james): throw in instance injection lookup and/or delegate type wrapping...
        return
          new RebindResult(RebindMode.USE_EXISTING, typeName);
      }
    } else {
      JClassType root = magic.getRootType(logger, ctx);
      Set<String> toSigs = getImplementedSignatures(root.getInheritableMethods());
      for (JMethod method :  model.extractMethods(logger, builder, ctx, type)) {
        if (toSigs.add(ModelGeneratorGwt.toSignature(method)))
          model.implementMethod(logger, method, ctx);
      }
    }

    // Step three, determine the fields we'll need to generate
    model.build(logger, builder, ctx, type);
    pw.println(builder.toString());
    ctx.commit(logger, pw);
//    ctx.commitArtifact(logger, model);

    // Step four, generate serialization protocols for this model type.

    logger.log(Type.ERROR, builder.toString());

    return new RebindResult(RebindMode.USE_ALL_NEW, MODEL_PACKAGE + "." + mangledName);
  }

  static Set<String> getImplementedSignatures(JMethod[] inheritableMethods) {
    Set<String> sigs = new LinkedHashSet<String>(inheritableMethods.length);
    for (JMethod method : inheritableMethods) {
      if (!method.isAbstract())
        sigs.add(ModelGeneratorGwt.toSignature(method));
    }
    return sigs;
  }

  public static IsType[] toTypes(JType[] parameterTypes) {
    int i = parameterTypes.length;
    IsType[] arr = new IsType[i];
    for (; i-->0;) {
      JType t = parameterTypes[i];
      arr[i] = X_Source.binaryToSource(t.getQualifiedBinaryName());
    }
    return arr;
  }

}
