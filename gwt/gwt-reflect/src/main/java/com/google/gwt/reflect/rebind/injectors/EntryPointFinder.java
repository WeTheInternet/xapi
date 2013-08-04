package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JReboundEntryPoint;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JVisitor;

final class EntryPointFinder extends JVisitor {
  JClassType result;
  boolean pastRebound, pastConstructor;
  @Override
  public boolean visit(JClassType x, Context ctx) {
    for (JInterfaceType i : x.getImplements()) {
      if (i.getName().equals("EntryPoint")) {
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

  public Context getContext() {
    return UNMODIFIABLE_CONTEXT;
  }
  
  @Override
  public boolean visit(JReboundEntryPoint x, Context ctx) {
    pastRebound = true;
    x.getEntryCalls().get(0).traverse(this, ctx);;
    return false;
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