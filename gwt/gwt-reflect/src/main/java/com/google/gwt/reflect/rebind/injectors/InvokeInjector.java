package com.google.gwt.reflect.rebind.injectors;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;

import java.lang.reflect.Method;

public class InvokeInjector extends DeclaredMethodInjector {

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall methodCall,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast) throws UnableToCompleteException {
    final JExpression methodProvider = super.injectMagic(logger, methodCall, enclosingMethod, context, ast);

    if (methodCall.getArgs().size() != 5) {
      logger.log(Type.ERROR, "Method call provided to replace GwtReflect.invoke("
          + "Class<?> cls, String name, Class<?>[] paramTypes, Object inst, Object ... params"
          + ") supplied incorrect number of arguments; you supplied "+methodCall.getArgs().size()
          +" arguments: "+methodCall.getArgs());
      throw new UnableToCompleteException();
    }
    // call the invoke method on the result of our methodProvider
    final JDeclaredType ctor = ast.searchForTypeBySource(Method.class.getName());
    for (final JMethod method : ctor.getMethods()) {
      if (method.getName().equals("invoke")) {
        final JMethodCall call = new JMethodCall(method.getSourceInfo(), methodProvider, method);
        call.addArg(methodCall.getArgs().get(3));
        call.addArg(methodCall.getArgs().get(4));
        return call;
      }
    }
    logger.log(Type.ERROR, "Unable to implement GwtReflect.invoke from "+methodCall.toSource());
    throw new UnableToCompleteException();
  }

}
