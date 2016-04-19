package com.github.javaparser.ast.expr;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public abstract class UiExpr extends LiteralExpr {

  protected static final ThreadLocal<Integer> depth = new ThreadLocal<Integer>(){
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };

  public UiExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn) {
    super(beginLine, beginColumn, endLine, endColumn);
  }

}
