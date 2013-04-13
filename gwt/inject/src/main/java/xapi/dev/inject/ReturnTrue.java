package xapi.dev.inject;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;

public class ReturnTrue implements MagicMethodGenerator{

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod currentMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    return ast.getProgram().getLiteralBoolean(true);
  }

}
