package com.google.gwt.dev.jjs;

import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.UnifyAst;

public interface UnifyAstView {

  void error(JNode x, String errorMessage);

  /**
   * See {@link UnifyAst#searchForTypeByBinary(String)}
   * @param binaryTypeName - A binary class name, like com.foo.Enclosing$Inner
   * @return - The type, if it exists
   * @throws NoClassDefFoundError if the type is not found.
   */
  JDeclaredType searchForTypeByBinary(String binaryTypeName);

  /**
   * See {@link UnifyAst#searchForTypeBySource(String)}
   * @param binaryTypeName - A source class name, like com.foo.Enclosing.Inner
   * @return - The type, if it exists
   * @throws NoClassDefFoundError if the type is not found.
   */
  JDeclaredType searchForTypeBySource(String sourceTypeName);

  RebindPermutationOracle getRebindPermutationOracle();

  JProgram getProgram();

}
