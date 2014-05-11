/**
 *
 */
package xapi.dev.ui.html;

import static xapi.collect.X_Collect.newStringMap;
import static xapi.collect.X_Collect.newStringMultiMap;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;

import xapi.annotation.compile.Import;
import xapi.collect.api.StringTo;
import xapi.collect.api.StringTo.Many;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlTemplate;

/**
 * A collection of data for a given class pertaining to the pertinent html
 * generator annotations that are discovered from any given type (a class plus
 * it's methods).
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class HtmlGeneratorContext extends HtmlGeneratorNode {

  protected final StringTo<JMethod> allMethods = newStringMap(JMethod.class);
  protected final Many<Css> allCss = newStringMultiMap(Css.class);
  protected final JClassType cls;

  protected final StringTo<HtmlGeneratorNode> allNodes = newStringMap(HtmlGeneratorNode.class);
  protected final StringTo<HtmlGeneratorNode> myNodes = newStringMap(HtmlGeneratorNode.class);


  public HtmlGeneratorContext(JClassType cls) {
    super(cls);
    this.cls = cls;

    for (JClassType type : cls.getFlattenedSupertypeHierarchy()) {
      for (JMethod method : type.getMethods()) {
        String enclosingType = method.getEnclosingType().getQualifiedSourceName();
        if (enclosingType.equals(Object.class.getName())) {
          break;
        }
        if (method.isAbstract() || enclosingType.equals(cls.getQualifiedSourceName())) {
          HtmlGeneratorNode node = new HtmlGeneratorNode(method);
          if (!allNodes.containsKey(method.getName())) {
            allNodes.put(method.getName(), node);
          }
          if (method.getEnclosingType() == cls) {
            myNodes.put(method.getName(), node);
          }
        }
      }
    }

    allNodes.put("", this);
  }

  public void clear() {

    allCss.clear();
    allNodes.clear();
  }

  /**
   * @param html
   * @param name
   */
  public void addHtml(String name, Html html) {

    if (html == null)
      return;
    for (El el : html.body()) {
      addEl(name, el);
    }
    for (Css css : html.css()) {
      addCss(name, css);
    }
  }

  /**
   * @param template
   */
  public void addTemplate(String name, HtmlTemplate template) {

    allNodes.get(name).addTemplate(template);
  }

  /**
   * @param name
   * @param imports
   */
  public void addImports(String name, Import... imports) {

    if (imports.length > 0 &&
       (imports.length != 1 || imports[0] != null)) {
      allNodes.get(name).addImports(imports);
    }
  }

  /**
   * @param name
   * @param imports
   */
  public void addImport(String name, Import imports) {

    if (imports != null) {
      allNodes.get(name).addImports(imports);
    }
  }

  public void addCss(String name, Css css) {

    if (css != null) {
      allCss.get(Integer.toString(css.priority())).add(css);
    }
  }

  /**
   * @param name
   * @param el
   */
  public void addEl(String name, El el) {
    if (el != null) {
      allNodes.get(name).addEl(el);
    }
  }

  /**
   * @param name
   * @param method
   */
  public void addMethod(String name, JMethod method) {
    allMethods.put(name, method);
  }

  public Iterable<HtmlTemplate> getNodes(String key) {
    return allNodes.get(key).getTemplates();
  }

  public Iterable<El> getElements(String key) {
    return allNodes.get(key).getElements();
  }

}
