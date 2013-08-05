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
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilType;
import com.google.gwt.reflect.rebind.generators.MemberGenerator;

public abstract class AbstractFieldInjector extends MemberGenerator implements MagicMethodGenerator {

  protected abstract boolean isDeclared();

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall,
      JMethod enclosingMethod, Context context, UnifyAstView ast)
      throws UnableToCompleteException {
    
    boolean isFromGwtReflect = methodCall.getArgs().size() == 3;
    JExpression inst = isFromGwtReflect ? methodCall.getArgs().get(0) : methodCall.getInstance();
    JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, inst, false);
    List<JExpression> args = methodCall.getArgs();
    JExpression arg0 = args.get(isFromGwtReflect?1:0), arg1 = args.get(isFromGwtReflect?2:1);
    
    if (classLit == null) {
      if (logger.isLoggable(Type.DEBUG))
        logger.log(Type.DEBUG,
            "Non-final class literal used to invoke reflection method; "
                + ReflectionUtilAst.debug(methodCall.getInstance()));
      return checkConstPool(ast, inst, arg0, arg1);
    }
    
    JStringLiteral stringLit = ReflectionUtilAst.extractImmutableNode(logger, JStringLiteral.class, arg0, false);
    if (stringLit == null) {
      if (logger.isLoggable(Type.DEBUG))
        logger.log(Type.DEBUG,
            "Non-final string arg used to retrieve reflection method; "
                + ReflectionUtilAst.debug(arg0));
      return checkConstPool(ast, inst, arg0, arg1);
    }
    String name = stringLit.getValue();
    
    JNewArray newArray = ReflectionUtilAst.extractImmutableNode(logger, JNewArray.class, arg1, false);
    if (newArray == null) {
      if (logger.isLoggable(Type.DEBUG))
        logger.log(Type.DEBUG,
            "Non-final array arg used to retrieve reflection method "+name+" "
                + ReflectionUtilAst.debug(arg1));
    }
  
    ArrayList<JType> params = new ArrayList<JType>(); 
    for (JExpression expr : newArray.initializers) {
      JClassLiteral type = ReflectionUtilAst.extractClassLiteral(logger, expr, false);
      if (type == null) {
        return checkConstPool(ast, inst, arg0, arg1);
      } else {
        params.add(type.getRefType());
      }
    } 

    // We got all our literals; the class, method name and parameter classes
    // now get the requested method
    JClassType oracleType = ast.getTypeOracle().findType(classLit.getRefType().getName());
    com.google.gwt.core.ext.typeinfo.JMethod method = 
        ReflectionUtilType.findMethod(oracleType, name, ReflectionUtilAst.getTypeNames(params), isDeclared()); 
    
    if (method == null) {
      // We fail here because the requested method is not findable.
      logger.log(Type.ERROR, "Unable to find method " + oracleType.getQualifiedSourceName()+"."+name+ "("+params+").");
      logger.log(Type.ERROR, "Did you forget to call StandardGeneratorContext.finish()?");
      throw new UnableToCompleteException();
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

  protected JExpression checkConstPool(UnifyAstView ast, JExpression inst, JExpression arg0, JExpression arg1) throws UnableToCompleteException {
    for (JField field : ast.getProgram().getTypeJavaLangClass().getFields()) {
      if (field.getName().equals("members")) {
        JDeclaredType result = ast.searchForTypeByBinary(field.getType().getName());
        for (JMethod memberPoolMethod : result.getMethods()) {
          if (memberPoolMethod.getName().equals(memberGetter())) {
            JFieldRef ref = new JFieldRef(memberPoolMethod.getSourceInfo(), inst, field, ast.getProgram().getTypeJavaLangClass());
            JMethodCall methodCall = new JMethodCall(memberPoolMethod.getSourceInfo(), ref, memberPoolMethod);
            methodCall.addArg(arg0);
            methodCall.addArg(arg1);
            return methodCall.makeStatement().getExpr();
          }
        }
      }
    }
    throw new UnableToCompleteException();
  }

  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Method";
  }
}
