package com.google.gwt.reflect.rebind.injectors;

import java.util.HashMap;
import java.util.Queue;

import xapi.source.read.SourceUtil;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.UnifyAst;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator;
import com.google.gwt.reflect.rebind.generators.MagicClassGenerator;
import com.google.gwt.reflect.rebind.generators.MemberGenerator;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorContext;

public class MagicClassInjector implements MagicMethodGenerator, UnifyAstListener {

  private static final Type logLevel = Type.TRACE;
  private static final ThreadLocal<MagicClassInjector> injector = new ThreadLocal<MagicClassInjector>();

  public static JExpression injectMagicClass(TreeLogger logger, JMethodCall x, JMethod currentMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    // defer to our static instance here
    return injector.get().injectMagic(logger, x, currentMethod, context, ast);
  }

  private final HashMap<String, JMethodCall> classEnhancers = new HashMap<String, JMethodCall>();

  ReflectionStrategy strategy;

  /**
   * Instance created by {@link UnifyAst} before any methods are called.
   */
  public MagicClassInjector() {
    assert injector.get() == null : "You cannot create MagicClassInjector more than once; "
        + " if you need to run parallel gwt compiles, run them in different threads.";
    injector.set(this);
  }

  @Override
  public void destroy(TreeLogger logger) {
    // Tell the generator thread to clean itself up;
    // This will allow us to intelligently handle recompiles.
    // Every recompile should generate fresh results,
    // but we don't want to generate a new type if we recursively access the same type twice.
    MagicClassGenerator.cleanup();


    MemberGenerator.cleanup();
    // Our annotation generator caches types that it has seen.
    // We want to clear these regularly, so our generator
    // can examine cached, generated types, and avoid needless regeneration.
    GwtAnnotationGenerator.cleanup();


    injector.remove();
  }

    /**
     * Call {@link MagicClassGenerator#execImpl(TreeLogger, ReflectionGeneratorContext, JClassType)},
     * then load (and cache) a jjs {@link JMethodCall} to call the generated class enhancer method.
     *
     * @param clsName -> The name of the class to rebind
     * @param params -> The {@link ReflectionGeneratorContext}
     * @return -> JMethod call MyClassEnhancer.enhance(MyClass.class)
     * @throws UnableToCompleteException
     */
    public JMethodCall doRebind(String clsName, ReflectionGeneratorContext params) throws UnableToCompleteException {
      // generate
      params.getLogger().log(logLevel, "Binding magic class for " + clsName);
      String srcName = SourceUtil.toSourceName(params.getClazz().getRefType().getName());
      JClassType type = params.getTypeOracle().findType(srcName);

      if (type == null) {
        params.getLogger().log(Type.ERROR, "Unable to enhance class; "+srcName+" not found by Gwt type oracle");
        throw new UnableToCompleteException();
      }

      StandardGeneratorContext ctx = params.getGeneratorContext();
      String result = MagicClassGenerator.generate(params.getLogger(), params, type);
      ctx.finish(params.getLogger());

      params.getLogger().log(logLevel, "Generated Class Enhancer: " + result);
      JDeclaredType success = params.getAst().searchForTypeBySource(result);

      //Okay, we've generated the correct magic class subtype;
      //Now pull off its static accessor method to enhance and return our class.

      for (JMethod method : success.getMethods()) {
        if (method.isStatic() && method.getName().equals("enhanceClass")) {
          JMethodCall call = new JMethodCall(method.getSourceInfo().makeChild(SourceOrigin.UNKNOWN), null, method);
          call.addArg(params.getClazz().makeStatement().getExpr());
          return call;
        }
      }
      params.getLogger().log(Type.ERROR, "Unable to load .enhanceClass() for "+result);
      throw new UnableToCompleteException();
    }

    @Override
    public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod currentMethod,
      Context context, UnifyAstView ast) throws UnableToCompleteException {

      JClassLiteral clazz = ReflectionUtilAst.extractClassLiteral(logger, methodCall, 0, ast);
      JType type = clazz.getRefType();
      if (type == null) {
        logger.log(Type.WARN, "ClassLiteral with null reftype: "+clazz.toSource());
        return null;
      }
      return get(type.getName(), new ReflectionGeneratorContext(
          logger, clazz, methodCall, currentMethod, context, ast));
    }

    @Override
    public void onUnifyAstStart(TreeLogger logger, UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo) {
      for (final JMethod method : ast.getProgram().getEntryMethods()) {
        if (method.getBody() instanceof JMethodBody) {
          JMethodBody body = (JMethodBody) method.getBody();
          // obtain the entry point
          EntryPointFinder finder = findEntryPoint(logger);
          body.traverse(finder, finder.getContext());
          // find a default strategy
          if (finder.result == null) {
            strategy = MagicClassGenerator.class.getAnnotation(ReflectionStrategy.class);
          } else {
            com.google.gwt.core.ext.typeinfo.JClassType type = ast.getTypeOracle().findType(finder.result.getName());
            strategy = type.getAnnotation(ReflectionStrategy.class);
            if (strategy == null) {
              strategy = type.getPackage().getAnnotation(ReflectionStrategy.class);
              if (strategy == null) {
                // Nothing on the entry point or it's package;
                // use a default instance of the ReflectionStrategy annotation
                strategy = MagicClassGenerator.class.getAnnotation(ReflectionStrategy.class);
              }
            }
          }
          assert strategy != null;
        }
      }
    }

    @Override
    public boolean onUnifyAstPostProcess(TreeLogger logger, UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo) {
      return false;// no per-permutation post processing needed
    }

    public static ReflectionStrategy getDefaultStrategy() {
      return injector.get().strategy;
    }

    /**
     * Implements a caching layer guarding the {@link #initialize(String, ReflectionGeneratorContext)} method
     *
     * @param key
     * @param params
     * @return
     * @throws UnableToCompleteException
     */
    protected JExpression get(String key, ReflectionGeneratorContext params) throws UnableToCompleteException {
      //because we cache results, super dev mode recompiles need to skip the
      //cache if the magic class does not exist, thus we test type presence on every get().

      JDeclaredType type =
        params.getAst().searchForTypeByBinary(params.getClazz().getRefType().getName());
      String typeName = BinaryName.toSourceName(type.getName());
      String generatedName = ReflectionUtilJava.generatedMagicClassName(typeName);
      try {
        params.getAst().searchForTypeBySource(generatedName);
      }catch(NoClassDefFoundError e) {
        classEnhancers.remove(key);
      }
      if (classEnhancers.containsKey(key)) {
        JMethodCall previous = classEnhancers.get(key);
        previous = new JMethodCall(previous, previous.getInstance());
        previous.addArg(params.getClazz().makeStatement().getExpr());
        return previous.makeStatement().getExpr();
      }
      JMethodCall expr = initialize(key, params);
      classEnhancers.put(key, expr);
      expr.setArg(0, params.getClazz().makeStatement().getExpr());
      return expr.makeStatement().getExpr();
    }

  protected JMethodCall initialize(String clsName, ReflectionGeneratorContext params) {
      try {
        return doRebind(clsName, params);
      } catch (Exception e) {
        if (e instanceof RuntimeException) {
          throw (RuntimeException)e;
        }
        throw new RuntimeException("Could not initialize magic class for " + clsName, e);
      }
    }

  private EntryPointFinder findEntryPoint(TreeLogger logger) {
    return new EntryPointFinder(logger);
  }

}
