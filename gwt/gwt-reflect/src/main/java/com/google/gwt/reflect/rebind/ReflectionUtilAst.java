package com.google.gwt.reflect.rebind;

import java.util.ArrayList;
import java.util.List;

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

public final class ReflectionUtilAst {

  private static final Type logLevel = Type.DEBUG;
  
  private ReflectionUtilAst() {}

  public static ArrayList<String> getTypeNames(ArrayList<JType> params) {
    ArrayList<String> list = new ArrayList<String>();
    for (JType param : params) {
      list.add(param.getName());
    }
    return list ;
  }

  public static JClassLiteral extractClassLiteral(TreeLogger logger, JExpression inst, UnifyAstView ast, boolean strict) throws UnableToCompleteException {
    return ReflectionUtilAst.extractImmutableNode(logger, JClassLiteral.class, inst, ast, strict);
  }

  public static JClassLiteral extractClassLiteral(TreeLogger logger, JMethodCall methodCall, int paramPosition, UnifyAstView ast) throws UnableToCompleteException {
    List<JExpression> args = methodCall.getArgs();
    JExpression arg = args.get(paramPosition);
    JClassLiteral classLit = extractClassLiteral(logger, arg, ast, false);
    if (classLit == null) {
      logger.log(Type.ERROR, "The method " +
        methodCall.getTarget().toSource() + " only accepts class literals." +
        " You sent a " + arg.getClass() + " : " + arg.toSource()+" from method "
            + methodCall.toSource()+ " with argumetsn "
                + methodCall.getArgs()+ ";");
      throw new UnableToCompleteException();
    }
    return classLit;
  }

  @SuppressWarnings("unchecked")
  public static <X extends JExpression> X extractImmutableNode(
      TreeLogger logger, Class<X> type, JExpression inst, 
      UnifyAstView ast, boolean strict) throws UnableToCompleteException {
    boolean doLog = logger.isLoggable(logLevel);
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
        JLocal local = ((JLocalRef)inst).getLocal();
        if (local.isFinal()) {
          JExpression localInit = local.getInitializer();
          if (localInit == null) {
            inst = localInit;
          } else {
            return extractImmutableNode(logger, type, localInit, ast, strict);
          }
        } else {
          if (doLog) ReflectionUtilAst.logNonFinalError(logger, inst);
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
        if (doLog) ReflectionUtilAst.logNonFinalError(logger, inst);
      }
    } else if (inst instanceof JCastOperation){
      JCastOperation op = (JCastOperation) inst;
      return extractImmutableNode(logger, type, op.getExpr(), ast, strict);
    } else if (inst instanceof JMethodCall){
      JMethodCall call = (JMethodCall)inst;
      JMethod target = (call).getTarget();
      if (isGetMagicClass(target) || isClassCast(target)) {
        return extractImmutableNode(logger, type, call.getArgs().get(0), ast, strict);
      }
      if (target.isExternal()) {
        target = ast.translate(target);
      }
      JAbstractMethodBody method = target.getBody();
      // TODO: maybe enforce final / static on method calls
      if (method.isNative()) {
        JsniMethodBody jsni = (JsniMethodBody)method;
        if (JClassLiteral.class.isAssignableFrom(type)) {
          List<JsniClassLiteral> literals = jsni.getClassRefs();
          if (literals.size() == 1) {
            // Might want to not allow jsni methods to magically tag class literals...
            return (X)literals.get(0);
          }
        }
      } else {
        JMethodBody java = (JMethodBody)method;
        ArrayList<JReturnStatement> returns = new ArrayList<JReturnStatement>();
        for (JStatement statement : java.getStatements()) {
          if (statement instanceof JReturnStatement)
            returns.add((JReturnStatement)statement);
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
    } catch (Exception e) {
      logger.log(Type.ERROR, "Unknown exception in extractImmutableNode", e);
      throw new UnableToCompleteException();
    }
    return null;
  }

  public static String debug(JExpression inst) {
    return inst.getClass()+" ["+inst.toSource()+"] @"+inst.getSourceInfo();
  }

  private static void failIfStrict(TreeLogger logger, boolean strict,
      JExpression inst, Class<?> type) throws UnableToCompleteException {
    if (strict) {
      logger.log(logLevel, "Unable to acquire a " + type.getCanonicalName()
          + " from "+ReflectionUtilAst.debug(inst));
      throw new UnableToCompleteException();
    }
  }

  private static boolean isGetMagicClass(JMethod target) {
    return 
        (target.getName().equals("magicClass") &&
        target.getEnclosingType().getName().equals(GwtReflect.class.getName()))
        ||
        (target.getName().equals("enhanceClass") &&
            target.getEnclosingType().getName().endsWith(ReflectionUtilJava.MAGIC_CLASS_SUFFIX))
        ;
  }
  
  private static boolean isClassCast(JMethod target) {
    return target.getName().equals("cast") &&
            target.getEnclosingType().getName().equals(Class.class.getName());
  }

  private static boolean isUnknownType(JExpression inst) {
    return !(inst instanceof JParameterRef);
  }

  private static void logNonFinalError(TreeLogger logger, JExpression inst) {
    logger.log(logLevel, "Traced class literal down to a "+ debug(inst)+","
        + " but this member was not marked final."
        + " Aborting class literal search due to lack of determinism.");
  }
  
}
