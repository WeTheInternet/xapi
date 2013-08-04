package com.google.gwt.reflect.rebind.injectors;

import java.util.List;

import com.google.gwt.core.ext.TreeLogger;
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
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorUtil;

public class MultiDimArrayInjector implements MagicMethodGenerator{


  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod enclosingMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    JClassLiteral clazz = ReflectionGeneratorUtil.extractClassLiteral(logger, methodCall, 0);
    JProgram prog = ast.getProgram();
    List<JExpression> args = methodCall.getArgs();
    List<JExpression> dims = Lists.create();
    JType type = clazz.getRefType();
    if (args.size() == 3) {
      // we have a typed call to GwtReflect.newArray(Class, int, int); we know we have two dimensions
      type = prog.getTypeArray(type);
      type = prog.getTypeArray(type);
      dims.add(args.get(1));
      dims.add(args.get(2));
    } else {
      // we have an untyped call to Array.newInstance
      JNewArray newArr = (JNewArray)methodCall.getArgs().get(1);
      int dimensions = newArr.initializers.size();
      while (dimensions --> 0) {
        type = prog.getTypeArray(type);
      }
      dims = newArr.initializers;
    }
    return JNewArray.createDims(methodCall.getSourceInfo().makeChild(), (JArrayType)type, dims)
      .makeStatement().getExpr();
  }

}
