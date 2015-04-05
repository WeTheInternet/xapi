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
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilType;

import java.util.List;

public abstract class FieldGenerator extends MemberGenerator implements MagicMethodGenerator {

  protected abstract boolean isDeclared();

  protected JMethodCall getFactoryMethod(final TreeLogger logger, final JMethodCall callSite,
      final JMethod enclosingMethod, final Context context, final JClassLiteral classLit, final JExpression inst, final JExpression arg0, final UnifyAstView ast)
          throws UnableToCompleteException {

    if (classLit == null) {
      if (logger.isLoggable(logLevel())) {
        logger.log(logLevel(),
            "Non-final class literal used to invoke reflection field; "
                + ReflectionUtilAst.debug(callSite.getInstance()));
      }
      return checkConstPool(ast, callSite, inst, arg0);
    }

    final JStringLiteral stringLit = ReflectionUtilAst.extractImmutableNode(logger, JStringLiteral.class, arg0, ast, false);
    if (stringLit == null) {
      if (logger.isLoggable(logLevel())) {
        logger.log(logLevel(),
            "Non-final string arg used to retrieve reflection field; "
                + ReflectionUtilAst.debug(arg0));
      }
      return checkConstPool(ast, callSite, inst, arg0);
    }
    final String name = stringLit.getValue();

    // We got all our literals; the class, method name and parameter classes
    // now get the requested method

    final JType ref = classLit.getRefType();
    final JClassType oracleType = ast.getTypeOracle().findType(ref.getName().replace('$', '.'));
    final com.google.gwt.core.ext.typeinfo.JField field =
        ReflectionUtilType.findField(logger, oracleType, name, isDeclared());

    if (field == null) {
      // We fail here because the requested method is not findable.
      if (shouldFailIfMissing(logger, ast)) {
        logger.log(Type.ERROR, "Unable to find field " + oracleType.getQualifiedSourceName()+"."+name+ ";");
        logger.log(Type.ERROR, "Did you forget to call StandardGeneratorContext.finish()?");
        throw new UnableToCompleteException();
      } else {
        return checkConstPool(ast, callSite, inst, arg0);
      }
    }
    if (logger.isLoggable(logLevel())) {
      logger.log(logLevel(), "Found injectable field " + field);
    }

    // now, get or make a handle to the requested method,
    return getFieldProvider(logger, ast, field, classLit, isDeclared());
  }

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall callSite,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {

    final boolean isFromGwtReflect = callSite.getArgs().size() == 2;
    final JExpression inst = isFromGwtReflect ? callSite.getArgs().get(0) : callSite.getInstance();
    final JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, inst, ast, false);
    final List<JExpression> args = callSite.getArgs();
    final JExpression arg0 = args.get(isFromGwtReflect?1:0);

    // and return a call to the generated Method provider
    return
        getFactoryMethod(logger, callSite, enclosingMethod, context, classLit, inst, arg0, ast)
        .makeStatement().getExpr();
  }

  public JMethodCall getFieldProvider(final TreeLogger logger, final UnifyAstView ast, final com.google.gwt.core.ext.typeinfo.JField field,
      final JClassLiteral classLit, final boolean declaredOnly) throws UnableToCompleteException {
    final String clsName = classLit.getRefType().getName();
    final ReflectionManifest manifest = ReflectionManifest.getReflectionManifest(logger, clsName, ast.getGeneratorContext());
    final String factoryCls = getOrMakeFieldFactory(logger, ast, field, field.getEnclosingType(), manifest, declaredOnly);
    ast.getGeneratorContext().finish(logger);
    final JDeclaredType factory = ast.searchForTypeBySource(factoryCls);
    // pull out the static accessor method
    for (final JMethod factoryMethod : factory.getMethods()) {
      if (factoryMethod.isStatic() && factoryMethod.getName().equals("instantiate")) {
        return new JMethodCall(factoryMethod.getSourceInfo(), null, factoryMethod);
      }
    }
    logger.log(Type.ERROR, "Unable to find static initializer for Field subclass "+factoryCls);
    throw new UnableToCompleteException();
  }

  public String getOrMakeFieldFactory(final TreeLogger logger, final UnifyAstView ast, final com.google.gwt.core.ext.typeinfo.JField field,
      final com.google.gwt.core.ext.typeinfo.JType classType, final ReflectionManifest manifest, final boolean declaredOnly) throws UnableToCompleteException {
    // get cached manifest for this type
    final String clsName = classType.getQualifiedSourceName();
    final TypeOracle oracle = ast.getTypeOracle();
    final String name = field.getName();
    final JClassType cls = oracle.findType(clsName);
    if (cls == null) {
      logger.log(Type.ERROR, "Unable to find enclosing class "+clsName);
      throw new UnableToCompleteException();
    }

    final String fieldFactoryName = getFieldFactoryName(cls, name);
    JClassType factory;
    final String pkgName = field.getEnclosingType().getPackage().getName();
    factory = oracle.findType(pkgName, fieldFactoryName);
    if (factory == null) {
      return generateFieldFactory(logger, ast, field, fieldFactoryName, manifest);
    } else {
      return (pkgName.length()==0?"":pkgName+".")+ fieldFactoryName;
    }
  }

  @Override
  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Field";
  }
}
