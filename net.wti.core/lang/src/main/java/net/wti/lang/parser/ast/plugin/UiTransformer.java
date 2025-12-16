package net.wti.lang.parser.ast.plugin;

import net.wti.lang.parser.JavaParser;
import net.wti.lang.parser.ParseException;
import net.wti.lang.parser.ast.Node;
import net.wti.lang.parser.ast.expr.*;
import xapi.fu.Printable;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/11/16.
 */
public class UiTransformer extends Transformer {

  public UiTransformer() {
    setShouldQuote(true);
  }

  private UiTranslatorPlugin plugin;
  private Transformer delegate;

  @Override
  public String onTemplateStart(
      Printable printer, TemplateLiteralExpr template
  ) {
    if (shouldConvertToString(template)) {
      if (delegate == null) {
        normalizeToString(printer, template.getValueWithoutTicks());
      } else {
        delegate.normalizeToString(printer, template.getValueWithoutTicks());
      }
      return DO_NOT_PRINT;
    }
    if (plugin == null) {
      if (delegate != null) {
        return delegate.onTemplateStart(printer, template);
      }
      return super.onTemplateStart(printer, template);
    }
    final UiContainerExpr ui;
    try {
      // For now, only supporting "html" roots
      ui = JavaParser.parseUiContainer("<content>"+template.getValueWithoutTicks()+"</content>");
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    return plugin.transformUi(printer, ui.getBody());
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
