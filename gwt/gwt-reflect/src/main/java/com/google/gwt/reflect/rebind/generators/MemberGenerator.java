package com.google.gwt.reflect.rebind.generators;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.reflect.client.ConstPool.ArrayConsts;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilType;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.thirdparty.xapi.dev.source.ClassBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.MethodBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.PrintBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.SourceBuilder;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsNamedType;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

@ReflectionStrategy
public class MemberGenerator {

  private static class ManifestMap {
    private final HashMap<String, JMethod> getters =
      new HashMap<String, JMethod>();
    private JMethod initMethod;

    public JMethod findGetterFor(
      final UnifyAstView ast, final String memberGetter)
        throws UnableToCompleteException {
      final JMethod getter = getters
        .get(memberGetter);
      if (getter == null) {
        initMethod(ast);
        final JDeclaredType type = ast.translate((JDeclaredType) initMethod
          .getOriginalReturnType());
        for (final JMethod method : type
          .getMethods()) {
          if (method.getName().endsWith(memberGetter)) {
            getters.put(memberGetter, method);
            return method;
          }
        }
        ast.error(type, "Type " + type.getName()
          + " does not have member getter method " + memberGetter);
        throw new UnableToCompleteException();
      }
      return getter;
    }

    public JMethod initMethod(final UnifyAstView ast)
      throws UnableToCompleteException {
      if (initMethod == null) {
        final JDeclaredType type = ast
          .searchForTypeBySource("com.google.gwt.reflect.shared.JsMemberPool");
        for (final JMethod method : type
          .getMethods()) {
          if (method.getName().equals("getMembers")) {
            initMethod = method;
            break;
          }
        }
      }
      return initMethod;
    }
  }

  public static void cleanup() {
    manifests.remove();
    shouldFail = null;
  }

  public static String getConstructorFactoryName(final JClassType type,
    final JParameter[] list) {
    final StringBuilder b = new StringBuilder(type.getName());
    b.append(CONSTRUCTOR_SPACER).append(
      ReflectionUtilType.toUniqueFactory(list, type.getConstructors()));
    return b.toString();
  }

  public static String getFieldFactoryName(final JClassType type,
    final String name) {
    final StringBuilder b = new StringBuilder(type.getName());
    b.append(FIELD_SPACER).append(name);
    return b.toString();
  }

  public static String getMethodFactoryName(final JClassType type,
    final String name,
    final JParameter[] list) {
    final StringBuilder b = new StringBuilder(type.getName());
    b.append(METHOD_SPACER).append(name);
    // Check for polymorphism
    final com.google.gwt.core.ext.typeinfo.JMethod[] overloads = type
      .getOverloads(name);
    if (overloads.length > 1) {
      // Have to use the parameters to make a unique name.
      // Might be worth it to move this method to instance level, and use a count
      final String uniqueName = ReflectionUtilType.toUniqueFactory(list,
        overloads);
      b.append('_').append(uniqueName);
    }
    return b.toString();
  }

  public static final ReflectionStrategy DEFAULT_STRATEGY = MemberGenerator.class
    .getAnnotation(ReflectionStrategy.class);
  public static final String
  METHOD_SPACER = "_mthd_",
  FIELD_SPACER = "_fld_",
  CONSTRUCTOR_SPACER = "_ctr_";

  private static final String
  GWT_REFLECT = GwtReflect.class.getName(),
  JSO = JavaScriptObject.class.getSimpleName(),
  NULL_CHECK = "@" + GWT_REFLECT + "::nullCheck(*)(o);";

  private static final Type logLevel = Type.TRACE;

  private static Boolean shouldFail;

  private static final ThreadLocal<ManifestMap> manifests = new ThreadLocal<ManifestMap>() {
    @Override
    protected ManifestMap initialValue() {
      return new ManifestMap();
    };
  };

