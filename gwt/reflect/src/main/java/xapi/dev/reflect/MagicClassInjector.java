package xapi.dev.reflect;


import static xapi.inject.X_Inject.singletonLazy;

import javax.inject.Provider;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.impl.AbstractMultiInitMap;
import xapi.source.read.SourceUtil;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorContext;

@SingletonDefault(implFor = MagicClassInjector.class)
public class MagicClassInjector implements MagicMethodGenerator {

  // Yes, we let you use our jre injection mechanism during gwt injection generation.
  //
  // Third party tools should probably not override this;
  // it's more for end users to experiment with while developing their own plugins,
  // but you can if you want and are fine with upgrading as the api grows.
  //
  // If you need more functionality than provided, submit a pull request
  // and we can make a usable upstream api
  private static final Provider<MagicClassInjector> generator = singletonLazy(MagicClassInjector.class);

  public static JExpression injectMagicClass(TreeLogger logger, JMethodCall x, JMethod currentMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    // defer to our static final instance here
    return generator.get().injectMagic(logger, x, currentMethod, context, ast);
  }

  private class InitMap extends AbstractMultiInitMap<String,JExpression,ReflectionGeneratorContext> {

    public InitMap() {
      super(PASS_THRU);
    }

    @Override
    protected JExpression initialize(String clsName, ReflectionGeneratorContext params) {
      try {
        return doRebind(clsName, params);
      } catch (Exception e) {
        if (e instanceof RuntimeException) throw (RuntimeException)e;
        throw new RuntimeException("Could not initialize magic class for " + clsName, e);
      }
    }
    @Override
    public JExpression get(String key, ReflectionGeneratorContext params) {
      //because we cache results, super dev mode recompiles need to skip the
      //cache if the magic class does not exist, so we test for that state on every get().

      JDeclaredType type =
        params.getAst().searchForTypeBySource(params.getClazz().getRefType().getName());
      String typeName = JGwtCreate.nameOf(type);
      String generatedName = ReflectionUtilJava.generatedMagicClassName(typeName);
      try {
        params.getAst().searchForTypeBySource(generatedName);
      }catch(NoClassDefFoundError e) {
        removeValue(key);
      }
      return super.get(key, params);
    }

  }

  private final InitMap mappedClasses = new InitMap();

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod currentMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    if (methodCall.getArgs().size() != 1) {
      logger.log(Type.ERROR, "X_Reflect.magicClass accepts one and only one argument: a class literal.");
      throw new UnableToCompleteException();
    }
    if (!(methodCall.getArgs().get(0) instanceof JClassLiteral)) {
      logger.log(Type.ERROR, "X_Reflect.magicClass accepts one and only one argument: a class literal." +
        " You sent a " + methodCall.getArgs().get(0).getClass() + " : " +
        methodCall.getArgs().get(0).toSource());
      throw new UnableToCompleteException();
    }
    JClassLiteral clazz = (JClassLiteral)methodCall.getArgs().get(0);
    return mappedClasses.get(clazz.getRefType().getName(), new ReflectionGeneratorContext(logger, clazz, methodCall,
      currentMethod, context, ast));
  }

  public JExpression doRebind(String clsName, ReflectionGeneratorContext params) throws UnableToCompleteException {
    // generate
    params.getLogger().log(Type.INFO, "Binding magic class for " + clsName);
    // JType type = params.getClazz().getRefType();
    JDeclaredType type = params.getAst().searchForTypeBySource(params.getClazz().getRefType().getName());

    StandardGeneratorContext ctx = params.getGeneratorContext();
    Class<? extends Generator> generator = MagicClassGenerator.class;

    String result = ctx.runGenerator(params.getLogger(), generator,
      SourceUtil.toSourceName(type.getName()));
    ctx.finish(params.getLogger());

    params.getLogger().log(Type.INFO, "Generated Class Enhancer: " + result);
    JDeclaredType success = params.getAst().searchForTypeBySource(result);

    //Okay, we've generated the correct magic class subtype;
    //Now pull off its static accessor method to grab our generated class.

    for (JMethod method : success.getMethods()) {
      if (method.isStatic() && method.getName().equals("enhanceClass")) {
        JMethodCall call = new JMethodCall(method.getSourceInfo(), null, method);
        call.addArg(params.getClazz().makeStatement().getExpr());
        return call;
      }
    }
    params.getLogger().log(Type.ERROR, "Unable to load "+result+".enhanceClass()");
    throw new UnableToCompleteException();
  }

}
