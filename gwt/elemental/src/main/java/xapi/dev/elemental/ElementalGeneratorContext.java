/**
 *
 */
package xapi.dev.elemental;

import java.util.HashMap;
import java.util.Map;

import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.html.HtmlGeneratorNode;
import xapi.dev.ui.html.HtmlGeneratorResult;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.ui.api.View;
import xapi.ui.api.Widget;
import xapi.util.api.ConvertsValue;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.i18n.client.Messages;

import elemental.dom.Element;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class ElementalGeneratorContext {

  public static class ElementalGeneratorResult extends
      HtmlGeneratorResult {

    private String templateName;
    private HtmlGeneratorNode root;

    public ElementalGeneratorResult(JClassType existing, String pkgName, String finalName) {
      super(existing, pkgName, finalName);
    }

    public HtmlGeneratorNode getRoot(JClassType type, TypeOracle typeOracle) {
      if (root == null) {
        if (type == null) {
          type = typeOracle.findType(getFinalName());
        }
        root = new HtmlGeneratorNode(type);
      }
      return root;
    }

    public String getTemplateName() {
      return templateName;
    }

    public void setTemplateName(String finalName) {
      this.templateName = finalName;
    }

    public boolean isTypeAssignable(JClassType templateType) {
      return getSourceType() == null ? false :
        getSourceType().getErasedType().isAssignableFrom(templateType.getErasedType());
    }

    public void printMethodImport(MethodBuffer out, String styleServiceRef) {
      out.println(
        out.addImport(getTemplateName())
        +"."+ElementalGenerator.FIELD_STYLIZE
        +".set("+styleServiceRef+");");
    }

  }

  private static final String
      NAME_CONVERTER = ConvertsValue.class.getName(),
      NAME_CHAR_SEQUENCE = CharSequence.class.getName(),
      NAME_ELEMENT = Element.class.getName(),
      NAME_POTENTIAL_ELEMENT = PotentialNode.class.getName(),
      NAME_SERVICE = ElementalService.class.getName(),
      NAME_VIEW = View.class.getName(),
      NAME_MESSAGES = Messages.class.getName(),
      NAME_WIDGET = Widget.class.getName();
  // NAME_TEMPLATES = SafeHtmlTemplates.class.getName() // TODO implement templates

  private final JClassType
      typeConverter,
      typeCharSequence,
      typeElement,
      typeElementalService,
      typePotentialElemental,
      typeMessages,
      typeView,
      typeWidget;

  private final Map<String, ElementalGeneratorResult> results;

  public ElementalGeneratorContext(TreeLogger logger, UnifyAstView ast) {
    typeConverter = findType(logger, ast, NAME_CONVERTER);
    typeCharSequence = findType(logger, ast, NAME_CHAR_SEQUENCE);
    typeElement = findType(logger, ast, NAME_ELEMENT);
    typeElementalService = findType(logger, ast, NAME_SERVICE);
    typePotentialElemental = findType(logger, ast, NAME_POTENTIAL_ELEMENT);
    typeMessages = findType(logger, ast, NAME_MESSAGES);
    typeView = findType(logger, ast, NAME_VIEW);
    typeWidget = findType(logger, ast, NAME_WIDGET);
    results = new HashMap<String, ElementalGeneratorResult>();
  }

  private JClassType findType(
      TreeLogger logger,
      UnifyAstView ast,
      String name) {
    return ast.getTypeOracle().findType(name);
  }

  /**
   * @return the typeWidget
   */
  public JClassType getTypeWidget() {
    return typeWidget;
  }

  /**
   * @return the type of CharSequence
   */
  public JClassType getTypeCharSequence() {
    return typeCharSequence;
  }

  /**
   * @return the type of PotentialNode
   */
  public JClassType getTypePotentialElement() {
    return typePotentialElemental;
  }

  /**
   * @return the typeElement
   */
  public JClassType getTypeElement() {
    return typeElement;
  }

  /**
   * @return the typeElementalService
   */
  public JClassType getTypeElementalService() {
    return typeElementalService;
  }

  /**
   * @return the typeView
   */
  public JClassType getTypeMessages() {
    return typeMessages;
  }

  /**
   * @return the typeView
   */
  public JClassType getTypeView() {
    return typeView;
  }

  public ElementalGeneratorResult findExistingProvider(String name) {
    return results.get(name);
  }

  public void setExistingProvider(
      String implName,
      ElementalGeneratorResult result) {
    assert !results.containsKey(implName) || results.get(implName) == result
        : "Duplicate results found for "+implName+": "+
        results.get(implName).getFinalName() +" and "+result.getFinalName();
    results.put(implName, result);
  }

  /**
   * @return the typeConverter
   */
  public JClassType getTypeConverter() {
    return typeConverter;
  }
}