  public String generateConstructorFactory(final TreeLogger logger,
    final ReflectionGeneratorContext ctx,
    final JConstructor ctor, String factory, final ReflectionManifest manifest)
      throws UnableToCompleteException {
    final JClassType type = ctor.getEnclosingType();
    final String pkg = type.getPackage().getName();
    factory = factory.replace('.', '_');
    final SourceBuilder<?> out = ctx.tryCreate(PUBLIC | FINAL, pkg, factory);

    if (out == null) {
      // TODO some kind of test to see if structure has changed...
      return pkg + "." + factory;
    }

    final String simpleName = out.getImports().addImport(
      type.getQualifiedSourceName());

    final ClassBuffer cb = out.getClassBuffer();

    cb.createConstructor(Modifier.PRIVATE);
    cb.createField("Constructor <" + simpleName + ">", "ctor", PRIVATE | STATIC);

    final MethodBuffer instantiator = cb
      .addImports(Constructor.class, GwtReflect.class)
      .createMethod(
        "public static Constructor <" + simpleName + "> instantiate()")
        .println("if (ctor == null) {")
        .indent()
        .println("ctor = new Constructor<" + simpleName + ">(")
        .print(ctor.getEnclosingType().getQualifiedSourceName() + ".class, ")
        .print(ReflectionUtilType.getModifiers(ctor) + ", ")
        .println("invoker(), ");
    ConstPoolGenerator.getGenerator();
    final GwtRetention retention = manifest.getRetention(ctor);

    out.getImports().addStatic(
      ArrayConsts.class.getCanonicalName() + ".EMPTY_CLASSES");
    final JParameter[] params = ctor.getParameters();
    instantiator
    .addImports(JavaScriptObject.class)
    .addImports(ArrayConsts.class);

    if (retention.annotationRetention() > 0) {
      final Annotation[] annos = ReflectionUtilType.extractAnnotations(
        retention.annotationRetention(), ctor);
      if (annos.length == 0) {
        out.getImports().addStatic(
          ArrayConsts.class.getCanonicalName() + ".EMPTY_ANNOTATIONS");
        instantiator.print("EMPTY_ANNOTATIONS, ");
      } else {
        ctx.getConstPool().arrayOfAnnotations(logger,
          ctx.getGeneratorContext(), instantiator, ctx.getAst(), annos);
        instantiator.print(", ");
      }
    } else {
      out.getImports().addStatic(
        ArrayConsts.class.getCanonicalName() + ".EMPTY_ANNOTATIONS");
      instantiator.print("EMPTY_ANNOTATIONS, ");
    }
    appendClassArray(instantiator, params, ctx);

    instantiator
    .print(", EMPTY_CLASSES ")
    .println(");")
    .outdent()
    .println("}")
    .returnValue("ctor");

    createInvokerMethod(cb, type, type, "new", ctor.getParameters(), true, ctor.isPublic());

    if (isDebug(type, ReflectionStrategy.CONSTRUCTOR)) {
      logger.log(Type.INFO, out.toString());
    }

    ctx.commit(logger);

    return out.getQualifiedName();
  }

