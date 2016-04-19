package com.github.javaparser.ast.plugin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.expr.UiExpr;
import com.github.javaparser.ast.visitor.DumpVisitor.SourcePrinter;
import com.github.javaparser.ast.visitor.TransformVisitor;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/11/16.
 */
public class UiTransformer extends Transformer {

  private UiTranslatorPlugin plugin;
  private Transformer delegate;

  @Override
  public String onTemplateStart(
      SourcePrinter printer, TemplateLiteralExpr template
  ) {
    if (shouldConvertToString(template)) {
      TransformVisitor.normalizeToString(printer, template.getValueWithoutTicks());
      return DO_NOT_PRINT;
    }
    if (plugin == null) {
      if (delegate != null) {
        return delegate.onTemplateStart(printer, template);
      }
      return super.onTemplateStart(printer, template);
    }
    final UiExpr ui;
    try {
      // For now, only supporting "html" roots
      ui = JavaParser.parseUiContainer(template.getValueWithoutTicks());
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    return plugin.transformUi(printer, ui);
  }

  protected boolean shouldConvertToString(TemplateLiteralExpr template) {
    final Node parent = template.getParentNode();
    if (parent instanceof SingleMemberAnnotationExpr ||
        ( parent instanceof MemberValuePair && parent.getParentNode() instanceof AnnotationExpr)) {
      // Template strings in annotations we will just turn into escaped java strings.
      // If there is a desire to make this behavior pluggable,
      // this method has been left protected to aid you in your journey.
      return true;
    }
    return false;
  }

  public Transformer getDelegate() {
    return delegate;
  }

  public void setDelegate(Transformer delegate) {
    this.delegate = delegate;
  }

  public UiTranslatorPlugin getPlugin() {
    return plugin;
  }

  public void setPlugin(UiTranslatorPlugin plugin) {
    this.plugin = plugin;
  }
}
