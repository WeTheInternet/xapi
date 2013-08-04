package com.google.gwt.reflect.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;

public class MagicContext {

  private final UnifyAstView ast;
  private final JClassLiteral clazz;
  private final Context context;
  private final JMethod enclosingMethod;
  private final SourceInfo info;
  private final TreeLogger logger;
  private final JMethodCall methodCall;

  public MagicContext(TreeLogger logger, SourceInfo info, JClassLiteral clazz,
    JMethodCall methodCall, JMethod currentMethod, Context context, UnifyAstView ast) {
    this.ast = ast;
    this.clazz = clazz;
    this.context = context;
    this.enclosingMethod = currentMethod;
    this.info = info;
    this.logger = logger;
    this.methodCall = methodCall;
  }

  /**
   * @return the ast
   */
  public UnifyAstView getAst() {
    return ast;
  }

  /**
   * @return the clazz
   */
  public JClassLiteral getClazz() {
    return clazz;
  }

  /**
   * @return the context
   */
  public Context getContext() {
    return context;
  }

  /**
   * @return the currentMethod
   */
  public JMethod getCurrentMethod() {
    return enclosingMethod;
  }

  /**
   * @return the info
   */
  public SourceInfo getSourceInfo() {
    return info;
  }

  /**
   * @return the logger
   */
  public TreeLogger getLogger() {
    return logger;
  }

  /**
   * @return the methodCall
   */
  public JMethodCall getMethodCall() {
    return methodCall;
  }

  public StandardGeneratorContext getGeneratorContext() {
    return getAst().getRebindPermutationOracle().getGeneratorContext();
  }


}
