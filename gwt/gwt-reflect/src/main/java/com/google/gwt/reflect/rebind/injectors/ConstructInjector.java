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
import com.google.gwt.reflect.shared.GwtReflect;

import java.lang.reflect.Constructor;

/**
 * This injector is the provider for the {@link GwtReflect#construct(Class, Class[], Object...)} method.
 * <p>
 * This is designed to create a fly-weight invocation of a constructor without actually creating a
 * heavyweight Constructor object.  We will actually generator the regular constructor provider class,
 * but, instead of accessing it as a constructor object, we will simply grab the javascript accessor function,
 * and then invoke it manually using the super-sourced constructor's .create() method, which is what the
 * constructor will (eventually) invoke.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ConstructInjector extends DeclaredConstructorInjector {

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall methodCall,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast) throws UnableToCompleteException {
    final JExpression ctorProvider = super.injectMagic(logger, methodCall, enclosingMethod, context, ast);

    // Find the invoker method

    // call the newInstance method on the result of our ctorProvider
    final JDeclaredType ctor = ast.searchForTypeBySource(Constructor.class.getName());
    for (final JMethod method : ctor.getMethods()) {
      if (method.getName().equals("newInstance")) {
        final JMethodCall call = new JMethodCall(methodCall.getSourceInfo().makeChild(), ctorProvider, method);
        call.addArg(methodCall.getArgs().get(2));
        return call;
      }
    }
    logger.log(Type.ERROR, "Unable to implement GwtReflect.construct from "+methodCall.toSource());
    throw new UnableToCompleteException();
  }

}
