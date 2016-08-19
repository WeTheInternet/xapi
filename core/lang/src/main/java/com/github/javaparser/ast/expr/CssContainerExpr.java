package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class CssContainerExpr extends CssExpr {

  private List<CssSelectorExpr> selectors;
  private List<CssRuleExpr> rules;

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

  public List<CssSelectorExpr> getSelectors() {
    return selectors;
  }

  public List<CssRuleExpr> getRules() {
    return rules;
  }

  public void setSelectors(List<CssSelectorExpr> selectors) {
    this.selectors = selectors;
  }

  public void setRules(List<CssRuleExpr> rules) {
    this.rules = rules;
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    return v.visit(this, arg);
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    v.visit(this, arg);
  }

  public boolean isSingleClassSelector() {
    return getSelectors().size() == 1 &&
           getSelectors().get(0).getParts().size() == 1 &&
           getSelectors().get(0).getParts().get(0).startsWith(".");
  }
}