  public String generateFieldFactory(final TreeLogger logger,
    final UnifyAstView ast,
    final JField field, String factoryName, final ReflectionManifest manifest)
      throws UnableToCompleteException {
    final String pkg = field.getEnclosingType().getPackage().getName();
    final JClassType enclosingType = field.getEnclosingType();
    final JType fieldType = field.getType().getErasedType();
    final String jni = field.getType().getJNISignature();
    factoryName = factoryName.replace('.', '_');

    final SourceBuilder<JField> out = new SourceBuilder<JField>
    ("public final class " + factoryName).setPackage(pkg);

    out.getClassBuffer().createConstructor(PRIVATE);

    final GeneratorContext ctx = ast.getGeneratorContext();
    final PrintWriter pw = ctx.tryCreate(logger, pkg, factoryName);
    if (pw == null) {
      if (isDebug(enclosingType, ReflectionStrategy.FIELD)) {
        logger.log(Type.INFO, "Skipped writing field for " + factoryName
          + ", as factory already exists");
      }
      return out.getQualifiedName();
    }

    final ClassBuffer cb = out.getClassBuffer();

    final GwtRetention retention = manifest.getRetention(field);
    final boolean hasAnnos = retention.annotationRetention() > 0;
    final Annotation[] annos;
    if (hasAnnos) {
      annos = ReflectionUtilType.extractAnnotations(
        retention.annotationRetention(), field);
      generateGetAnnos(logger, out, annos, ast);
    } else {
      annos = new Annotation[0];
      generateGetAnnos(logger, out, new Annotation[0], ast);
    }

    final String ref = (field.isStatic() ? "" : "o.") + "@"
      + enclosingType.getQualifiedSourceName() + "::" + field.getName();
    final MethodBuffer accessor = cb
      .createMethod("private static JavaScriptObject getAccessor()")
      .setUseJsni(true)
      .println("return {").indent();
    if (hasAnnos) {
      accessor
      .println("annos: function() {")
      .indent()
      .print("return ");
      if (annos.length == 0) {
        accessor.println("{};");
      } else {
        accessor
        .print("@")
        .print(out.getQualifiedName())
        .println("::allAnnos()();");

      }
      accessor.outdent().println("},");
    }
    accessor
    .println("getter: function(o) {");
    if (!field.isStatic()) {
      accessor.indentln(NULL_CHECK);
    }
    final boolean isPrimitive = field.getType().isPrimitive() != null;

    accessor.indent().print("return ");
    accessor.append(ref);

    accessor
    .println(";")
    .outdent()
    .print("}");
    if (field.isFinal()) {
      accessor.println().outdent().println("};");
    } else {
      accessor.println(", setter: function(o, v) {");
      if (!field.isStatic()) {
        accessor.indentln(NULL_CHECK);
      }
      accessor.indentln(ref + " = ");

      final StringBuilder unboxer = new StringBuilder();
      unboxer.append("v");

      accessor
      .indentln(unboxer + ";")
      .println("}")
      .outdent().println("};");
    }

    final MethodBuffer instantiate = cb
      .createMethod("public static Field instantiate()")
      .print("return new ")
      .addImports(Field.class, JavaScriptObject.class);

    if (isPrimitive) {
      switch (jni.charAt(0)) {
      case 'Z':
        instantiate.addImport("java.lang.reflect.Boolean_Field");
        instantiate.print("Boolean_Field(");
        break;
      case 'B':
        instantiate.addImport("java.lang.reflect.Byte_Field");
        instantiate.print("Byte_Field(");
        break;
      case 'S':
        instantiate.addImport("java.lang.reflect.Short_Field");
        instantiate.print("Short_Field(");
        break;
      case 'C':
        instantiate.addImport("java.lang.reflect.Char_Field");
        instantiate.print("Char_Field(");
        break;
      case 'I':
        instantiate.addImport("java.lang.reflect.Int_Field");
        instantiate.print("Int_Field(");
        break;
      case 'J':
        accessor.addAnnotation(UnsafeNativeLong.class);
        instantiate.addImport("java.lang.reflect.Long_Field");
        instantiate.print("Long_Field(");
        break;
      case 'F':
        instantiate.addImport("java.lang.reflect.Float_Field");
        instantiate.print("Float_Field(");
        break;
      case 'D':
        instantiate.addImport("java.lang.reflect.Double_Field");
        instantiate.print("Double_Field(");
        break;
      default:
        logger.log(Type.ERROR, "Bad primitive type in field generator "
          + fieldType.getQualifiedSourceName());
        throw new UnableToCompleteException();
      }
    } else {
      final String imported = instantiate.addImport(fieldType
        .getQualifiedSourceName());
      instantiate.print("Field(" + imported + ".class, ");
    }

    final String enclosing = instantiate.addImport(field.getEnclosingType()
      .getQualifiedSourceName());
    instantiate
    .print(enclosing + ".class, ")
    .print("\"" + field.getName() + "\", ")
    .print(ReflectionUtilType.getModifiers(field) + ", getAccessor());");

    final String src = out.toString();
    pw.println(src);
    if (isDebug(enclosingType, ReflectionStrategy.FIELD)) {
      logger.log(Type.INFO, "Field provider for " + field.toString() + "\n"
        + src);
    }

    ctx.commit(logger, pw);
    return out.getQualifiedName();
  }

