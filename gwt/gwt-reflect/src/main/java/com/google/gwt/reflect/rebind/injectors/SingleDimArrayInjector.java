package com.google.gwt.reflect.rebind.injectors;

import java.util.List;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;

public class SingleDimArrayInjector implements MagicMethodGenerator {

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod enclosingMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    JClassLiteral clazz = ReflectionUtilAst.extractClassLiteral(logger, methodCall, 0, ast);
    JType cur, type = cur =clazz.getRefType();
    JIntLiteral size = ReflectionUtilAst.extractImmutableNode(logger,
        JIntLiteral.class, methodCall.getArgs().get(1), ast, false);

    // Add absent array dimensions in case use supplies a Class[].class
    List<JExpression> dims = Lists.create(size.makeStatement().getExpr());
    while (cur instanceof JArrayType) {
      dims = Lists.add(dims, JAbsentArrayDimension.INSTANCE);
      cur = ((JArrayType)cur).getElementType();
    }

    // Toss on an extra array dimension
    JArrayType arrayType = ast.getProgram().getTypeArray(type);

    // Collect up the class literals
    JClassLiteral classLit = null;
    SourceInfo info = methodCall.getSourceInfo().makeChild();
    cur = arrayType;
    while (cur instanceof JArrayType) {
      classLit = new JClassLiteral(info.makeChild(), cur);
      cur = ((JArrayType) cur).getElementType();
    }

    // Define new array[n]...[]; statement
    JNewArray newArr = new JNewArray(info, arrayType, dims, null, classLit);
    return newArr.makeStatement().getExpr();
  }

}
