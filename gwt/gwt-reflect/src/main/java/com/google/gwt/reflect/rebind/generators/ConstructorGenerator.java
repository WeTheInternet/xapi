package com.google.gwt.reflect.rebind.generators;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
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
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilType;

public abstract class ConstructorGenerator extends MemberGenerator implements MagicMethodGenerator {

  protected abstract boolean isDeclared();

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall callSite,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {
    final boolean isDebug = logger.isLoggable(logLevel());
    final boolean isFromGwtReflect = callSite.getArgs().size() > 1;
    final JExpression inst = isFromGwtReflect ? callSite.getArgs().get(0) : callSite.getInstance();

    if (isDebug)
      logger.log(logLevel(), "Searching for class literal from "+inst.getClass().getName()+": "+inst);
    
    JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, inst, ast, false);
    ReflectionGeneratorContext ctx = new ReflectionGeneratorContext(logger, classLit, callSite, enclosingMethod, context, ast);
    List<JExpression> args = callSite.getArgs();
    JExpression arg0 = args.get(isFromGwtReflect?1:0);
    
    if (classLit == null) {
      if (isDebug)
        logger.log(logLevel(),
            "Non-final class literal used to invoke constructor via reflection; "
                + ReflectionUtilAst.debug(callSite.getInstance()));
      return checkConstPool(ast, callSite, inst, arg0);
    }
    if (isDebug) {
      logger.log(logLevel(), "Found class literal "+classLit.getRefType().getName());
      logger.log(logLevel(), "Searching for constructor from signature "+arg0.getClass().getName()+": "+arg0);
    }
    
    JNewArray newArray = ReflectionUtilAst.extractImmutableNode(logger, JNewArray.class, arg0, ast, false);
    if (newArray == null) {
      if (isDebug)
        logger.log(logLevel(),
            "Non-final array arg used to retrieve reflection constructor "
              + classLit.getRefType().getName()+"("
                + ReflectionUtilAst.debug(arg0)+")");
      return checkConstPool(ast, callSite, inst, arg0);
    }
    if (isDebug)
      logger.log(logLevel(), "Found parameter arguments: "+newArray.initializers);
  
    ArrayList<JType> params = new ArrayList<JType>(); 
    for (JExpression expr : newArray.initializers) {
      if (isDebug)
        logger.log(logLevel(), "Resolving parameter argument: "+expr.getClass().getName()+": "+expr);
      JClassLiteral type = ReflectionUtilAst.extractClassLiteral(logger, expr, ast, false);
      if (type == null) {
        if (isDebug)
          logger.log(logLevel(),
              "Encountered non-class literal parameter argument "+expr.getClass().getName()+" : "+expr);
        return checkConstPool(ast, callSite, inst, arg0);
      } else {
        params.add(type.getRefType());
      }
    } 

    // We got all our literals; the class and parameter classes
    // now get the requested constructor
    JClassType oracleType = ast.getTypeOracle().findType(classLit.getRefType().getName().replace('$', '.'));
    JConstructor ctor = ReflectionUtilType.findConstructor(logger, oracleType,
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
    JMethodCall constructorFactory = getConstructorProvider(logger, ctx, ctor, isDeclared());
    // and return a call to the generated Constructor provider
    return constructorFactory.makeStatement().getExpr();
  }
  
  public JMethodCall getConstructorProvider(TreeLogger logger, ReflectionGeneratorContext ctx,
      JConstructor ctor, boolean declared) throws UnableToCompleteException {
    JClassLiteral classLit = ctx.getClazz();
    UnifyAstView ast = ctx.getAst();
    String clsName = classLit.getRefType().getName();
    ReflectionManifest manifest = ReflectionManifest.getReflectionManifest(logger, clsName, ast.getGeneratorContext());
    String factoryCls = getOrMakeConstructorFactory(logger, ctx, ctor, ctor.getEnclosingType(), manifest, declared);
    ast.getRebindPermutationOracle().getGeneratorContext().finish(logger);
    JDeclaredType factory = ast.searchForTypeBySource(factoryCls);
    // pull out the static accessor method
    for (JMethod factoryMethod : factory.getMethods()) {
      if (factoryMethod.isStatic() && factoryMethod.getName().equals("instantiate")) {
        return new JMethodCall(factoryMethod.getSourceInfo(), null, factoryMethod);
      }
    }
    logger.log(Type.ERROR, "Unable to find static initializer for Constructor subclass "+factoryCls);
    throw new UnableToCompleteException();
  }
  
  public String getOrMakeConstructorFactory(TreeLogger logger, ReflectionGeneratorContext ctx, 
      JConstructor ctor,
      com.google.gwt.core.ext.typeinfo.JType classType, ReflectionManifest manifest, boolean declaredOnly) throws UnableToCompleteException {
    // get cached manifest for this type
    String clsName = classType.getQualifiedSourceName();
    TypeOracle oracle = ctx.getGeneratorContext().getTypeOracle();
    JClassType cls = oracle.findType(clsName);
    if (cls == null) {
      logger.log(Type.ERROR, "Unable to find enclosing class "+clsName);
      throw new UnableToCompleteException();
    }
    
    String constructorFactoryName = getConstructorFactoryName(cls, ctor.getParameters());
    JClassType factory;
    String pkgName = ctor.getEnclosingType().getPackage().getName();
    factory = oracle.findType(pkgName, constructorFactoryName);
    if (factory == null) {
      return generateConstructorFactory(logger, ctx, ctor, constructorFactoryName, manifest);
    } else 
      return (pkgName.length()==0?"":pkgName+".")+ constructorFactoryName;
  }
  
  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Constructor";
  }
}
