package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class CssContainerExpr extends UiExpr {

  private final List<CssSelectorExpr> selectors;
  private final List<CssRuleExpr> rules;

  public CssContainerExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      List<CssSelectorExpr> selectors,
      List<CssRuleExpr> rules
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.selectors = selectors;
    this.rules = rules;
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    for (CssSelectorExpr selector : selectors) {
      R r = selector.accept(v, arg);
      if (r != null) {
        return r;
      }
    }
    for (CssRuleExpr rule : rules) {
      R r = rule.accept(v, arg);
      if (r != null) {
        return r;
      }
    }
    return null;
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    for (CssSelectorExpr selector : selectors) {
      selector.accept(v, arg);
    }
    for (CssRuleExpr rule : rules) {
      rule.accept(v, arg);
    }
  }
}
