package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.AccessModifier;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.generators.ConstPoolGenerator;
import com.google.gwt.reflect.rebind.generators.MemberGenerator;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorContext;
import com.google.gwt.reflect.shared.GwtReflect;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Queue;

@ReflectionStrategy
public class ConstPoolInjector implements MagicMethodGenerator,
  UnifyAstListener {

  private static final Type logLevel = Type.TRACE;

  @Override
  public void destroy(final TreeLogger logger) {
    ConstPoolGenerator.cleanup();
  }

  @Override
  public JExpression injectMagic(TreeLogger logger,
    final JMethodCall methodCall, final JMethod enclosingMethod,
    final Context context, final UnifyAstView ast)
    throws UnableToCompleteException {

    final ReflectionGeneratorContext ctx = new ReflectionGeneratorContext(
      logger, null, methodCall, enclosingMethod, context, ast);

    final HashMap<String, JPackage> packages = new HashMap<String, JPackage>();
    final LinkedHashMap<JClassType, ReflectionStrategy> retained = new LinkedHashMap<JClassType, ReflectionStrategy>();
    final ReflectionStrategy defaultStrategy = ConstPoolInjector.class
      .getAnnotation(ReflectionStrategy.class);
    final TypeOracle oracle = ctx.getTypeOracle();
    final boolean doLog = logger.isLoggable(logLevel);
    if (doLog) {
      logger = logger.branch(logLevel,
        "Injecting all remaining members into ClassPool");
    }
    for (final com.google.gwt.core.ext.typeinfo.JClassType type : oracle
      .getTypes()) {
      ReflectionStrategy strategy = type
        .getAnnotation(ReflectionStrategy.class);
      if (strategy == null) {
        JPackage pkg = packages.get(type.getPackage().getName());
        if (pkg == null) {
          pkg = type.getPackage();
          packages.put(pkg.getName(), pkg);
        }
        strategy = pkg.getAnnotation(ReflectionStrategy.class);
        if (strategy == null) {
          if (type.findAnnotationInTypeHierarchy(GwtRetention.class) != null) {
            strategy = defaultStrategy;
          }
        }
      }
      if (strategy != null) {
        if (doLog) {
          logger.log(logLevel,
            "Adding type to ConstPool: " + type.getJNISignature());
        }
        retained.put(type, strategy);
      }
    }
    final JDeclaredType gwtReflect = ast.searchForTypeBySource(GwtReflect.class
      .getName());
    JMethod magicClass = null;
    for (final JMethod method : gwtReflect.getMethods()) {
      if (method.getName().equals("magicClass")) {
        magicClass = method;
        break;
      }
    }
    if (magicClass == null) {
      logger.log(Type.ERROR, "Unable to get a handle on GwtReflect.magicClass");
      throw new UnableToCompleteException();
    }

    final SourceInfo methodSource = methodCall.getSourceInfo().makeChild(
      SourceOrigin.UNKNOWN);

    final JMethod newMethod = new JMethod(methodSource, "enhanceAll",
      methodCall.getTarget().getEnclosingType(),
      ast.getProgram().getTypeVoid(),
      false, true, true, AccessModifier.PUBLIC);

    newMethod.setOriginalTypes(ast.getProgram().getTypeVoid(), methodCall
      .getTarget().getOriginalParamTypes());
    final JMethodBody body = new JMethodBody(methodSource);
    newMethod.setBody(body);
    final JBlock block = body.getBlock();
    for (final Entry<JClassType, ReflectionStrategy> type : retained.entrySet()) {
      final JClassType cls = type.getKey();
      if (cls.isPrivate())
      {
        continue;//use a jsni helper instead here.
      }
      if (cls.getName().endsWith(ReflectionUtilJava.MAGIC_CLASS_SUFFIX)
        || cls.getName().contains(MemberGenerator.METHOD_SPACER)
        || cls.getName().contains(MemberGenerator.FIELD_SPACER)
        || cls.getName().contains(MemberGenerator.CONSTRUCTOR_SPACER)) {
        continue;
      }
      final JType asType = ast.getProgram().getFromTypeMap(
        cls.getQualifiedSourceName());
      if (asType == null) {
        continue;
      }
      final JMethodCall call = new JMethodCall(
        methodSource.makeChild(SourceOrigin.UNKNOWN), null, magicClass);
      call.addArg(new JClassLiteral(methodSource
        .makeChild(SourceOrigin.UNKNOWN), asType));
      final JExpression invoke = MagicClassInjector.injectMagicClass(logger,
        call, magicClass, context, ast);
      if (invoke != null) {
        block.addStmt(invoke.makeStatement());
      }
    }
    block.addStmts(((JMethodBody) methodCall.getTarget().getBody())
      .getStatements());
    methodCall.getTarget().getEnclosingType().addMethod(newMethod);
    final JMethodCall call = new JMethodCall(methodSource, null, newMethod);

    ast.finish(logger);
    return call.makeStatement().getExpr();
  }

  @Override
  public boolean onUnifyAstPostProcess(final TreeLogger logger,
    final UnifyAstView ast, final UnifyVisitor visitor,
    final Queue<JMethod> todo) {
    return false;
  }

  @Override
  public void onUnifyAstStart(final TreeLogger logger, final UnifyAstView ast,
    final UnifyVisitor visitor, final Queue<JMethod> todo) {

  }

}
