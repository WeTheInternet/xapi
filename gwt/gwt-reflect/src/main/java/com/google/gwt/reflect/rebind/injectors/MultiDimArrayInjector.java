package com.google.gwt.reflect.rebind.injectors;

import static com.google.gwt.reflect.rebind.ReflectionUtilAst.extractImmutableNode;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Queue;

public class MultiDimArrayInjector implements MagicMethodGenerator, UnifyAstListener {


  private JMethod registerArray;
  private JMethod newArrayMethod;

  @Override
  public void destroy(final TreeLogger logger) {
    registerArray = null;
    newArrayMethod = null;
  }

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall methodCall, final JMethod enclosingMethod,
      final Context context, final UnifyAstView ast) throws UnableToCompleteException {
    findMethods(ast);
    final JClassLiteral clazz = ReflectionUtilAst.extractClassLiteral(logger, methodCall, 0, ast, false);
    final SourceInfo info = methodCall.getSourceInfo().makeChild();
    if (clazz == null) {
      // A non-literal referenced was passed.  Lets defer to a runtime call that
      // will only succeed if a previous call to Array.newInstance() succeeded
      // with a class literal of sufficient dimensions to be able to lookup
      // the correct type in an internal cache of java.lang.reflect.Array's
      // super sourced class.  We populate that map upon successful injections.
      final JExpression[] args = methodCall.getArgs().toArray(new JExpression[2]);
      return new JMethodCall(info, null, newArrayMethod, args);
    }
    final JProgram prog = ast.getProgram();
    final List<JExpression> args = methodCall.getArgs();
    List<JExpression> emptyDims = Lists.create(), sizedDims;
    JType type = ast.translate(clazz.getRefType());

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
      final JNewArray newArr = extractImmutableNode(logger, JNewArray.class, args.get(1), ast, true);
      sizedDims = newArr.initializers;
    }
    int dimensions = sizedDims.size();
    while (dimensions --> 0) {
      type = prog.getTypeArray(type);
    }

    JClassLiteral classLiteral = null;

    cur = type;
    while (cur instanceof JArrayType) {
      // Add array type wrappers for the number of requested dimensions
      cur = ((JArrayType) cur).getElementType();
    }
    classLiteral = new JClassLiteral(info.makeChild(), cur);
    final List<JExpression> dims = Lists.addAll(sizedDims, emptyDims);
    final JNewArray newArr = new JNewArray(info, (JArrayType)type, dims, null, classLiteral);
    return new JMethodCall(info, null, registerArray, newArr, new JClassLiteral(info.makeChild(), type));
  }

  @Override
  public boolean onUnifyAstPostProcess(final TreeLogger logger, final UnifyAstView ast,
      final UnifyVisitor visitor, final Queue<JMethod> todo) {

    return false;
  }

  @Override
  public void onUnifyAstStart(final TreeLogger logger, final UnifyAstView ast,
      final UnifyVisitor visitor, final Queue<JMethod> todo) {

  }

  private void findMethods(final UnifyAstView ast) {
    final JDeclaredType arrayType = ast.searchForTypeBySource(Array.class.getName());
    for (final JMethod method : arrayType.getMethods()) {
      if ("register".equals(method.getName())) {
        registerArray = ast.translate(method);
        if (newArrayMethod != null) {
          return;
        }
      } else if ("newMultiDimArray".equals(method.getName())) {
        newArrayMethod = ast.translate(method);
        if (registerArray != null) {
          return;
        }
      }
    }
  }

}
