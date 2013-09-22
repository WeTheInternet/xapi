package com.google.gwt.reflect.rebind.injectors;

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
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilType;
import com.google.gwt.reflect.rebind.generators.MemberGenerator;

public abstract class AbstractFieldInjector extends MemberGenerator implements MagicMethodGenerator {

  protected abstract boolean isDeclared();

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall callSite,
      JMethod enclosingMethod, Context context, UnifyAstView ast)
      throws UnableToCompleteException {
    
    boolean isFromGwtReflect = callSite.getArgs().size() == 2;
    JExpression inst = isFromGwtReflect ? callSite.getArgs().get(0) : callSite.getInstance();
    JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, inst, ast, false);
    List<JExpression> args = callSite.getArgs();
    JExpression arg0 = args.get(isFromGwtReflect?1:0);
    
    if (classLit == null) {
//      if (logger.isLoggable(Type.DEBUG))
        logger.log(Type.ERROR,
            "Non-final class literal used to invoke reflection field; "
                + ReflectionUtilAst.debug(callSite.getInstance()));
      return checkConstPool(ast, callSite, inst, arg0);
    }
    
    JStringLiteral stringLit = ReflectionUtilAst.extractImmutableNode(logger, JStringLiteral.class, arg0, ast, false);
    if (stringLit == null) {
      if (logger.isLoggable(Type.DEBUG))
        logger.log(Type.DEBUG,
            "Non-final string arg used to retrieve reflection field; "
                + ReflectionUtilAst.debug(arg0));
      return checkConstPool(ast, callSite, inst, arg0);
    }
    String name = stringLit.getValue();
    
    // We got all our literals; the class, method name and parameter classes
    // now get the requested method

    JType ref = classLit.getRefType();
    JClassType oracleType = ast.getTypeOracle().findType(ref.getName().replace('$', '.'));
    com.google.gwt.core.ext.typeinfo.JField field = 
        ReflectionUtilType.findField(logger, oracleType, name, isDeclared()); 
    
    if (field == null) {
      // We fail here because the requested method is not findable.
      if (shouldFailIfMissing(logger, ast, classLit)) {
        logger.log(Type.ERROR, "Unable to find field " + oracleType.getQualifiedSourceName()+"."+name+ ";");
        logger.log(Type.ERROR, "Did you forget to call StandardGeneratorContext.finish()?");
        throw new UnableToCompleteException();
      } else {
        return checkConstPool(ast, callSite, inst, arg0);
      }
    }
    if (logger.isLoggable(Type.TRACE)) {
      logger.log(Type.TRACE, "Found injectable field " + field);
    }
    
    // now, get or make a handle to the requested method,
    JMethodCall methodFactory = getFieldProvider(logger, ast, field, classLit, isDeclared());
    // and return a call to the generated Method provider
    return methodFactory.makeStatement().getExpr();
  }
  
  public JMethodCall getFieldProvider(TreeLogger logger, UnifyAstView ast, com.google.gwt.core.ext.typeinfo.JField field,
      JClassLiteral classLit, boolean declaredOnly) throws UnableToCompleteException {
    String clsName = classLit.getRefType().getName();
    ReflectionManifest manifest = ReflectionManifest.getReflectionManifest(logger, clsName, ast.getGeneratorContext());
    String factoryCls = getOrMakeFieldFactory(logger, ast.getRebindPermutationOracle().getGeneratorContext(), field, field.getEnclosingType(), manifest, declaredOnly);
    ast.getGeneratorContext().finish(logger);
    JDeclaredType factory = ast.searchForTypeBySource(factoryCls);
    // pull out the static accessor method
    for (JMethod factoryMethod : factory.getMethods()) {
      if (factoryMethod.isStatic() && factoryMethod.getName().equals("instantiate")) {
        return new JMethodCall(factoryMethod.getSourceInfo(), null, factoryMethod);
      }
    }
    logger.log(Type.ERROR, "Unable to find static initializer for Field subclass "+factoryCls);
    throw new UnableToCompleteException();
  }
  
  public String getOrMakeFieldFactory(TreeLogger logger, GeneratorContext ctx, com.google.gwt.core.ext.typeinfo.JField field,
      com.google.gwt.core.ext.typeinfo.JType classType, ReflectionManifest manifest, boolean declaredOnly) throws UnableToCompleteException {
    // get cached manifest for this type
    String clsName = classType.getQualifiedSourceName();
    TypeOracle oracle = ctx.getTypeOracle();
    String name = field.getName();
    JClassType cls = oracle.findType(clsName);
    if (cls == null) {
      logger.log(Type.ERROR, "Unable to find enclosing class "+clsName);
      throw new UnableToCompleteException();
    }
      
    String fieldFactoryName = getFieldFactoryName(cls, name);
    JClassType factory;
    String pkgName = field.getEnclosingType().getPackage().getName();
    factory = oracle.findType(pkgName, fieldFactoryName);
    if (factory == null) {
      return generateFieldFactory(logger, ctx, field, fieldFactoryName, manifest);
    } else 
      return (pkgName.length()==0?"":pkgName+".")+ fieldFactoryName;
  }

  @Override
  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Field";
  }
}
