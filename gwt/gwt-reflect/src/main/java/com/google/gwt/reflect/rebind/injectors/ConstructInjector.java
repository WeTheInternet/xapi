package com.google.gwt.reflect.rebind.injectors;

import java.lang.reflect.Constructor;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;

public class ConstructInjector extends DeclaredConstructorInjector {

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall,
      JMethod enclosingMethod, Context context, UnifyAstView ast) throws UnableToCompleteException {
    JExpression ctorProvider = super.injectMagic(logger, methodCall, enclosingMethod, context, ast);
    
    // call the newInstance method on the result of our ctorProvider
    JDeclaredType ctor = ast.searchForTypeBySource(Constructor.class.getName());
    for (JMethod method : ctor.getMethods()) {
      if (method.getName().equals("newInstance")) {
        JMethodCall call = new JMethodCall(method.getSourceInfo(), ctorProvider, method);
        call.addArg(methodCall.getArgs().get(2));
        return call;
      }
    }
    logger.log(Type.ERROR, "Unable to implement GwtReflect.construct from "+methodCall.toSource());
    throw new UnableToCompleteException();
  }

}
