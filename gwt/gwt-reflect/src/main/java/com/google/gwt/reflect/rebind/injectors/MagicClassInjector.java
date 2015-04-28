package com.google.gwt.reflect.rebind.injectors;

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
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator;
import com.google.gwt.reflect.rebind.generators.MagicClassGenerator;
import com.google.gwt.reflect.rebind.generators.MemberGenerator;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorContext;
import com.google.gwt.thirdparty.xapi.source.read.SourceUtil;

import java.util.HashMap;
import java.util.Queue;

public class MagicClassInjector implements MagicMethodGenerator, UnifyAstListener {

  public static ReflectionStrategy getDefaultStrategy() {
    return injector.get().strategy;
  }
  public static JExpression injectMagicClass(final TreeLogger logger, final JMethodCall x, final JMethod currentMethod,
      final Context context, final UnifyAstView ast) throws UnableToCompleteException {
    // defer to our static instance here
    return injector.get().injectMagic(logger, x, currentMethod, context, ast);
  }

  private static final Type logLevel = Type.TRACE;

  private static final ThreadLocal<MagicClassInjector> injector = new ThreadLocal<MagicClassInjector>();

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
  public void destroy(final TreeLogger logger) {
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

    ReflectionManifest.cleanup();


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
  public JMethodCall doRebind(final String clsName, final ReflectionGeneratorContext params) throws UnableToCompleteException {
    params.getAst().getGeneratorContext().setCurrentRebindBinaryTypeName(clsName);
    // generate
    params.getLogger().log(logLevel, "Binding magic class for " + clsName);
    final String srcName = SourceUtil.toSourceName(params.getClazz().getRefType().getName());
    final JClassType type = params.getTypeOracle().findType(srcName);

    if (type == null) {
      params.getLogger().log(Type.ERROR, "Unable to enhance class; "+srcName+" not found by Gwt type oracle");
      throw new UnableToCompleteException();
    }

    final StandardGeneratorContext ctx = params.getGeneratorContext();
    final String result = MagicClassGenerator.generate(params.getLogger(), params, type);
    ctx.finish(params.getLogger());

    final UnifyAstView ast = params.getAst();
    params.getLogger().log(logLevel, "Generated Class Enhancer: " + result);
    JDeclaredType success = ast.searchForTypeBySource(result);
    success = ast.translate(success);
    //Okay, we've generated the correct magic class subtype;
    //Now pull off its static accessor method to enhance and return our class.

    for (final JMethod method : success.getMethods()) {
      if (method.isStatic() && method.getName().equals("enhanceClass")) {
        final JMethodCall call = new JMethodCall(method.getSourceInfo().makeChild(SourceOrigin.UNKNOWN), null, ast.translate(method));
        call.addArg(params.getClazz().makeStatement().getExpr());

        // Mark that the enclosing type (GwtReflect) has caused this class enhancer to be generated.
//        final JDeclaredType enclosingType = params.getMethodCall().getTarget().getEnclosingType();
//        params.getAst().recordRebinderTypeForReboundType(clsName, enclosingType.getName());

        return call;
      }
    }
    params.getLogger().log(Type.ERROR, "Unable to load .enhanceClass() for "+result);
    throw new UnableToCompleteException();
  }

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall methodCall, final JMethod currentMethod,
      final Context context, final UnifyAstView ast) throws UnableToCompleteException {

    final JClassLiteral clazz = ReflectionUtilAst.extractClassLiteral(logger, methodCall, 0, ast);
    final JType type = clazz.getRefType();
    if (type == null) {
      logger.log(Type.WARN, "ClassLiteral with null reftype: "+clazz.toSource());
      return null;
    }
    return get(type.getName(), new ReflectionGeneratorContext(
        logger, clazz, methodCall, currentMethod, context, ast));
  }

  @Override
  public boolean onUnifyAstPostProcess(final TreeLogger logger, final UnifyAstView ast, final UnifyVisitor visitor, final Queue<JMethod> todo) {
    return false;// no per-permutation post processing needed
  }

  @Override
  public void onUnifyAstStart(final TreeLogger logger, final UnifyAstView ast, final UnifyVisitor visitor, final Queue<JMethod> todo) {
    for (final JMethod method : ast.getProgram().getEntryMethods()) {
      if (method.getBody() instanceof JMethodBody) {
        final JMethodBody body = (JMethodBody) method.getBody();
        // obtain the entry point
        final EntryPointFinder finder = findEntryPoint(logger);
        body.traverse(finder, finder.getContext());
        // find a default strategy
        if (finder.result == null) {
          strategy = MagicClassGenerator.class.getAnnotation(ReflectionStrategy.class);
        } else {
          final com.google.gwt.core.ext.typeinfo.JClassType type = ast.getTypeOracle().findType(finder.result.getName());
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

  /**
   * Implements a caching layer guarding the {@link #initialize(String, ReflectionGeneratorContext)} method
   *
   * @param key
   * @param params
   * @return
   * @throws UnableToCompleteException
   */
  protected JExpression get(final String key, final ReflectionGeneratorContext params) throws UnableToCompleteException {
    //because we cache results, super dev mode recompiles need to skip the
    //cache if the magic class does not exist, thus we test type presence on every get().

    final JDeclaredType type =
        params.getAst().searchForTypeByBinary(params.getClazz().getRefType().getName());
    final String typeName = BinaryName.toSourceName(type.getName());
    final String generatedName = ReflectionUtilJava.generatedMagicClassName(typeName);
    if (null == params.getAst().searchForTypeBySource(generatedName)) {
      classEnhancers.remove(key);
    }
    if (classEnhancers.containsKey(key)) {
      JMethodCall previous = classEnhancers.get(key);
      previous = new JMethodCall(previous, previous.getInstance());
      previous.addArg(params.getClazz().makeStatement().getExpr());
      return previous.makeStatement().getExpr();
    }
    final JMethodCall expr = initialize(key, params);

    classEnhancers.put(key, expr);
    expr.setArg(0, params.getClazz().makeStatement().getExpr());
    return expr.makeStatement().getExpr();
  }

  protected JMethodCall initialize(final String clsName, final ReflectionGeneratorContext params) {
    try {
      return doRebind(clsName, params);
    } catch (final Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      throw new RuntimeException("Could not initialize magic class for " + clsName, e);
    }
  }

  private EntryPointFinder findEntryPoint(final TreeLogger logger) {
    return new EntryPointFinder(logger);
  }

}
