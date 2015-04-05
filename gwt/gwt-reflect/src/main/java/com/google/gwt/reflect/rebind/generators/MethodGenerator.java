package com.google.gwt.reflect.rebind.generators;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
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
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilType;

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
    final JClassType oracleType = ast.getTypeOracle().findType(classLit.getRefType().getName());
    final com.google.gwt.core.ext.typeinfo.JMethod method =
        ReflectionUtilType.findMethod(logger, oracleType, name, ReflectionUtilAst.getTypeNames(params), isDeclared());

    if (method == null) {
      // We fail here because the requested method is not findable.
      logger.log(Type.ERROR, "Unable to find method " + oracleType.getQualifiedSourceName()+"."+name+ "("+params+").");
      logger.log(Type.ERROR, "Did you forget to call StandardGeneratorContext.finish()?");
      return null;
    }
    if (logger.isLoggable(Type.TRACE)) {
      logger.log(Type.TRACE, "Found method " + method);
    }

    // now, get or make a handle to the requested method,
    final JMethodCall methodFactory = getMethodProvider(logger, ast, method, classLit, isDeclared());
    // and return a call to the generated Method provider
    return methodFactory.makeStatement().getExpr();
  }

  public JMethodCall getMethodProvider(final TreeLogger logger, final UnifyAstView ast, final com.google.gwt.core.ext.typeinfo.JMethod method,
      final JClassLiteral classLit, final boolean declaredOnly) throws UnableToCompleteException {
    final String clsName = classLit.getRefType().getName();
    final ReflectionManifest manifest = ReflectionManifest.getReflectionManifest(logger, clsName, ast.getGeneratorContext());
    final String factoryCls = getOrMakeMethodFactory(logger, ast, method, method.getEnclosingType(), manifest, declaredOnly);
    ast.getRebindPermutationOracle().getGeneratorContext().finish(logger);
    final JDeclaredType factory = ast.searchForTypeBySource(factoryCls);
    // pull out the static accessor method
    for (final JMethod factoryMethod : factory.getMethods()) {
      if (factoryMethod.isStatic() && factoryMethod.getName().equals("instantiate")) {
        return new JMethodCall(factoryMethod.getSourceInfo(), null, factoryMethod);
      }
    }
    logger.log(Type.ERROR, "Unable to find static initializer for Method subclass "+factoryCls);
    throw new UnableToCompleteException();
  }

  public String getOrMakeMethodFactory(final TreeLogger logger, final UnifyAstView ast, final com.google.gwt.core.ext.typeinfo.JMethod method,
      final com.google.gwt.core.ext.typeinfo.JType classType, final ReflectionManifest manifest, final boolean declaredOnly) throws UnableToCompleteException {
    // get cached manifest for this type
    final String clsName = classType.getQualifiedSourceName();
    final TypeOracle oracle = ast.getTypeOracle();
    final String name = method.getName();
    final JClassType cls = oracle.findType(clsName);
    if (cls == null) {
      logger.log(Type.ERROR, "Unable to find enclosing class "+clsName);
      throw new UnableToCompleteException();
    }

    final String methodFactoryName = getMethodFactoryName(cls, name, method.getParameters());
    JClassType factory;
    final String pkgName = method.getEnclosingType().getPackage().getName();
    factory = oracle.findType(pkgName, methodFactoryName);
    if (factory == null) {
      return generateMethodFactory(logger, ast, method, methodFactoryName, manifest);
    } else {
      return (pkgName.length()==0?"":pkgName+".")+ methodFactoryName;
    }
  }

  @Override
  protected Type logLevel() {
    return super.logLevel();
  }

  @Override
  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Method";
  }
}
