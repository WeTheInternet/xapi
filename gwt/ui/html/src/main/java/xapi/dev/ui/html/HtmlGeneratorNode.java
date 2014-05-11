package xapi.dev.ui.html;

import static xapi.collect.X_Collect.newList;
import static xapi.collect.X_Collect.newStringMap;

import javax.inject.Named;

import xapi.annotation.compile.Import;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlTemplate;
import xapi.ui.html.api.Style;

import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.safehtml.client.SafeHtmlTemplates.Template;

public class HtmlGeneratorNode {

  private final HasAnnotations source;
  private final String name;
  private final StringTo<HtmlGeneratorNode> children;
  private final IntTo<El> elements;
  final IntTo<HtmlTemplate> templates;
  private final StringTo<HtmlTemplate> namedTemplates;
  private final IntTo<Style> styles;
  private final IntTo<Css> css;
  private final IntTo<Import> imports;
  private final Html html;
  private String elementKey;

  private HtmlGeneratorNode(HasAnnotations source, String name) {
    this.source = source;
    this.name = name;
    this.children = newStringMap(HtmlGeneratorNode.class);
    this.elements = newList(El.class);
    this.styles = newList(Style.class);
    this.namedTemplates = newStringMap(HtmlTemplate.class);
    this.templates = newList(HtmlTemplate.class);
    this.css = newList(Css.class);
    this.imports = newList(Import.class);

    this.html = source.getAnnotation(Html.class);

    addEl(source.getAnnotation(El.class));
    if (html != null) {
      for (El e : html.body()) {
        addEl(e);
      }

      for (HtmlTemplate template : html.templates()) {
        addTemplate(template);
      }

      for (Css css : html.css()) {
        addCss(css);
      }
    }

    addTemplate(source.getAnnotation(HtmlTemplate.class));
    addCss(source.getAnnotation(Css.class));
    addStyle(source.getAnnotation(Style.class));
    addImports(source.getAnnotation(Import.class));
  }

  public HtmlGeneratorNode addTemplate(HtmlTemplate template) {
    if (template != null) {
      // TODO check if the template is import-based, text-based, or both.
      templates.add(template);
      addImports(template.imports());
      if (template.name().length() > 0) {
        namedTemplates.put(template.name(), template);
      }
      Class<?>[] refs = template.references();
      if (refs.length > 0) {
      }
    }
    return this;
  }

  public HtmlGeneratorNode addImports(Import ... imports) {
    if (imports.length > 0 && imports[0] != null) {
      this.imports.addAll(imports);
    }
    return this;
  }

  public HtmlGeneratorNode addStyle(Style ... style) {
    if (style.length > 0 && style[0] != null) {
      this.styles.addAll(style);
    }
    return this;
  }

  public HtmlGeneratorNode addCss(Css css) {
    if (css != null) {
      this.css.add(css);
      addStyle(css.style());
    }
    return this;
  }

  public void addEl(El e) {
    if (e == null) {
      return;
    }
    elements.add(e);
    for (Style style : e.style()) {
      styles.add(style);
    }
    addImports(e.imports());
  }

  public HtmlGeneratorNode(JClassType fromClass) {
    this(fromClass,
        fromClass.isAnnotationPresent(Named.class)
        ? fromClass.getAnnotation(Named.class).value()
        : fromClass.getSimpleSourceName());
    for (JMethod method : fromClass.getMethods()) {
      addChild(new HtmlGeneratorNode(method));
    }
  }


  public HtmlGeneratorNode(JMethod fromMethod) {
    this(fromMethod,
        fromMethod.isAnnotationPresent(Named.class)
        ? fromMethod.getAnnotation(Named.class).value()
        : fromMethod.getName());
  }

  /**
   * @return the source
   */
  public HasAnnotations getSource() {
    return source;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }


  public void addChild(HtmlGeneratorNode node) {
    children.put(node.getName(), node);
  }

  /**
   * @return the html
   */
  public Html getHtml() {
    return html;
  }

  /**
   * @return the elements
   */
  public Iterable<El> getElements() {
    return elements.forEach();
  }

  /**
   * @return the css
   */
  public Iterable<Css> getCss() {
    return css.forEach();
  }

  /**
   * @return the imports
   */
  public Iterable<Import> getImports() {
    return imports.forEach();
  }

  /**
   * @return the styles
   */
  public Iterable<Style> getStyles() {
    return styles.forEach();
  }

  /**
   * @return if there are any elements
   */
  public boolean hasElements() {
    return !elements.isEmpty();
  }

  /**
   * @return if there are any css
   */
  public boolean hasCss() {
    return !css.isEmpty();
  }

  /**
   * @return if there are any imports
   */
  public boolean hasImports() {
    return !imports.isEmpty();
  }

  /**
   * @return if there are any styles
   */
  public boolean hasStyles() {
    return !styles.isEmpty();
  }

  public JClassType isClassType() {
    return source instanceof JClassType ? (JClassType)source : null;
  }

  public JMethod isMethodType() {
    return source instanceof JMethod ? (JMethod)source : null;
  }

  /**
   * @return the actual dom tag name to use for the generated node.
   * Html.ROOT_ELEMENT is translated to "div"
   */
  public String rootElementTag() {
    return html == null ? elements.isEmpty() ? El.DIV : elements.at(0).tag() :
      html.document().equals(Html.ROOT_ELEMENT) ? El.DIV : html.document();
  }

  public boolean isEmpty() {
    if (html == null) {
      return elements.isEmpty();
    }
    if (elements.isEmpty()) {
      // yes, an == reference;
      // In case you want to use "x-root" as an element,
      // the only document value you should use for "empty"
      // is a reference to Html.ROOT_ELEMENT
      if (html.document().equals(Html.ROOT_ELEMENT)) {
        return templates.isEmpty();
      }
    }
    return false;
  }

  public String escape(String template, String methodName, String accessor) {
    int ind = template.indexOf('$');
    if (ind >= 0) {
      //
//      return
    }
    return template;
  }

  public boolean hasTemplates() {
    return !templates.isEmpty();
  }

  public Iterable<HtmlTemplate> getTemplates() {
    return templates.forEach();
  }

  public HtmlGeneratorNode getNode(String name) {
    return children.get(name);
  }

  public void setNameElement(String elementKey) {
    this.elementKey = elementKey;
  }

  /**
   * @return the elementKey
   */
  public String getNameElement() {
    return elementKey;
  }

  public boolean hasChildren() {
    for (HtmlTemplate template : getTemplates()) {
      if (template.wrapsChildren()) {
        return true;
      }
    }
    for (El el : getElements()) {
      for(String html : el.html()) {
        if (html.contains(HtmlTemplate.KEY_CHILDREN)) {
          return true;
        }
      }
    }
    return false;
  }

}
