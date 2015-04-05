package com.google.gwt.reflect.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.JAbstractMethodBody;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JsniClassLiteral;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.reflect.shared.GwtReflect;

import java.util.ArrayList;
import java.util.List;

public final class ReflectionUtilAst {

  private static final Type logLevel = Type.DEBUG;

  private ReflectionUtilAst() {}

  public static ArrayList<String> getTypeNames(final ArrayList<JType> params) {
    final ArrayList<String> list = new ArrayList<String>();
    for (final JType param : params) {
      list.add(param.getName());
    }
    return list ;
  }

  public static JClassLiteral extractClassLiteral(final TreeLogger logger, final JExpression inst, final UnifyAstView ast, final boolean strict) throws UnableToCompleteException {
    return ReflectionUtilAst.extractImmutableNode(logger, JClassLiteral.class, inst, ast, strict);
  }

  public static JClassLiteral extractClassLiteral(final TreeLogger logger, final JMethodCall methodCall, final int paramPosition, final UnifyAstView ast) throws UnableToCompleteException {
    return extractClassLiteral(logger, methodCall, paramPosition, ast, true);
  }

  public static JClassLiteral extractClassLiteral(final TreeLogger logger,
      final JMethodCall methodCall, final int paramPosition, final UnifyAstView ast,
      final boolean strict) throws UnableToCompleteException {
    final List<JExpression> args = methodCall.getArgs();
    final JExpression arg = args.get(paramPosition);
    final JClassLiteral classLit = extractClassLiteral(logger, arg, ast, false);
    if (strict && classLit == null) {
      logger.log(Type.ERROR, "The method " +
        methodCall.getTarget().toSource() + " only accepts class literals." +
        " You sent a " + arg.getClass() + " : " + arg.toSource()+" from method "
            + methodCall.toSource()+ " with arguments "
                + methodCall.getArgs()+ ";");
      throw new UnableToCompleteException();
    }
    return classLit;
  }

  @SuppressWarnings("unchecked")
  public static <X extends JExpression> X extractImmutableNode(
      final TreeLogger logger, final Class<X> type, JExpression inst,
      final UnifyAstView ast, final boolean strict) throws UnableToCompleteException {
    final boolean doLog = logger.isLoggable(logLevel);
    if (inst == null) {
      failIfStrict(logger, strict, inst, type);
      return null;
    }
    if (doLog) {
      logger.log(logLevel, "Extracting "+type.getName()+" from "+inst.getClass().getName()+": "+inst);
    }
    try {
    if (type.isAssignableFrom(inst.getClass())) {
      // We have a winner!
      return (X)inst;
    } else if (inst instanceof JLocalRef) {
        final JLocal local = ((JLocalRef)inst).getLocal();
        if (local.isFinal()) {
          final JExpression localInit = local.getInitializer();
          if (localInit == null) {
            inst = localInit;
          } else {
            return extractImmutableNode(logger, type, localInit, ast, strict);
          }
        } else {
          if (doLog) {
            ReflectionUtilAst.logNonFinalError(logger, inst);
          }
        }
    } else if (inst instanceof JFieldRef) {
      com.google.gwt.dev.jjs.ast.JField field = ((JFieldRef)inst).getField();
      if (field.isExternal()) {
        field = ast.translate(field);
      }
      if (field.getLiteralInitializer() != null) {
        return extractImmutableNode(logger, type, field.getLiteralInitializer(), ast, strict);
      } else if (field.isFinal()) {
        return extractImmutableNode(logger, type, field.getInitializer(), ast, strict);
      } else {
        logger.log(logLevel, "Not final "+field);
        if (doLog) {
          ReflectionUtilAst.logNonFinalError(logger, inst);
        }
      }
    } else if (inst instanceof JCastOperation){
      final JCastOperation op = (JCastOperation) inst;
      return extractImmutableNode(logger, type, op.getExpr(), ast, strict);
    } else if (inst instanceof JMethodCall){
      final JMethodCall call = (JMethodCall)inst;
      JMethod target = (call).getTarget();
      if (isGetMagicClass(target) || isClassCast(target)) {
        return extractImmutableNode(logger, type, call.getArgs().get(0), ast, strict);
      }
      if (target.isExternal()) {
        target = ast.translate(target);
      }
      final JAbstractMethodBody method = target.getBody();
      // TODO: maybe enforce final / static on method calls
      if (method.isNative()) {
        final JsniMethodBody jsni = (JsniMethodBody)method;
        if (JClassLiteral.class.isAssignableFrom(type)) {
          final List<JsniClassLiteral> literals = jsni.getClassRefs();
          if (literals.size() == 1) {
            // Might want to not allow jsni methods to magically tag class literals...
            return (X)literals.get(0);
          }
        }
      } else {
        final JMethodBody java = (JMethodBody)method;
        final ArrayList<JReturnStatement> returns = new ArrayList<JReturnStatement>();
        for (final JStatement statement : java.getStatements()) {
          if (statement instanceof JReturnStatement) {
            returns.add((JReturnStatement)statement);
          }
        }
        if (returns.size() == 1) {
          return extractImmutableNode(logger, type, returns.get(0).getExpr(), ast, strict);
        } else {
          if (doLog) {
            logger.log(logLevel, "Java "+type.getName()+" provider method must have one " +
              "and only one return statement, which returns a "+ type.getName()+ " " + method);
          }
        }
      }
    } else {
      if (ReflectionUtilAst.isUnknownType(inst)) {
        logger.log(Type.WARN, "Encountered unhandled type while searching for "+
            type.getName()+ ": "+ReflectionUtilAst.debug(inst));
      }
    }
    failIfStrict(logger, strict, inst, type);
    } catch (final Exception e) {
      logger.log(Type.ERROR, "Unknown exception in extractImmutableNode", e);
      throw new UnableToCompleteException();
    }
    return null;
  }

  public static String debug(final JExpression inst) {
    return inst == null ? "null" : inst.getClass()+" ["+inst.toSource()+"] @"+inst.getSourceInfo();
  }

  private static void failIfStrict(final TreeLogger logger, final boolean strict,
      final JExpression inst, final Class<?> type) throws UnableToCompleteException {
    if (strict) {
      logger.log(logLevel, "Unable to acquire a " + type.getCanonicalName()
          + " from "+ReflectionUtilAst.debug(inst));
      throw new UnableToCompleteException();
    }
  }

  private static boolean isGetMagicClass(final JMethod target) {
    return
        (target.getName().equals("magicClass") &&
        target.getEnclosingType().getName().equals(GwtReflect.class.getName()))
        ||
        (target.getName().equals("enhanceClass") &&
            target.getEnclosingType().getName().endsWith(ReflectionUtilJava.MAGIC_CLASS_SUFFIX))
        ;
  }

  private static boolean isClassCast(final JMethod target) {
    return target.getName().equals("cast") &&
            target.getEnclosingType().getName().equals(Class.class.getName());
  }

  private static boolean isUnknownType(final JExpression inst) {
    return !(inst instanceof JParameterRef);
  }

  private static void logNonFinalError(final TreeLogger logger, final JExpression inst) {
    logger.log(logLevel, "Traced class literal down to a "+ debug(inst)+","
        + " but this member was not marked final."
        + " Aborting class literal search due to lack of determinism.");
  }

}
