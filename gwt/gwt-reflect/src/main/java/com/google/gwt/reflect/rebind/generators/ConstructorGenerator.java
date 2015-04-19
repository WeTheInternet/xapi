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
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The abstract base class for all constructor generators.  This class handles determining the source of the
 * injection request, assembling the correct parameters and types, then invoking the correct generator methods
 * in MemberGenerator to return instances of Constructor objects.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public abstract class ConstructorGenerator extends MemberGenerator implements MagicMethodGenerator {

  protected abstract boolean isDeclared();

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall callSite,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {
    final boolean isDebug = logger.isLoggable(logLevel());
    final boolean isFromGwtReflect = callSite.getArgs().size() > 1;
    final JExpression inst = isFromGwtReflect ? callSite.getArgs().get(0) : callSite.getInstance();

    if (isDebug) {
      logger.log(logLevel(), "Searching for class literal from "+inst.getClass().getName()+": "+inst);
    }

    final JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, inst, ast, false);
    final List<JExpression> args = callSite.getArgs();
    final JExpression arg0 = args.get(isFromGwtReflect?1:0);

    if (classLit == null) {
      if (isDebug) {
        logger.log(logLevel(),
            "Non-final class literal used to invoke constructor via reflection; "
                + ReflectionUtilAst.debug(callSite.getInstance()));
      }
      return checkConstPool(ast, callSite, inst, arg0);
    }
    if (isDebug) {
      logger.log(logLevel(), "Found class literal "+classLit.getRefType().getName());
      logger.log(logLevel(), "Searching for constructor from signature "+arg0.getClass().getName()+": "+arg0);
    }

    final JNewArray newArray = ReflectionUtilAst.extractImmutableNode(logger, JNewArray.class, arg0, ast, false);
    if (newArray == null) {
      if (isDebug) {
        logger.log(logLevel(),
            "Non-final array arg used to retrieve reflection constructor "
              + classLit.getRefType().getName()+"("
                + ReflectionUtilAst.debug(arg0)+")");
      }
      return checkConstPool(ast, callSite, inst, arg0);
    }
    if (isDebug) {
      logger.log(logLevel(), "Found parameter arguments: "+newArray.initializers);
    }

    final ArrayList<JType> params = new ArrayList<JType>();
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
        return checkConstPool(ast, callSite, inst, arg0);
      } else {
        params.add(type.getRefType());
      }
    }

    // We got all our literals; the class and parameter classes
    // now get the requested constructor
    final ReflectionGeneratorContext ctx = new ReflectionGeneratorContext(logger, classLit, callSite, enclosingMethod, context, ast);
    final JClassType oracleType = ast.getTypeOracle().findType(classLit.getRefType().getName().replace('$', '.'));
    final JConstructor ctor = ReflectionUtilType.findConstructor(logger, oracleType,
        ReflectionUtilAst.getTypeNames(params), isDeclared());

    if (ctor == null) {
      // We fail here because the requested method is not findable.
      logger.log(Type.ERROR, "Unable to find constructor "+classLit.getRefType().getName()+ "("+params+").");
      logger.log(Type.ERROR, "Did you forget to call StandardGeneratorContext.finish()?");
      return null;
    }
    if (logger.isLoggable(Type.TRACE)) {
      logger.log(Type.TRACE, "Found constructor " + ctor);
    }

    // now, get or make a handle to the requested constructor,
    final JMethodCall constructorFactory = getConstructorProvider(logger, ctx, ctor, isDeclared());
    // and return a call to the generated Constructor provider
    return constructorFactory.makeStatement().getExpr();
  }

  public JMethodCall getConstructorProvider(final TreeLogger logger, final ReflectionGeneratorContext ctx,
      final JConstructor ctor, final boolean declared) throws UnableToCompleteException {
    final JClassLiteral classLit = ctx.getClazz();
    final UnifyAstView ast = ctx.getAst();
    final String clsName = classLit.getRefType().getName();
    final ReflectionManifest manifest = ReflectionManifest.getReflectionManifest(logger, clsName, ast.getGeneratorContext());
    final String factoryCls = getOrMakeConstructorFactory(logger, ctx, ctor, ctor.getEnclosingType(), manifest, declared);
    ctx.finish(logger);
    final JDeclaredType factory = ast.searchForTypeBySource(factoryCls);
    // pull out the static accessor method
    for (final JMethod factoryMethod : factory.getMethods()) {
      if (factoryMethod.isStatic() && factoryMethod.getName().equals("instantiate")) {
        return new JMethodCall(factoryMethod.getSourceInfo(), null, factoryMethod);
      }
    }
    logger.log(Type.ERROR, "Unable to find static initializer for Constructor subclass "+factoryCls);
    throw new UnableToCompleteException();
  }

  public String getOrMakeConstructorFactory(final TreeLogger logger, final ReflectionGeneratorContext ctx,
      final JConstructor ctor,
      final com.google.gwt.core.ext.typeinfo.JType classType, final ReflectionManifest manifest, final boolean declaredOnly) throws UnableToCompleteException {
    // get cached manifest for this type
    final String clsName = classType.getQualifiedSourceName();
    final TypeOracle oracle = ctx.getGeneratorContext().getTypeOracle();
    final JClassType cls = oracle.findType(clsName);
    if (cls == null) {
      logger.log(Type.ERROR, "Unable to find enclosing class "+clsName);
      throw new UnableToCompleteException();
    }

    final String constructorFactoryName = ConstructorGenerator.getConstructorFactoryName(cls, ctor.getParameters());
    JClassType factory;
    final String pkgName = ctor.getEnclosingType().getPackage().getName();
    factory = oracle.findType(pkgName, constructorFactoryName);
    if (factory == null) {
      return generateConstructorFactory(logger, ctx, ctor, constructorFactoryName, manifest);
    } else {
      return (pkgName.length()==0?"":pkgName+".")+ constructorFactoryName;
    }
  }

  @Override
  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Constructor";
  }

  public static String getConstructorFactoryName(final JClassType type,
    final JParameter[] list) {
    final StringBuilder b = new StringBuilder(type.getName());
    b.append(MemberGenerator.CONSTRUCTOR_SPACER).append(
      ReflectionUtilType.toUniqueFactory(list, type.getConstructors()));
    return b.toString();
  }

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

    final String simpleName = out.getImports().addImport(type.getQualifiedSourceName());

    final ClassBuffer cb = out.getClassBuffer();

    cb.createConstructor(Modifier.PRIVATE);
    cb.createField("Constructor <" + simpleName + ">", "ctor", PRIVATE | STATIC);

    final MethodBuffer instantiator = cb
      .addImports(Constructor.class, GwtReflect.class, JavaScriptObject.class)
      .createMethod(
        "public static Constructor <" + simpleName + "> instantiate()")
        .println("if (ctor == null) {")
        .indent()
        .println("ctor = new Constructor<" + simpleName + ">(")
        .print(simpleName + ".class, ")
        .print(ReflectionUtilType.getModifiers(ctor) + ", ")
        .println("invoker(), ");


    // Print an array of all annotations retained on this constructor
    final GwtRetention retention = manifest.getRetention(ctor);
    appendAnnotationSupplier(logger, instantiator, ctor, retention, ctx);
    instantiator.print(", ");

    // Print the parameter array (also a reference to a constant array)
    appendClassArray(instantiator, ctor.getParameters(), ctx);
    instantiator.print(", ");

    // Include the throw exceptions
    appendClassArray(instantiator, ctor.getThrows(), ctx);

    // Finish up the instantiator method
    instantiator
      .println(");")
      .outdent()
      .println("}")
      .returnValue("ctor");

    // Construct an invoker method
    createInvokerMethod(cb, type, type, "new", ctor.getParameters(), true, ctor.isPublic());

    // Possibly log the source we have generated thus far
    if (isDebug(type, ReflectionStrategy.CONSTRUCTOR)) {
      logger.log(Type.INFO, out.toString());
    }

    // Commit the generator context (saves the provider class, and any constants we created)
    ctx.commit(logger);

    return out.getQualifiedName();
  }

  /**
   * @see com.google.gwt.reflect.rebind.generators.MemberGenerator#getNotFoundExceptionType()
   */
  @Override
  protected IsQualified getNotFoundExceptionType() {
    return new IsQualified("java.lang", NoSuchMethodException.class.getSimpleName());
  }
}
