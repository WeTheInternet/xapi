package com.google.gwt.reflect.rebind.injectors;

import static com.google.gwt.reflect.rebind.ReflectionUtilAst.*;

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
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;

public class MultiDimArrayInjector implements MagicMethodGenerator{


  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod enclosingMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    JClassLiteral clazz = ReflectionUtilAst.extractClassLiteral(logger, methodCall, 0, ast);
    JProgram prog = ast.getProgram();
    List<JExpression> args = methodCall.getArgs();
    List<JExpression> emptyDims = Lists.create(), sizedDims;
    JType type = clazz.getRefType();

    JType cur = type;
    while (cur instanceof JArrayType) {
      cur = ((JArrayType)cur).getElementType();
      emptyDims = Lists.add(emptyDims, JAbsentArrayDimension.INSTANCE);
    }
    
    if (args.size() == 3) {
      // we have a typed call to GwtReflect.newArray(Class, int, int); we know we have two dimensions
      // the Array.newInstance call is (Class, [int), which is a length of 2
      sizedDims = Lists.create(
          (JExpression)extractImmutableNode(logger, JIntLiteral.class, args.get(1), ast, true)
          , extractImmutableNode(logger, JIntLiteral.class, args.get(2), ast, true)
          );
    } else {
      assert args.size() == 2 : "Malformed arguments sent to MultiDimArrayInjector; "
          + "the only valid method calls have the signature (Class, int, int) or (Class, [int)";
      // we have an untyped call to Array.newInstance
      // TODO: have a runtime fallback so we can perform non-strict int 
      JNewArray newArr = extractImmutableNode(logger, JNewArray.class, args.get(1), ast, true);
      sizedDims = newArr.initializers;
    }
    int dimensions = sizedDims.size();
    while (dimensions --> 0) {
      type = prog.getTypeArray(type);
    }

    SourceInfo info = methodCall.getSourceInfo().makeChild();
    List<JClassLiteral> classLiterals = Lists.create();

    cur = type;
    while (cur instanceof JArrayType) {
      // Add array type wrappers for the number of requested dimensions
      JClassLiteral classLit = new JClassLiteral(info.makeChild(), cur);
      classLiterals = Lists.add(classLiterals, classLit);
      cur = ((JArrayType) cur).getElementType();
    }
    List<JExpression> dims = Lists.addAll(sizedDims, emptyDims);
    return new JNewArray(info, (JArrayType)type, dims, null, classLiterals);
  }

}
