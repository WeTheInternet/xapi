package com.google.gwt.reflect.rebind.injectors;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.ext.GeneratorContext;
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
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilType;
import com.google.gwt.reflect.rebind.generators.MemberGenerator;

public abstract class AbstractMethodInjector extends MemberGenerator implements MagicMethodGenerator {

  protected abstract boolean isDeclared();

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall callSite,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {
    final boolean isDebug = logger.isLoggable(logLevel);
    final boolean isFromGwtReflect = callSite.getArgs().size() == 3;
    final JExpression inst = isFromGwtReflect ? callSite.getArgs().get(0) : callSite.getInstance();

    if (isDebug)
      logger.log(logLevel, "Searching for class literal from "+inst.getClass().getName()+": "+inst);
    
    JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, inst, false);
    List<JExpression> args = callSite.getArgs();
    JExpression arg0 = args.get(isFromGwtReflect?1:0), arg1 = args.get(isFromGwtReflect?2:1);
    
    if (classLit == null) {
      if (isDebug)
        logger.log(logLevel,
            "Non-final class literal used to invoke reflection method; "
                + ReflectionUtilAst.debug(callSite.getInstance()));
      return checkConstPool(ast, callSite, inst, arg0, arg1);
    }
    if (isDebug) {
      logger.log(logLevel, "Found class literal "+classLit.getRefType().getName());
      logger.log(logLevel, "Searching for method name from "+arg0.getClass().getName()+": "+arg0);
    }
    
    JStringLiteral stringLit = ReflectionUtilAst.extractImmutableNode(logger, JStringLiteral.class, arg0, false);
    if (stringLit == null) {
      if (isDebug)
        logger.log(logLevel,
            "Non-final string arg used to retrieve reflection method; "
                + ReflectionUtilAst.debug(arg0));
      return checkConstPool(ast, callSite, inst, arg0, arg1);
    }
    String name = stringLit.getValue();
    
    if (isDebug) {
      logger.log(logLevel, "Found method name "+name);
      logger.log(logLevel, "Searching for parameter names from "+arg1.getClass().getName()+": "+arg1);
    }
    
    JNewArray newArray = ReflectionUtilAst.extractImmutableNode(logger, JNewArray.class, arg1, false);
    if (newArray == null) {
      if (isDebug)
        logger.log(logLevel,
            "Non-final array arg used to retrieve reflection method "+name+" "
                + ReflectionUtilAst.debug(arg1));
      return checkConstPool(ast, callSite, inst, arg0, arg1);
    }
    if (isDebug)
      logger.log(logLevel, "Found parameter arguments: "+newArray.initializers);
  
    ArrayList<JType> params = new ArrayList<JType>(); 
    for (JExpression expr : newArray.initializers) {
      if (isDebug)
        logger.log(logLevel, "Resolving parameter argument: "+expr.getClass().getName()+": "+expr);
      JClassLiteral type = ReflectionUtilAst.extractClassLiteral(logger, expr, false);
      if (type == null) {
        if (isDebug)
          logger.log(logLevel,
              "Encountered non-class literal parameter argument "+expr.getClass().getName()+" : "+expr);
        return checkConstPool(ast, callSite, inst, arg0, arg1);
      } else {
        params.add(type.getRefType());
      }
    } 

    // We got all our literals; the class, method name and parameter classes
    // now get the requested method
    JClassType oracleType = ast.getTypeOracle().findType(classLit.getRefType().getName());
    com.google.gwt.core.ext.typeinfo.JMethod method = 
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
    JMethodCall methodFactory = getMethodProvider(logger, ast, method, classLit, isDeclared());
    // and return a call to the generated Method provider
    return methodFactory.makeStatement().getExpr();
  }
  
  public JMethodCall getMethodProvider(TreeLogger logger, UnifyAstView ast, com.google.gwt.core.ext.typeinfo.JMethod method,
      JClassLiteral classLit, boolean declaredOnly) throws UnableToCompleteException {
    String clsName = classLit.getRefType().getName();
    ReflectionManifest manifest = ReflectionManifest.getReflectionManifest(logger, clsName, ast.getGeneratorContext());
    String factoryCls = getOrMakeMethodFactory(logger, ast.getRebindPermutationOracle().getGeneratorContext(), method, method.getEnclosingType(), manifest, declaredOnly);
    ast.getRebindPermutationOracle().getGeneratorContext().finish(logger);
    JDeclaredType factory = ast.searchForTypeBySource(factoryCls);
    // pull out the static accessor method
    for (JMethod factoryMethod : factory.getMethods()) {
      if (factoryMethod.isStatic() && factoryMethod.getName().equals("instantiate")) {
        return new JMethodCall(factoryMethod.getSourceInfo(), null, factoryMethod);
      }
    }
    logger.log(Type.ERROR, "Unable to find static initializer for Method subclass "+factoryCls);
    throw new UnableToCompleteException();
  }
  
  public String getOrMakeMethodFactory(TreeLogger logger, GeneratorContext ctx, com.google.gwt.core.ext.typeinfo.JMethod method,
      com.google.gwt.core.ext.typeinfo.JType classType, ReflectionManifest manifest, boolean declaredOnly) throws UnableToCompleteException {
    // get cached manifest for this type
    String clsName = classType.getQualifiedSourceName();
    TypeOracle oracle = ctx.getTypeOracle();
    String name = method.getName();
    JClassType cls = oracle.findType(clsName);
    if (cls == null) {
      logger.log(Type.ERROR, "Unable to find enclosing class "+clsName);
      throw new UnableToCompleteException();
    }
    
    String methodFactoryName = getMethodFactoryName(cls, name, method.getParameters());
    JClassType factory;
    String pkgName = method.getEnclosingType().getPackage().getName();
    factory = oracle.findType(pkgName, methodFactoryName);
    if (factory == null) {
      return generateMethodFactory(logger, ctx, method, methodFactoryName, manifest);
    } else 
      return (pkgName.length()==0?"":pkgName+".")+ methodFactoryName;
  }

//  protected JExpression checkConstPool(UnifyAstView ast, JExpression inst, JExpression arg0, JExpression arg1) throws UnableToCompleteException {
//    for (JField field : ast.getProgram().getTypeJavaLangClass().getFields()) {
//      if (field.getName().equals("members")) {
//        JDeclaredType result = ast.searchForTypeByBinary(field.getType().getName());
//        for (JMethod memberPoolMethod : result.getMethods()) {
//          if (memberPoolMethod.getName().equals(memberGetter())) {
//            JFieldRef ref = new JFieldRef(memberPoolMethod.getSourceInfo(), inst, field, ast.getProgram().getTypeJavaLangClass());
//            JMethodCall methodCall = new JMethodCall(memberPoolMethod.getSourceInfo(), ref, memberPoolMethod);
//            methodCall.addArg(arg0);
//            methodCall.addArg(arg1);
//            return methodCall.makeStatement().getExpr();
//          }
//        }
//      }
//    }
//    throw new UnableToCompleteException();
//  }

  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Method";
  }
}
