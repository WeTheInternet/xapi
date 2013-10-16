package com.google.gwt.reflect.rebind.generators;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;

public class ReflectionGeneratorContext {

  private final UnifyAstView ast;
  private final JClassLiteral clazz;
  private final Context context;
  private final JMethod enclosingMethod;
  private final TreeLogger logger;
  private final JMethodCall methodCall;
  private final Map<String, SourceBuilder<PrintWriter>> builders;
  private ConstPoolGenerator constPool;

  public ReflectionGeneratorContext(TreeLogger logger, JClassLiteral clazz,
    JMethodCall methodCall, JMethod enclosingMethod, Context context, UnifyAstView ast) {
    this.ast = ast;
    this.clazz = clazz;
    this.context = context;
    this.enclosingMethod = enclosingMethod;
    this.logger = logger;
    this.methodCall = methodCall;
    builders = new HashMap<String, SourceBuilder<PrintWriter>>();
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
    return getAst().getGeneratorContext();
  }

  public TypeOracle getTypeOracle() {
    return getGeneratorContext().getTypeOracle();
  }

  public SourceBuilder<?> tryCreate(int modifier, String pkg, String clsName) {
    String fqcn = pkg + "." + clsName;
    SourceBuilder<PrintWriter> builder = builders.get(fqcn);
    if (builder == null) {
      StandardGeneratorContext ctx = getGeneratorContext();
      PrintWriter writer = ctx.tryCreate(getLogger(), pkg, clsName);
      if (writer == null) {
        return null;
      }
      builder = new SourceBuilder<PrintWriter>
          (Modifier.toString(modifier)+" class "+clsName);
      builder.setPackage(pkg);
      builder.setPayload(writer);
      builders.put(fqcn, builder);
    }
    return builder;
  }
  
  public void commit(TreeLogger logger) {
    if (logger == null) logger = this.logger;
    StandardGeneratorContext ctx = getGeneratorContext();
    for (SourceBuilder<PrintWriter> builder : builders.values()) {
      PrintWriter pw = builder.getPayload();
      pw.print(builder.toString());
      ctx.commit(logger, pw);
    }
    builders.clear();
    if (constPool != null) {
      constPool.commit(logger, ctx);
      constPool = null;
    }
  }

  public ConstPoolGenerator getConstPool() {
    if (constPool == null) {
      constPool = ConstPoolGenerator.getGenerator();
    }
    return constPool;
  }


}
