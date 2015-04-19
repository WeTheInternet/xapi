package com.google.gwt.reflect.rebind.generators;

import static com.google.gwt.reflect.rebind.ReflectionUtilType.extractAnnotations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.reflect.shared.JsMemberPool;
import com.google.gwt.thirdparty.xapi.dev.source.ClassBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.MemberBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.MethodBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.PrintBuffer;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsQualified;

import java.lang.annotation.Annotation;
import java.util.HashMap;

/**
 * The base class of all constructor, method and field generators,
 * and also of almost all injectors used for reflection.
 * <p>
 * This rather large class is used to encapsulate all of the low-level generator details
 * used by all other generator / injector implementations.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@ReflectionStrategy
public abstract class MemberGenerator {

  private static class MemberPoolMethods {

    /**
     * A mapping to one of the various getter methods on the {@link JsMemberPool} class.
     * <p>
     * These getters are used for runtime lookups of members, when reflection is performed
     * on reference objects, rather than literals.
     */
    private final HashMap<String, JMethod> getters = new HashMap<String, JMethod>();

    /**
     * A mapping of constructors that we may want to cache and reuse.
     */
    private final HashMap<String, JConstructor> constructors = new HashMap<String, JConstructor>();

    private JMethod doThrow;

    /**
     * The {@link JMethod} for {@link JsMemberPool#getMembers(Class)}, used so we can create invocations
     * that will translate a {@link Class} into a {@link JsMemberPool}.
     */
    private JMethod getMembers;

    /**
     * Whether or not we should fail-fast when trying to perform runtime reflection.
     * <p>
     * If gwt.reflect.never.fail is set to false, then this boolean will be true,
     * and we should fail the compile instead of generating references to runtime lookup methods.
     */
    public Boolean shouldFail;

    /**
     * Finds the requested getter method from the {@link JsMemberPool} class.
     *
     * @param ast -> The {@link UnifyAstView} for looking up {@link JMethod}s
     * @param memberGetter -> The name of the getter to find
     * @return -> The {@link JMethod} needed to create a runtime invocation of the requested method.
     *
     * @throws UnableToCompleteException -> If any fatal error occurs
     */
    public JMethod findGetterFor(final UnifyAstView ast, final String memberGetter)
        throws UnableToCompleteException {

      // First, check the cache
      final JMethod getter = getters.get(memberGetter);
      if (getter != null) {
        return getter;
      }

      // Ensure we have a valid reference to the getMembers method.
      findGetMembersMethod(ast);

      // Use the method to grab the JClassType of JsMemberPool
      final JDeclaredType type = ast.translate((JDeclaredType) getMembers.getOriginalReturnType());

      // Search the type for the method that matches the requested name
      for (final JMethod method : type.getMethods()) {
        if (method.getName().equals(memberGetter)) {
          // Save the method so we don't waste time looking it up again
          getters.put(memberGetter, method);
          return method;
        }
      }

      // If the method was not found, abort
      ast.error(type, "Type " + type.getName()
        + " does not have member getter method " + memberGetter);
      throw new UnableToCompleteException();

    }

    /**
     * Search for the method {@link JsMemberPool#getMembers(Class)}.  This method is used to load
     * the {@link JsMemberPool} for a given class, to perform runtime reflection lookups.
     *
     * @param ast -> The {@link UnifyAstView} used to find the {@link JMethod} we want.
     * @return -> The JMethod used to invoke {@link JsMemberPool#getMembers(Class)}
     */
    public JMethod findGetMembersMethod(final UnifyAstView ast) {
      if (getMembers == null) {
        // First, get the type
        final JDeclaredType type = ast.searchForTypeBySource(JsMemberPool.class.getCanonicalName());
        // Then, iterate through the methers to find .getMembers().  We do not overload this method,
        // so we don't bother checking the signature.
        for (final JMethod method : type.getMethods()) {
          if (method.getName().equals("getMembers")) {
            getMembers = method;
            break;
          }
        }
      }
      return getMembers;
    }

    /**
     * This method checks if we should fail fast when a reflection method is referenced without all-literal
     * parameters, or if we should allow runtime lookups of reflection objects.  Defaults to false.
     *
     * @param logger -> The {@link TreeLogger} for logging
     * @param ast -> The {@link UnifyAstView} where we will retrieve our property oracle from.
     * @return  -> true if the compile should break if attempting to do reflection without using all
     * compile-time literals.
     */
    public boolean shouldFail(final TreeLogger logger, final UnifyAstView ast) {
      // Use the existing cached values
      if (shouldFail != null) {
        return shouldFail;
      }
      // Grab our property from the oracle
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
        logger.log(Type.WARN, "Could not find configuration property gwt.reflect.never.fail; "
            + "did you remember to inherit com.google.gwt.reflect.Reflect?", e);
        shouldFail = false;
      }
      return shouldFail;
    }

    public JConstructor findConstructor(final TreeLogger logger, final String sourceTypeName, final String signature, final UnifyAstView ast) throws UnableToCompleteException {
      JConstructor constructor = constructors.get(signature);
      if (constructor != null) {
        return constructor;
      }
      final JDeclaredType clazz = ast.searchForTypeBySource(sourceTypeName);
      if (clazz == null) {
        logger.log(Type.ERROR, "Unable to find type "+sourceTypeName+" using source name lookup for constructor "+signature);
        throw new UnableToCompleteException();
      }
      constructor = (JConstructor) clazz.findMethod(signature, false);
      if (constructor == null) {
        logger.log(Type.ERROR, "Unable to find constructor " + signature + " in "+sourceTypeName+"\n"+clazz.getMethods());
        for (final JMethod method : clazz.getMethods()) {
          logger.log(Type.ERROR, "Meow "+method.getSignature());
        }
        throw new UnableToCompleteException();
      }
      constructors.put(signature, constructor);
      return constructor;
    }

    public JMethod getThrowMethod(final TreeLogger logger, final UnifyAstView ast) {
      if (doThrow == null) {
        final JDeclaredType gwtReflect = ast.searchForTypeBySource(GwtReflect.class.getName());
        for (final JMethod method : gwtReflect.getMethods()) {
          if (method.getName().equals("doThrow")) {
            doThrow = method;
            break;
          }
        }
      }
      return doThrow;
    }
  }

  /**
   * A default instance of {@link ReflectionStrategy}, to be used if a type does not specify an instance
   * of its own to use.
   */
  public static final ReflectionStrategy DEFAULT_STRATEGY =
      MemberGenerator.class.getAnnotation(ReflectionStrategy.class);

  /**
   * A unique string to be placed between the canonical name of the type that sourced a method we want to
   * generate a provider for, and the name of the method itself, followed by the parameter types.
   * <p>
   * Example:
   * com.foo.Type::myMethod(Class) will have a provider class:
   * com.foo.Type_mthd_myMethod_java_lang_Class
   */
  public static final String METHOD_SPACER = "_mthd_";

  /**
   * A unique string to be placed between the canonical name of the type that sourced a field we want to
   * generate a provider for, and the name of the field itself.
   * <p>
   * Example:
   * com.foo.Type::myField will have a provider class:
   * com.foo.Type_fld_myField
   */
  public static final String FIELD_SPACER = "_fld_";

  /**
   * A unique string to be placed between the canonical name of the type that sourced a constructor we want to
   * generate a provider for, and the types of the parameters of that constructor.
   * <p>
   * Example:
   * com.foo.Type::new(Class) will have a provider class:
   * com.foo.Type_ctr_java_lang_Class
   */
  public static final String CONSTRUCTOR_SPACER = "_ctr_";

  /**
   * A handy string for the fully qualified name of the {@link GwtReflect} class
   */
  private static final String GWT_REFLECT = GwtReflect.class.getName();

  /**
   * A handy string for the fully qualified name of the {@link JavaScriptObject} class
   */
  private static final String JSO = JavaScriptObject.class.getSimpleName();

  /**
   * A handy string for the fully qualified name of the {@link GwtReflect#nullCheck(Object)} method
   */
  protected static final String NULL_CHECK = "@" + GWT_REFLECT + "::nullCheck(*)(o);";

  /**
   * The default logLevel, {@link Type#DEBUG}, for all subclasses.
   */
  private static final Type logLevel = Type.DEBUG;

  /**
   * A ThreadLocal for safely storing an instance of {@link MemberPoolMethods} during a compile.
   */
  private static final ThreadLocal<MemberPoolMethods> memberPoolMethods = new ThreadLocal<MemberPoolMethods>() {
    @Override
    protected MemberPoolMethods initialValue() {
      return new MemberPoolMethods();
    };
  };

  /**
   * Cleans up our thread local which stores {@link JMethod} that will no longer be valid on the next recompile.
   */
  public static void cleanup() {
    memberPoolMethods.remove();
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
  protected JExpression maybeCheckConstPool(final TreeLogger logger, final UnifyAstView ast,
    final JMethodCall callSite, final JExpression inst, final JExpression ... params) throws UnableToCompleteException {
    if (shouldFailIfMissing(logger, ast)) {
      throw new UnableToCompleteException();
    }
    return checkConstPool(ast, callSite, inst, params);
  }

  protected void appendAnnotationSupplier(final TreeLogger logger, final MemberBuffer<?> out,
      final HasAnnotations member, final GwtRetention retention, final ReflectionGeneratorContext ctx)
          throws UnableToCompleteException {

    final Annotation[] annos = extractAnnotations(retention.annotationRetention(), member);

      // Print a call to new AnnotationSupplierX, which returns the constant annotation array for the supplied annos
    final IsQualified supplier = ctx.getConstPool().annotationArraySupplier(logger,
          ctx.getGeneratorContext(), out, annos);
    final String supplierClass = out.addImport(supplier.getQualifiedName());
    out.println("new "+supplierClass+"()");
  }

  protected void appendClassArray(final MethodBuffer out,
    final JParameter[] params,
    final ReflectionGeneratorContext ctx) {
    final JType[] types = new JType[params.length];
    for (int i = params.length; i --> 0;) {
      types[i] = params[i].getType();
    }
    appendClassArray(out, types, ctx);
  }

  protected <T extends JType> void appendClassArray(final MethodBuffer out,
      final T[] types,
      final ReflectionGeneratorContext ctx) {
    int i = types.length;
    final String[] names = new String[i];
    for (; i-- > 0;) {
      names[i] = types[i].getErasedType().getQualifiedSourceName();
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

    final MemberPoolMethods map = memberPoolMethods.get();
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
    final String methodName, final JParameter[] params, final boolean isStatic, final boolean isNotPrivate) {
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
    final boolean useStaticDispatch = !isStatic && isNotPrivate;
    final boolean isConstructor = "new".equals(methodName);

    if (returns) {
      invoker.print("return ");
    }
    maybeStartBoxing(invoker, returnType);

    if (!isStatic) {
      // Due to a bug with accessing instance methods on Strings and JSOs, we add an
      // extra layer of "staticifying" to public instance methods.
      // This is because these methods are not translated correctly from JSNI,
      // so we instead make a static method that we can safely reference in JSNI.
      if (useStaticDispatch) {
        jsniSig = new StringBuilder(type.getJNISignature()).append(jsniSig);
        final String args = arguments.toString();
        arguments = new StringBuilder();
        if (!isConstructor) {
          arguments.append("o").append(args.length() == 0 ? "" : ",");
        }
        arguments.append(args);
        invokeName = methodName+"$$$";
        final String returnName = cb.addImport(returnType.getErasedType().getQualifiedSourceName());
        final MethodBuffer staticMethod = cb.createMethod("private static "+returnName+" "+invokeName+"()");
        if (!isConstructor) {
          staticMethod.addParameter(typeName, "_");
        }
        if (returns) {
          staticMethod.print("return ");
        }
        if (isConstructor) {
          staticMethod.print("new "+returnName);
        } else {
          staticMethod.print("_.");
        }
        staticMethod.print(methodName).print("(");
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

  protected JMethod getMemberPoolInit(
    final UnifyAstView ast) throws UnableToCompleteException {
    final MemberPoolMethods map = memberPoolMethods.get();
    return map.findGetMembersMethod(ast);
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

  protected Type logLevel() {
    return logLevel;
  }

  protected abstract String memberGetter();

  protected boolean shouldFailIfMissing(final TreeLogger logger, final UnifyAstView ast) {
    return memberPoolMethods.get().shouldFail(logger, ast);
  }

  protected String toString(final JExpression inst) {
    return inst == null ? "null" : inst.getClass().getName()+": "+inst;
  }

  protected Type warnLevel(final TreeLogger logger, final UnifyAstView ast) {
    if (shouldFailIfMissing(logger, ast)) {
     return Type.ERROR;
    }
    return Type.WARN;
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

  private String toParamName(int i) {
    final StringBuilder b = new StringBuilder();
    do {
      b.append((char)('a'+(i%26)));
      i = i / 26;
    } while (i > 0);

    return b.toString();
  }

  /**
   * @param ast
   * @param logger
   * @param callSite
   * @return
   * @throws UnableToCompleteException
   */
  public JExpression throwNotFoundException(final TreeLogger logger, final JMethodCall callSite, final UnifyAstView ast) throws UnableToCompleteException {
    final SourceInfo sourceInfo = callSite.getSourceInfo().makeChild();
    final MemberPoolMethods memberMap = memberPoolMethods.get();
    final IsQualified exceptionType = getNotFoundExceptionType();
    final JConstructor ctor = memberMap.findConstructor(logger, exceptionType.getQualifiedName(), exceptionType.getSimpleName()+"() <init>", ast);
    final JNewInstance newThrowable = new JNewInstance(sourceInfo, ctor);
    final JMethod throwMethod = memberMap.getThrowMethod(logger, ast);
    return new JMethodCall(sourceInfo, null, throwMethod, newThrowable);
  }

  protected abstract IsQualified getNotFoundExceptionType();

  /**
   * @param methodProvider
   * @return
   */
  public boolean isThrowStatement(final JExpression methodProvider) {
    if (methodProvider instanceof JMethodCall) {
      final JMethod method = ((JMethodCall)methodProvider).getTarget();
      return method.getType().getName().equals(GwtReflect.class.getName()) && method.getName().equals("doThrow");
    }
    return false;
  }

}
