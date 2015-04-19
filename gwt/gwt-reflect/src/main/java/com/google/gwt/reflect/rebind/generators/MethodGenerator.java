package com.google.gwt.reflect.rebind.generators;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilType;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.thirdparty.xapi.dev.source.ClassBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.MethodBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.SourceBuilder;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsQualified;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public abstract class MethodGenerator extends MemberGenerator implements MagicMethodGenerator {

  protected abstract boolean isDeclared();

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall callSite,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {
    final boolean isDebug = logger.isLoggable(logLevel());
    final boolean isFromGwtReflect = callSite.getArgs().size() > 2;
    final JExpression inst = isFromGwtReflect ? callSite.getArgs().get(0) : callSite.getInstance();


    if (isDebug) {
      logger.log(logLevel(), "Searching for class literal from "+inst.getClass().getName()+": "+inst);
    }

    final JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, inst, ast, false);
    final List<JExpression> args = callSite.getArgs();
    final JExpression arg0 = args.get(isFromGwtReflect?1:0), arg1 = args.get(isFromGwtReflect?2:1);

    if (classLit == null) {
      if (isDebug) {
        logger.log(logLevel(),
            "Non-final class literal used to invoke reflection method; "
                + ReflectionUtilAst.debug(callSite.getInstance()));
      }
      return checkConstPool(ast, callSite, inst, arg0, arg1);
    }
    if (isDebug) {
      logger.log(logLevel(), "Found class literal "+classLit.getRefType().getName());
      logger.log(logLevel(), "Searching for method name from "+arg0.getClass().getName()+": "+arg0);
    }

    final JStringLiteral stringLit = ReflectionUtilAst.extractImmutableNode(logger, JStringLiteral.class, arg0, ast, false);
    if (stringLit == null) {
      if (isDebug) {
        logger.log(logLevel(),
            "Non-final string arg used to retrieve reflection method; "
                + ReflectionUtilAst.debug(arg0));
      }
      return checkConstPool(ast, callSite, inst, arg0, arg1);
    }
    final String name = stringLit.getValue();

    if (isDebug) {
      logger.log(logLevel(), "Found method name "+name);
      logger.log(logLevel(), "Searching for parameter names from "+arg1.getClass().getName()+": "+arg1);
    }

    final JNewArray newArray = ReflectionUtilAst.extractImmutableNode(logger, JNewArray.class, arg1, ast, false);
    if (newArray == null) {
      if (isDebug) {
        logger.log(logLevel(),
            "Non-final array arg used to retrieve reflection method "+name+" "
                + ReflectionUtilAst.debug(arg1));
      }
      return checkConstPool(ast, callSite, inst, arg0, arg1);
    }
    if (isDebug) {
      logger.log(logLevel(), "Found parameter arguments: "+newArray.initializers);
    }

    final ArrayList<JType> params = new ArrayList<JType>();
    if (newArray.initializers == null) {
      assert newArray.dims.size() == 1;
      final JIntLiteral size = ReflectionUtilAst.extractImmutableNode(logger, JIntLiteral.class,
          newArray.dims.get(0), ast, true);
      if (size.getValue() != 0) {
        logger.log(Type.ERROR, "Cannot provide empty arrays to method injectors"
            + " unless they are of size [0].");
        return checkConstPool(ast, callSite, inst, arg0, arg1);
      }
    } else {
      for (final JExpression expr : newArray.initializers) {
        if (isDebug) {
          logger.log(logLevel(), "Resolving parameter argument: "+expr.getClass().getName()+": "+expr);
        }
        final JClassLiteral type = ReflectionUtilAst.extractClassLiteral(logger, expr, ast, false);
        if (type == null) {
          if (isDebug) {
            logger.log(logLevel(),
                "Encountered non-class literal parameter argument "+expr.getClass().getName()+" : "+expr);
          }
          return checkConstPool(ast, callSite, inst, arg0, arg1);
        } else {
          params.add(type.getRefType());
        }
      }
    }

    // We got all our literals; the class, method name and parameter classes
    // now get the requested method
    final ReflectionGeneratorContext ctx = new ReflectionGeneratorContext(logger, classLit, callSite, enclosingMethod, context, ast);
    final String className = classLit.getRefType().getName();
    final JDeclaredType res = ast.searchForTypeBySource(className);
    final JClassType oracleType = ast.getTypeOracle().findType(className);
    final com.google.gwt.core.ext.typeinfo.JMethod method =
        ReflectionUtilType.findMethod(logger, oracleType, name, ReflectionUtilAst.getTypeNames(params), isDeclared());

    if (method == null) {
      // We fail here because the requested method is not findable.
      logger.log(Type.WARN, "Unable to find method " + oracleType.getQualifiedSourceName()+"."+name+ "("+params+").");
      logger.log(Type.WARN, "Did you forget to call StandardGeneratorContext.finish()?");
      logger.log(Type.WARN, "Returning `throw new MethodNotFoundException()` to match JVM behavior");
      return throwNotFoundException(logger, callSite, ast);
    }
    if (logger.isLoggable(Type.TRACE)) {
      logger.log(Type.TRACE, "Found method " + method);
    }

    // now, get or make a handle to the requested method,
    final JMethodCall methodFactory = getMethodProvider(logger, ctx, method, classLit, isDeclared());
    // and return a call to the generated Method provider
    return methodFactory.makeStatement().getExpr();
  }

  public JMethodCall getMethodProvider(final TreeLogger logger, final ReflectionGeneratorContext ctx, final com.google.gwt.core.ext.typeinfo.JMethod method,
      final JClassLiteral classLit, final boolean declaredOnly) throws UnableToCompleteException {
    final String clsName = classLit.getRefType().getName();
    final ReflectionManifest manifest = ReflectionManifest.getReflectionManifest(logger, clsName, ctx.getGeneratorContext());
    final String factoryCls = getOrMakeMethodFactory(logger, ctx, method, method.getEnclosingType(), manifest, declaredOnly);
    ctx.finish(logger);
    JDeclaredType factory = ctx.getAst().searchForTypeBySource(factoryCls);
    factory = ctx.getAst().translate(factory);
    // pull out the static accessor method
    for (final JMethod factoryMethod : factory.getMethods()) {
      if (factoryMethod.isStatic() && factoryMethod.getName().equals("instantiate")) {
        return new JMethodCall(factoryMethod.getSourceInfo(), null, factoryMethod);
      }
    }
    logger.log(Type.ERROR, "Unable to find static initializer for Method subclass "+factoryCls);
    throw new UnableToCompleteException();
  }

  public String getOrMakeMethodFactory(final TreeLogger logger, final ReflectionGeneratorContext ctx, final com.google.gwt.core.ext.typeinfo.JMethod method,
      final com.google.gwt.core.ext.typeinfo.JType classType, final ReflectionManifest manifest, final boolean declaredOnly) throws UnableToCompleteException {
    // get cached manifest for this type
    final String clsName = classType.getQualifiedSourceName();
    final TypeOracle oracle = ctx.getTypeOracle();
    final String name = method.getName();
    final JClassType cls = oracle.findType(clsName);
    if (cls == null) {
      logger.log(Type.ERROR, "Unable to find enclosing class "+clsName);
      throw new UnableToCompleteException();
    }

    final String methodFactoryName = MethodGenerator.getMethodFactoryName(cls, name, method.getParameters());
    JClassType factory;
    final String pkgName = method.getEnclosingType().getPackage().getName();
    factory = oracle.findType(pkgName, methodFactoryName);
    if (factory == null) {
      return generateMethodFactory(logger, ctx, method, methodFactoryName, manifest);
    } else {
      return (pkgName.length()==0?"":pkgName+".")+ methodFactoryName;
    }
  }

  @Override
  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Method";
  }

  public static String getMethodFactoryName(final JClassType type,
    final String name,
    final JParameter[] list) {
    final StringBuilder b = new StringBuilder(type.getName());
    b.append(MemberGenerator.METHOD_SPACER).append(name);
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

  public String generateMethodFactory(final TreeLogger logger,
    final ReflectionGeneratorContext ctx,
    final com.google.gwt.core.ext.typeinfo.JMethod method,
    String factory, final ReflectionManifest manifest)
      throws UnableToCompleteException {


    final JClassType type = method.getEnclosingType();
    final String pkg = type.getPackage().getName();
    factory = factory.replace('.', '_');
    final SourceBuilder<?> out = ctx.tryCreate(PUBLIC | FINAL, pkg, factory);

    if (out == null) {
      // TODO some kind of test to see if structure has changed...
      return pkg + "." + factory;
    }

    final String simpleName = out.getImports().addImport(type.getQualifiedSourceName());
    final String returnType = out.getImports().addImport(method.getReturnType().getErasedType().getQualifiedSourceName());
    final ClassBuffer cb = out.getClassBuffer();
    final String methodType = cb.addImport(Method.class);


    cb.createConstructor(Modifier.PRIVATE);
    cb.createField(methodType, "method", PRIVATE | STATIC);

    final MethodBuffer instantiator = cb
      .addImports(GwtReflect.class, JavaScriptObject.class)
      .createMethod(
        "public static " + methodType + " instantiate()")
        .println("if (method == null) {")
        .indent()
        .println("method = new " + methodType + "(")
        .print(simpleName + ".class, ")
        .print(returnType + ".class, ")
        .print("\"" + method.getName() + "\", ")
        .print(ReflectionUtilType.getModifiers(method) + ", ")
        .println("invoker(), ");


    // Print an array of all annotations retained on this constructor
    final GwtRetention retention = manifest.getRetention(method);
    appendAnnotationSupplier(logger, instantiator, method, retention, ctx);
    instantiator.print(", ");

    // Print the parameter array (also a reference to a constant array)
    appendClassArray(instantiator, method.getParameters(), ctx);
    instantiator.print(", ");

    // Include the throw exceptions
    appendClassArray(instantiator, method.getThrows(), ctx);

    // Finish up the instantiator method
    instantiator
      .println(");")
      .outdent()
      .println("}")
      .returnValue("method");

    // Construct an invoker method
    createInvokerMethod(cb, type, method.getReturnType(), method.getName(),
      method.getParameters(), method.isStatic(), method.isPublic());

    // Possibly log the source we have generated thus far
    if (isDebug(type, ReflectionStrategy.METHOD)) {
      logger.log(Type.INFO, out.toString());
    }

    // Commit the generator context (saves the provider class, and any constants we created)
    ctx.commit(logger);

    return out.getQualifiedName();
  }

  @Override
  protected IsQualified getNotFoundExceptionType() {
    return new IsQualified("java.lang", NoSuchMethodException.class.getSimpleName());
  }

}