  public String generateMethodFactory(final TreeLogger logger,
    final UnifyAstView ast,
    final com.google.gwt.core.ext.typeinfo.JMethod method,
    String factoryName, final ReflectionManifest manifest)
      throws UnableToCompleteException {
    final JClassType type = method.getEnclosingType();
    final String pkg = type.getPackage().getName();
    factoryName = factoryName.replace('.', '_');

    final GeneratorContext ctx = ast.getGeneratorContext();
    final PrintWriter pw = ctx.tryCreate(logger, pkg, factoryName);
    if (pw == null) {
      return (pkg.length() == 0 ? "" : pkg + ".") + factoryName;
    }

    final SourceBuilder<JMethod> out = new SourceBuilder<JMethod>
    ("public final class " + factoryName).setPackage(pkg);
    final ClassBuffer cb = out.getClassBuffer().addImports(Method.class);

    createInvokerMethod(cb, type, method.getReturnType(), method.getName(),
      method.getParameters(), method.isStatic(), method.isPublic());

    cb
    .createMethod("public static Method instantiate()")
    .returnValue("new Method("
        + cb.addImport(type.getErasedType().getQualifiedSourceName())+".class, "
        + "\""+method.getName()+"\", "
        + "getParameterTypes(), "
        + cb.addImport(method.getReturnType().getErasedType().getQualifiedSourceName()) + ".class, "
        + "getExceptionTypes(), "
        + ReflectionUtilType.getModifiers(method)+", "
        + "invoker(),"
        + "allAnnos()"
        + ")");

    /**
     Class returnType, Class[] checkedExceptions,
    int modifiers, JavaScriptObject method, JavaScriptObject annos
     */

    final GwtRetention retention = manifest.getRetention(method);

    if (retention.annotationRetention() > 0) {
      final Annotation[] annos = ReflectionUtilType.extractAnnotations(
        retention.annotationRetention(), method);
      generateGetAnnos(logger, out, annos, ast);
    } else {
      generateGetAnnos(logger, out, new Annotation[0], ast);
    }

    generateGetParams(logger, cb, method.getParameters());
    generateGetExceptions(logger, cb, method.getThrows());
    generateGetReturnType(logger, cb, method);
    generateGetName(logger, cb, method);
    generateGetModifier(logger, cb, ReflectionUtilType.getModifiers(method));
    generateGetDeclaringClass(logger, cb, method.getEnclosingType(), "?");

    final String src = out.toString();
    if (isDebug(type, ReflectionStrategy.METHOD)) {
      logger.log(Type.INFO,
        "Method provider for " + method.getReadableDeclaration() + "\n" + src);
    }

    pw.println(src);

    ctx.commit(logger, pw);

    return out.getQualifiedName();
  }

  protected void appendClassArray(final MethodBuffer out,
    final JParameter[] params,
    final ReflectionGeneratorContext ctx) {
    int i = params.length;
    final String[] names = new String[i];
    for (; i-- > 0;) {
      names[i] = params[i].getType().getErasedType().getQualifiedSourceName();
    }
    final ConstPoolGenerator constPool = ctx.getConstPool();
    constPool.arrayOfClasses(ctx.getLogger(), out, names);
  }

  protected JMethodCall checkConstPool(final UnifyAstView ast,
    final JMethodCall callSite, final JExpression classRef,
    final JExpression... args) throws UnableToCompleteException {
    final JMethod initPool = getMemberPoolInit(ast);

    final JMethodCall getMemberPool = new JMethodCall(initPool.getSourceInfo(),
      null, initPool);
    getMemberPool.addArg(classRef);

    final ManifestMap map = manifests.get();
    final JMethod getter = map.findGetterFor(ast,
      memberGetter());

    final JMethodCall checkPool = new JMethodCall(initPool.getSourceInfo(),
      getMemberPool, getter);
    for (final JExpression arg : args) {
      checkPool.addArg(arg);
    }

    return checkPool;
  }

