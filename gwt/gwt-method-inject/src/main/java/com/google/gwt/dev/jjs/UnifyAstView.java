package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.UnifyAst;

/**
 * A wrapper interface around the {@link UnifyAst}.
 *
 * It gives you access to the {@link JProgram} and {@link RebindPermutationOracle},
 * which allows you to call into regular GWT.create generators,
 * as well as methods to find (and assimilate) source {@link JDeclaredType}s.
 *
 * This interface is used in case the UnifyAst internal api changes,
 * and will only be updated such that the version of net.wetheinter:gwt-method-inject
 * will match the gwt version, rather than the xapi version.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface UnifyAstView {

  /**
   * Log an error tied to a given JNode.
   * @param x - The node to blame for the error.
   * @param errorMessage - The debug error message to print.
   */
  void error(JNode x, String errorMessage);

  /**
   * Finds an assimilates a class file from the given fully.qualified.Binary$Name.
   * <p>
   * All types must be included as source in your gwt.xml modules, or generated dynamically.
   * <p>
   * Make sure you commit any {@link StandardGeneratorContext} before expecting to
   * find generated types this way.
   * <p>
   * See {@link UnifyAst#searchForTypeByBinary(String)}
   * <p>
   * @param binaryTypeName - A binary class name, like com.foo.Enclosing$Inner
   * @return - The type, if it exists
   * @throws NoClassDefFoundError if the type is not found.
   */
  JDeclaredType searchForTypeByBinary(String binaryTypeName);

  /**
   * Finds an assimilates a class file from the given fully.qualified.Source.Name.
   * <p>
   * All types must be included as source in your gwt.xml modules, or generated dynamically.
   * <p>
   * Make sure you commit any {@link StandardGeneratorContext} before expecting to
   * find generated types this way.
   * <p>
   * See {@link UnifyAst#searchForTypeBySource(String)}
   * <p>
   * @param binaryTypeName - A source class name, like com.foo.Enclosing.Inner
   * @return - The type, if it exists
   * @throws NoClassDefFoundError if class is unavailable to gwt classpath
   */
  JDeclaredType searchForTypeBySource(String sourceTypeName);

  /**
   * @return the {@link RebindPermutationOracle} for use in calling gwt generators.
   * <p>
   * An example of how to use gwt generators during magic method injection:
   * <pre>
  // make sure the requested interface is assimilated before generator called
  JDeclaredType type = ast.searchForTypeBySource("some.type.I.want.to.Rebind");
  // get or make a generator context
  StandardGeneratorContext ctx = ast.getRebindPermutationOracle().getGeneratorContext();
  // Run SomeGenerator
  RebindResult result = ctx.runGeneratorIncrementally(logger, SomeGenerator.class, type.getName());
  // commit the generator result
  ctx.finish(logger);
  // Make sure we call searchForTypeBySource to assimilate our new unit
  JDeclaredType answerType = ast.searchForTypeBySource(result.getResultTypeName());
  JExpression result = JGwtCreate.createInstantiationExpression(x.getSourceInfo(), (JClassType)answerType, answerType);
  if (result == null) {
    ast.error(methodCall, "Rebind result '" + answerType + "' has no default (zero argument) constructors");
    return null;
  }
  return result;
     </pre>
   *
   */
  RebindPermutationOracle getRebindPermutationOracle();

  /**
   * @return the {@link JProgram} instance,
   * which has many informative methods about the entire ast graph of your gwt program.
   *
   */
  JProgram getProgram();

  /**
   * @return the {@link TypeOracle} instance,
   * retrieved from getRebindPermutationOracle().getGeneratorContext().getTypeOracle()
   */
  TypeOracle getTypeOracle();

  /**
   * @return the {@link StandardGeneratorContext} instance,
   * retrieved from getRebindPermutationOracle().getGeneratorContext()
   */
  StandardGeneratorContext getGeneratorContext();

  JReferenceType translate(JReferenceType type);

  JClassType translate(JClassType type);

  JDeclaredType translate(JDeclaredType type);

  JField translate(JField field);

  JMethod translate(JMethod method);

  JType translate(JType type);

}
