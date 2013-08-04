package com.google.gwt.reflect.rebind.injectors;

import java.util.HashMap;
import java.util.Queue;

import xapi.source.read.SourceUtil;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.UnifyAst;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.MagicContext;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator;
import com.google.gwt.reflect.rebind.generators.MagicClassGenerator;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorUtil;

public class MagicClassInjector implements MagicMethodGenerator, UnifyAstListener {

  private static final Type logLevel = Type.TRACE;
  private static final ThreadLocal<MagicClassInjector> generator = new ThreadLocal<MagicClassInjector>();

  /**
   * Instance created by {@link UnifyAst} before any methods are called.
   */
  public MagicClassInjector() {
    assert generator.get() == null : "You cannot create MagicClassInjector more than once; "+generator.get().hashCode();
    generator.set(this);
  }

  public static JExpression injectMagicClass(TreeLogger logger, JMethodCall x, JMethod currentMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    // defer to our static instance here
    return generator.get().injectMagic(logger, x, currentMethod, context, ast);
  }

    private final HashMap<String, JExpression> classEnhancers = new HashMap<String, JExpression>();


    protected JExpression initialize(String clsName, MagicContext params) {
      try {
        return doRebind(clsName, params);
      } catch (Exception e) {
        if (e instanceof RuntimeException) throw (RuntimeException)e;
        throw new RuntimeException("Could not initialize magic class for " + clsName, e);
      }
    }

    public JExpression get(String key, MagicContext params) {
      //because we cache results, super dev mode recompiles need to skip the
      //cache if the magic class does not exist, so we test for that state on every get().

      JDeclaredType type =
        params.getAst().searchForTypeBySource(params.getClazz().getRefType().getName());
      String typeName = JGwtCreate.nameOf(type);
      String generatedName = ReflectionGeneratorUtil.generatedMagicClassName(typeName);
      try {
        params.getAst().searchForTypeBySource(generatedName);
      }catch(NoClassDefFoundError e) {
        classEnhancers.remove(key);
      }
      if (classEnhancers.containsKey(key)) {
        return classEnhancers.get(key);
      }
      JExpression expr = initialize(key, params);
      classEnhancers.put(key, expr);
      return expr;
    }

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod currentMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {

    JClassLiteral clazz = ReflectionGeneratorUtil.extractClassLiteral(logger, methodCall, 0);
    SourceInfo info = methodCall.getSourceInfo().makeChild();
    JType type = clazz.getRefType();
    if (type == null) {
      logger.log(Type.WARN, "ClassLiteral with null reftype: "+clazz.toSource());
      return null;
    }
    return get(type.getName(), new MagicContext(
        logger, info, clazz, methodCall, currentMethod, context, ast));
  }

  public JExpression doRebind(String clsName, MagicContext params) throws UnableToCompleteException {
    // generate
    params.getLogger().log(logLevel, "Binding magic class for " + clsName);
    // JType type = params.getClazz().getRefType();
    JDeclaredType type = params.getAst().searchForTypeBySource(params.getClazz().getRefType().getName());

    StandardGeneratorContext ctx = params.getGeneratorContext();
    Class<? extends Generator> generator = MagicClassGenerator.class;

    String result = ctx.runGenerator(params.getLogger(), generator,
      SourceUtil.toSourceName(type.getName()));
    ctx.finish(params.getLogger());

    params.getLogger().log(logLevel, "Generated Class Enhancer: " + result);
    JDeclaredType success = params.getAst().searchForTypeBySource(result);

    //Okay, we've generated the correct magic class subtype;
    //Now pull off its static accessor method to enhance and return our class.

    for (JMethod method : success.getMethods()) {
      if (method.isStatic() && method.getName().equals("enhanceClass")) {
        JMethodCall call = new JMethodCall(method.getSourceInfo(), null, method);
        call.addArg(params.getClazz().makeStatement().getExpr());
        return call;
      }
    }
    params.getLogger().log(Type.ERROR, "Unable to load .enhanceClass() for "+result);
    throw new UnableToCompleteException();
  }

  ReflectionStrategy strategy;
  
  @Override
  public void onUnifyAstStart(UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo) {
    for (final JMethod method : ast.getProgram().getEntryMethods()) {
      if (method.getBody() instanceof JMethodBody) {
        JMethodBody body = (JMethodBody) method.getBody();
        EntryPointFinder finder = new EntryPointFinder();
        body.traverse(finder, finder.getContext());
        if (finder.result == null) {
          strategy = MagicClassGenerator.class.getAnnotation(ReflectionStrategy.class);
        } else {
          com.google.gwt.core.ext.typeinfo.JClassType type = ast.getTypeOracle().findType(finder.result.getName());
          strategy = type.getAnnotation(ReflectionStrategy.class);
          if (strategy == null) {
            strategy = type.getPackage().getAnnotation(ReflectionStrategy.class);
            if (strategy == null) {
              strategy = MagicClassGenerator.class.getAnnotation(ReflectionStrategy.class);
            }
          }
        }
        assert strategy != null;
      }
    }
  }

  @Override
  public boolean onUnifyAstPostProcess(UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo) {
    return false;
  }

  @Override
  public void destroy() {
    // Tell the generator thread to clean itself up;
    // This will allow us to intelligently handle recompiles.
    // Every recompile should generate fresh results,
    // but we don't want to generate a new type if we recursively access the same type twice.
    MagicClassGenerator.cleanup();
    // Our annotation generator caches types that it has seen.
    // We want to clear these regularly, so our generator
    // can examine cached, generated types, and avoid needless regeneration.
    GwtAnnotationGenerator.cleanup();
    
    generator.remove();
  }

  public static ReflectionStrategy getDefaultStrategy() {
    return generator.get().strategy;
  }

}