  protected void createInvokerMethod(final ClassBuffer cb,
    final JClassType type, final JType returnType,
    final String methodName, final JParameter[] params, final boolean isStatic, final boolean isPublic) {
    boolean hasLong = returnType.getJNISignature().equals("J");

    final StringBuilder functionSig = new StringBuilder();
    StringBuilder jsniSig = new StringBuilder();
    StringBuilder arguments = new StringBuilder();
    // Fill in parameter data
    final boolean isNotCtor = !"new".equals(methodName);
    assert isStatic || isNotCtor : "Constructors must be static!";

    for (int i = 0, m = params.length; i < m; i++) {
      JType param = params[i].getType();
//      if (param.isParameterized() != null) {
//        param = param.isParameterized().getRawType();
//      }
      final boolean isArray = param.isArray() != null;
      while (param.isArray() != null) {
        jsniSig.append("[");
        param = param.isArray().getComponentType();
      }
      jsniSig.append(param.getJNISignature());
      final char varName = Character
        .toUpperCase(Character.forDigit(10 + i, 36));
      if (isNotCtor || i > 0) {
        functionSig.append(", ");
      }
      functionSig.append(varName);
      if (i > 0) {
        arguments.append(", ");
      }
      if (!isArray) {
        maybeStartUnboxing(arguments, param);
      }
      arguments.append(varName);
      if (!isArray) {
        maybeFinishUnboxing(arguments, param);
      }
      hasLong |= "J".equals(param.getJNISignature());
    }

    final MethodBuffer invoker = cb.addImports(JavaScriptObject.class)
      .createMethod("public static " + JSO + " " + "invoker()")
      .setUseJsni(true)
      .print("return function(");
    if (isNotCtor) {
      invoker.print("o");
    }
    if (hasLong) {
      invoker.addAnnotation(UnsafeNativeLong.class);
    }
    invoker.println(functionSig + ") {");
    if (!isStatic) {
      invoker.indentln(NULL_CHECK);
    }
    // Build the structure of the method invoker javascript function


    String typeName = type.getQualifiedSourceName();
    String invokeName = methodName;
    final boolean returns = isReturnable(returnType);
    final boolean staticDispatch = !isStatic && isPublic && !"new".equals(methodName);

    if (returns) {
      invoker.print("return ");
    }
    maybeStartBoxing(invoker, returnType);

    if (!isStatic) {
      // Due to a bug with accessing instance methods on String, we add an
      // extra layer of "staticifying" to public instance methods.
      if (staticDispatch) {
        jsniSig = new StringBuilder(type.getJNISignature()).append(jsniSig);
        arguments = new StringBuilder("o").append(arguments.length() == 0 ? "" : ",").append(arguments);
        invokeName = methodName+"$$$";
        final MethodBuffer staticMethod = cb.createMethod("private static "+returnType.getErasedType().getQualifiedSourceName()+" "+invokeName+"()");
        staticMethod.addParameter(typeName, "_");
        if (returns) {
          staticMethod.print("return ");
        }
        staticMethod.print("_.").print(methodName).print("(");
        staticMethod.addExceptions(Throwable.class);// Let exceptions bubble
        for (int i = 0, m = params.length; i < m; i++) {
          final JParameter param = params[i];
          final String paramName = toParamName(i);
          staticMethod.addParameter(param.getType().getErasedType().getQualifiedSourceName(), paramName);
          if (i > 0) {
            staticMethod.print(",");
          }
          staticMethod.print(paramName);
        }
        staticMethod.println(");");
        typeName = cb.getQualifiedName();
      } else {
        invoker.print("o.");
      }
    }
    invoker
    .indent()
    .print("@").print(typeName)
    .print("::")
    .print(invokeName)
    .print("(")
    .print(jsniSig.toString())
    .print(")")
    .print("(")
    .print(arguments.toString())
    .print(")");
    maybeFinishBoxing(invoker, returnType);

    invoker
    .println(";")
    .outdent()
    .println("};");

  }

  private String toParamName(int i) {
    final StringBuilder b = new StringBuilder();
    do {
      b.append((char)('a'+(i%26)));
      i = i / 26;
    } while (i > 0);

    return b.toString();
  }

  protected JMethod getMemberPoolInit(
    final UnifyAstView ast) throws UnableToCompleteException {
    final ManifestMap map = manifests.get();
    return map.initMethod(ast);
  }

  protected boolean isDebug(final JClassType type, final int memberType) {
    ReflectionStrategy strategy = type.getAnnotation(ReflectionStrategy.class);
    if (strategy == null) {
      strategy = type.getPackage().getAnnotation(ReflectionStrategy.class);
    }
    if (strategy == null) {
      return false;
    }
    return (strategy.debug() & memberType) > 0;
  }

  protected boolean isReturnable(final JType returnType) {
    return !"V".equals(returnType.getJNISignature());
  }

