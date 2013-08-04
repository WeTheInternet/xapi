package com.google.gwt.reflect.rebind.injectors;

import java.util.Arrays;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorUtil;

public class SingleDimArrayInjector implements MagicMethodGenerator {

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod enclosingMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    logger.log(Type.ERROR, enclosingMethod.getEnclosingType()+ enclosingMethod.getSignature());
    JClassLiteral clazz = ReflectionGeneratorUtil.extractClassLiteral(logger, methodCall, 0);
    JArrayType arrayType = ast.getProgram().getTypeArray(clazz.getRefType(), 1);
    JNewArray newArr = JNewArray.createDims(methodCall.getSourceInfo().makeChild(), arrayType, Arrays.asList(
      methodCall.getArgs().get(1)
    ));
    return newArr.makeStatement().getExpr();
  }

}
