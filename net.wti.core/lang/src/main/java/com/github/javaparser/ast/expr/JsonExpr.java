package com.github.javaparser.ast.expr;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public abstract class JsonExpr extends UiExpr {

  public JsonExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn) {
    super(beginLine, beginColumn, endLine, endColumn);
  }

}