  protected String memberGetter() {
    throw new UnsupportedOperationException("memberGetting not implemented by "
      + getClass().getName());
  }

  protected boolean shouldFailIfMissing(final TreeLogger logger, final UnifyAstView ast) {
    if (shouldFail != null) {
      return shouldFail;
    }
    final PropertyOracle properties = ast.getRebindPermutationOracle().getConfigurationPropertyOracle();
    try {
      final ConfigurationProperty config = properties
          .getConfigurationProperty("gwt.reflect.never.fail");
      if (config == null) {
        // We may want to change the default fail level to true
        shouldFail = false;
      } else {
        shouldFail = !"true".equals(config.getValues().get(0));
      }
    } catch (final BadPropertyValueException e) {
      e.printStackTrace();
      shouldFail = false;
    }
    return shouldFail;
  }

  protected String toClass(final JClassType param) {
    return param.getErasedType().getQualifiedSourceName() + ".class";
  }

  protected String toClass(final JParameter param) {
    return param.getType().getErasedType().getQualifiedSourceName() + ".class";
  }

  protected Type logLevel() {
    return logLevel;
  }

  protected Type warnLevel(final TreeLogger logger, final UnifyAstView ast) {
    if (shouldFailIfMissing(logger, ast)) {
     return Type.ERROR;
    }
    return Type.WARN;
  }

  private void generateGetAnnos(final TreeLogger logger,
    final SourceBuilder<?> sb, final Annotation[] annos,
    final UnifyAstView ast) throws UnableToCompleteException {

    final ClassBuffer out = sb.getClassBuffer();

    // Start the method to retrive all annotation in an array.  Note we always return a new array,
    // to prevent modifications from corrupting future invocation.
    final String jso = out.addImport(JavaScriptObject.class);
    final MethodBuffer getAnnos = out.createMethod(
        "private static native "+jso+" allAnnos()")
        .setUseJsni(true)
        .println("var annos = {};");

    // Short circuit when there are no annotations to provide
    if (annos.length == 0) {
      getAnnos.returnValue("annos");
      return;
    }

    getAnnos.println("var name;");
    final String cls = Class.class.getName();
    for (int i = 0, m = annos.length; i < m; i++) {
      // First, generate the annotation provider method, which returns the runtime instance of this annotation
      final Annotation anno = annos[i];
      final IsNamedType gen = GwtAnnotationGenerator.generateAnnotationProvider(logger, sb, anno, ast);

      // Then, print an entry in the method which returns all annotations in an array.
      getAnnos.println("name = @"+anno.annotationType().getCanonicalName()+"::class.@"+cls+"::getName()();");
      getAnnos.println("annos[name] = @"+gen.getQualifiedName()+"::"+gen.getName()+"()();");
      getAnnos.println("annos[name].declared = true;");

    }// end loop

    getAnnos.returnValue("annos");
  }

  private void generateGetDeclaringClass(final TreeLogger logger,
    final ClassBuffer cb, final JClassType type, final String generic) {
    final MethodBuffer getDeclaringClass = cb.createMethod("public Class<"
      + generic + "> getDeclaringClass()");
    if (type.isPrivate()) {
      getDeclaringClass
      .setUseJsni(true)
      .returnValue("@" + type.getQualifiedSourceName() + "::class");
    } else {
      getDeclaringClass
      .returnValue(type.getQualifiedSourceName() + ".class");
    }
  }

  private void generateGetExceptions(final TreeLogger logger,
    final ClassBuffer cb, final JClassType[] exceptions) {
    final MethodBuffer getExceptions = cb
      .createMethod("public static Class<?>[] getExceptionTypes()")
      .println("return new Class<?>[]{");
    if (exceptions.length > 0) {
      getExceptions.println(toClass(exceptions[0]));
      for (int i = 1, m = exceptions.length; i < m; i++) {
        getExceptions.println(", " + toClass(exceptions[i]));
      }
    }
    getExceptions.println("};");
  }

  private void generateGetModifier(final TreeLogger logger,
    final ClassBuffer cb, final int mod) {
    cb.createMethod("public int getModifiers()").returnValue(
      Integer.toString(mod));
  }

