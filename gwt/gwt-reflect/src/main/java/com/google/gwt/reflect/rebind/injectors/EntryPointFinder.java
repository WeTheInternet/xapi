package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

final class EntryPointFinder extends JVisitor {

  private static final Type logLevel = Type.INFO;

  JClassType result;
  boolean pastRebound, pastConstructor;
  private final TreeLogger logger;

  class JUnit3EntryPointFinder extends JVisitor {

  }

  public EntryPointFinder(TreeLogger logger) {
    this.logger = logger == null ? new PrintWriterTreeLogger() : logger;
  }

  @Override
  public boolean visit(JClassType x, Context ctx) {
    for (JInterfaceType i : x.getImplements()) {
      if (i.getName().equals("EntryPoint")) {
        if (logger.isLoggable(logLevel)) {
           logger.log(logLevel, "Using entry point class "+x.getName()+" for gwt-reflection module defaults");
        }
        result = x;
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean visit(JNode x, Context ctx) {
    return result == null;
  }

  public final Context getContext() {
    return UNMODIFIABLE_CONTEXT;
  }

  @Override
  public boolean visit(JGwtCreate x, Context ctx) {
    JType type = x.getType();
    if (type instanceof JClassType) {
      for (JInterfaceType iface : ((JClassType)type).getImplements()) {
        if (iface.getName().equals(EntryPoint.class.getName())) {
          pastRebound = true;
          break;
        }
      }
    }
    return super.visit(x, ctx);
  }

  @Override
  public boolean visit(JNewInstance x, Context ctx) {
    x.getTarget().traverse(this, ctx);
    return false;
  }

  @Override
  public boolean visit(JConstructor x, Context ctx) {
    if (pastRebound) {
      for (JMethod m : x.getEnclosingType().getMethods()) {
        if (m.getName().equals("createNewTestCase")) {
          for (JStatement statement : ((JMethodBody)m.getBody()).getBlock().getStatements()) {
            if (statement instanceof JIfStatement) {
              pastConstructor = true;
              ((JIfStatement)statement).getThenStmt().traverse(this, ctx);
              return false;
            }
          }
        }

      }
    }
    return super.visit(x, ctx);
  }

  @Override
  public boolean visit(JClassLiteral x, Context ctx) {
    result = (JClassType)x.getRefType();
    return false;
  }

}