  private void generateGetName(final TreeLogger logger, final ClassBuffer cb,
    final com.google.gwt.core.ext.typeinfo.JMethod method) {
    cb.createMethod("public String getName()").returnValue(
      "\"" + method.getName() + "\"");
  }

  private void generateGetParams(final TreeLogger logger, final ClassBuffer cb,
    final JParameter[] params) {
    final MethodBuffer getParameters = cb
      .createMethod("public static Class<?>[] getParameterTypes()")
      .println("return new Class<?>[]{");
    if (params.length > 0) {
      getParameters.println(toClass(params[0]));
      for (int i = 1, m = params.length; i < m; i++) {
        getParameters.println(", " + toClass(params[i]));
      }
    }
    getParameters.println("};");
  }

  private void generateGetReturnType(final TreeLogger logger,
    final ClassBuffer cb, final com.google.gwt.core.ext.typeinfo.JMethod method) {
    cb.createMethod("public Class<?> getReturnType()").returnValue(
      method.getReturnType().getErasedType().getQualifiedSourceName()
      + ".class");
  }

  private void maybeFinishBoxing(final PrintBuffer invoke,
    final JType returnType) {
    final JPrimitiveType prim = returnType.isPrimitive();
    if (prim != null) {
      switch (prim) {
      case BOOLEAN:
        invoke.print(" ? @java.lang.Boolean::TRUE : @java.lang.Boolean::FALSE");
      case VOID:
        return;
      default:
        invoke.print(")");
      }
    }
  }

  private void maybeFinishUnboxing(final StringBuilder b, final JType returnType) {
    final JPrimitiveType type = returnType.isPrimitive();
    if (type != null) {
      switch (type) {
      case BOOLEAN:
        b.append(".@java.lang.Boolean::booleanValue()()");
        break;
      case CHAR:
        b.append(".@java.lang.Character::charValue()()");
        break;
      case LONG:
        b.append(")");
        break;
      case BYTE:
      case DOUBLE:
      case INT:
      case FLOAT:
      case SHORT:
        b.append(".@java.lang.Number::doubleValue()()");
        break;
      default:
      }
    }
  }

  private void maybeStartBoxing(final PrintBuffer invoke, final JType returnType) {
    final JPrimitiveType prim = returnType.isPrimitive();
    if (prim != null) {
      switch (prim) {
      case LONG:
        invoke.print("@" + GWT_REFLECT + "::boxLong(J)(");
        break;
      case BYTE:
        invoke.print("@java.lang.Byte::new(B)(");
        break;
      case CHAR:
        invoke.print("@java.lang.Character::new(C)(");
        break;
      case DOUBLE:
        invoke.print("@java.lang.Double::new(D)(");
        break;
      case FLOAT:
        invoke.print("@java.lang.Float::new(F)(");
        break;
      case INT:
        invoke.print("@java.lang.Integer::new(I)(");
        break;
      case SHORT:
        invoke.print("@java.lang.Short::new(S)(");
        break;
      default:
      }
    }
  }

  private void maybeStartUnboxing(final StringBuilder b, final JType returnType) {
    if (JPrimitiveType.LONG == returnType.isPrimitive()) {
      b.append("@" + GWT_REFLECT + "::unboxLong(Ljava/lang/Number;)(");
    }
  }

  protected String toString(final JExpression inst) {
    return inst == null ? "null" : inst.getClass().getName()+": "+inst;

  }

  /**
   * If the configuration property gwt.reflect.never.fail is true or missing, then
   * this method will return an expression that checks the JsMemberPool at runtime
   * for an enhanced member as defined by the {@link #memberGetter()} method name.
   * <p>
   * If gwt.reflect.never.fail is set to the default of false, this will throw
   * an {@link UnableToCompleteException}, thus, you are encouraged to log a warning
   * at the logLevel of {@link #warnLevel(TreeLogger, UnifyAstView)} before invoking this method.
   */
  public JExpression maybeCheckConstPool(final TreeLogger logger, final UnifyAstView ast,
    final JMethodCall callSite, final JExpression inst, final JExpression ... params) throws UnableToCompleteException {
    if (shouldFailIfMissing(logger, ast)) {
      throw new UnableToCompleteException();
    }
    return checkConstPool(ast, callSite, inst, params);
  }

}
