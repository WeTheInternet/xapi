package xapi.elemental.api;

import static xapi.collect.X_Collect.newStringMap;
import xapi.collect.api.StringTo;
import xapi.elemental.X_Elemental;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.Stylizer;
import xapi.ui.api.Widget;
import xapi.util.X_String;
import xapi.util.impl.LazyProvider;
import elemental.dom.Element;

public class PotentialNode <E extends Element> extends NodeBuilder <E>{

  public class ClassnameBuilder extends
      AttributeBuilder {

    private final StringTo<String> existing;
    public ClassnameBuilder() {
      super("class");
      existing = newStringMap(String.class);
    }

    @Override
    public <C extends NodeBuilder<String>> C addChild(C child) {
      String value = child.getElement();
      for (String part : value.split("\\s+")) {
        if (part.length() > 0) {
          if (existing.put(part, part) == null) {
            if (existing.size() > 1) {
              part = part + " ";
            }
            super.addChild(wrapChars(part));
          }
        }
      }
      return child;
    }

  }

  public class ApplyLiveAttribute implements AttributeApplier {

    @Override
    public void addAttribute(String name, String value) {
      String is = getAttribute(name);
      if (X_String.isEmpty(is)) {
        setAttribute(name, value);
      } else {
        setAttribute(name, concat(name, is, value));
      }
    }

    protected String concat(String name, String is, String value) {
      return "class".equals(name) ?
          X_Elemental.concatClass(is, value)
          : is.concat(value);
    }

    @Override
    public void setAttribute(String name, String value) {
      getElement().setAttribute(name, value);
    }

    @Override
    public String getAttribute(String name) {
      return getElement().getAttribute(name);
    }

    @Override
    public void removeAttribute(String name) {
      getElement().removeAttribute(name);
    }

  }

  public class ApplyPendingAttribute implements AttributeApplier {

    @Override
    public void addAttribute(String name, String value) {
      AttributeBuilder attr = attributes.get(name);
      if (attr == null) {
        setAttribute(name, value);
      } else {
        concat(attr, value);
      }
    }

    protected void concat(AttributeBuilder attr, String value) {
      attr.addChild(attr.wrapChars(value));
    }

    @Override
    public void setAttribute(String name, String value) {
      attributes.put(name, new AttributeBuilder(value));
    }

    @Override
    public String getAttribute(String name) {
      AttributeBuilder attr = attributes.get(name);
      if (attr == null) {
        return EMPTY;
      }
      return attr.getElement();
    }

    @Override
    public void removeAttribute(String name) {
      attributes.remove(name);
    }
  }

  private class StyleApplier extends AttributeBuilder implements Stylizer<PotentialNode<E>> {

    private final StringTo<AttributeBuilder> styles;

    public StyleApplier() {
      super("style");
      styles = newStringMap(AttributeBuilder.class);
    }

    @Override
    protected void toHtml(Appendable out) {
      for (AttributeBuilder style : styles.values()) {
        style.toHtml(out);
      }
    }

    @Override
    public Stylizer<PotentialNode<E>> applyStyle(
        PotentialNode<E> element,
        String key,
        String value) {
      if (element.el == null) {
        StyleApplier attr = init();
        attr.setStyle(key, value);
      } else {
        if (value == null) {
          element.el.getStyle().removeProperty(key);
        } else {
          element.el.getStyle().setProperty(key, value);
        }
      }
      return this;
    }

    private void setStyle(String key, String value) {
      if (value == null) {
        styles.remove(key);
      } else {
        init();
        styles.put(key, new AttributeBuilder(key+":"+value+";"));
      }
    }

    @SuppressWarnings("unchecked" )
    private StyleApplier init() {
      el = null;
      AttributeBuilder attr = attributes.get("style");
      if (attr == null) {
        attributes.put("style", (attr=this));
      } else {
        assert attr instanceof PotentialNode.StyleApplier;
      }
      return (PotentialNode<E>.StyleApplier) attr;
    }

    public void setValue(String value) {
      E element = PotentialNode.this.el;
      if (element == null) {
        init();
        clearAll();
        styles.clear();
        addValue(value);
      } else {
        element.setAttribute("style", value);
      }
    }

    public void addValue(String value) {
      E element = PotentialNode.this.el;
      for (String part : value.split(";")) {
        String[] parts = part.trim().split(":");
        assert parts.length == 2 : "Malformed style string: "+value
            +"; expected format: key:value;key:value;...";
        if (element == null) {
          setStyle(parts[0], parts[1]);
        } else {
          element.getStyle().setProperty(parts[0], parts[1]);
        }
      }
    }

  }

  private static class AttributeBuilder extends NodeBuilder<String> {

    public AttributeBuilder(CharSequence value) {
      append(value);
    }

    @Override
    public void append(Widget<String> child) {
      append(child.getElement());
    }

    @Override
    protected String create(CharSequence node) {
      return node.toString();
    }

    @Override
    protected NodeBuilder<String> wrapChars(CharSequence body) {
      return new AttributeBuilder(body);
    }

    @Override
    protected void toHtml(Appendable out) {
      super.toHtml(out);
    }

  }

  private String tagName;
  private final StringTo<AttributeBuilder> attributes;
  private AttributeApplier attributeApplier;
  private LazyProvider<StyleApplier> stylizer;

  public PotentialNode() {
    attributes = newStringMap(AttributeBuilder.class);
    attributeApplier = new ApplyPendingAttribute();
    stylizer = new LazyProvider<StyleApplier>(()-> new StyleApplier());
  }
  public PotentialNode(String tagName) {
    this();
    setTagName(tagName);
  }

  public PotentialNode(E element) {
    attributes = newStringMap(AttributeBuilder.class);
    attributeApplier = new ApplyLiveAttribute();
    stylizer = new LazyProvider<StyleApplier>(()-> new StyleApplier());
    el = element;
  }

  public void setAttribute(String name, String value) {
    if ("style".equals(name)) {
      stylizer.get().setValue(value);
    } else {
      attributeApplier.setAttribute(name, value);
    }
  }

  public void setClass(String value) {
    setAttribute("class", value);
  }

  public void addAttribute(String name, String value) {
    switch(name) {
      case "style":
        stylizer.get().addValue(value);
        break;
      case "class":
        AttributeBuilder was = attributes.get(name);
        if (was == null) {
          attributes.put(name, new ClassnameBuilder());
        }
      default:
        attributeApplier.addAttribute(name, value);
    }
  }

  public void removeAttribute(String name) {
    attributeApplier.removeAttribute(name);
  }

  public void setStyle(String name, String value) {
    stylizer.get().applyStyle(this, name, value);
  }

  public void setStyle(String value) {
    setAttribute("style", value);
  }

  public void removeStyle(String name) {
    stylizer.get().applyStyle(this, name, null);
  }

  @Override
  protected E create(CharSequence csq) {
    try {
      return build(csq.toString());
    } finally {
      attributeApplier = new ApplyLiveAttribute();
    }
  }

  protected E build(String html) {
    return X_Elemental.toElement(html.replaceAll("\n", "<br/>"));
  }

  @Override
  public void append(Widget<E> child) {
    getElement().appendChild(child.getElement());
  }

  @Override
  protected NodeBuilder<E> wrapChars(CharSequence body) {
    PotentialNode<E> node = new PotentialNode<E>();
    node.append(body);
    return node;
  }

  @Override
  protected CharSequence getCharsBefore() {
    StringBuilder b = new StringBuilder();
    if (tagName == null || tagName.isEmpty()) {
      assert attributes.isEmpty() : "Cannot have attributes without a tagname";
    } else {
      b.append("<");
      b.append(tagName);
      appendAttributes(b);
      if (isEmpty()) {
        b.append("/");
      }
      b.append(">");
    }
    return b.length() == 0 ? EMPTY : b.toString();
  }

  protected boolean isEmpty() {
    if (super.isChildrenEmpty()) {
      switch(tagName.toLowerCase()) {
        case "br":
        case "img":
          return true;
      }
    }
    return false;
  }

  private void appendAttributes(StringBuilder b) {
    if (!attributes.isEmpty()) {
      for (String attribute : attributes.keys()) {
        b.append(" ").append(attribute).append("='");
        String result = attributes.get(attribute).getElement();
        b.append(sanitizeAttribute(result)).append("'");
      }
    }
  }

  protected String sanitizeAttribute(String result) {
    return result.replaceAll("'", "&apos;");
  }
  @Override
  protected CharSequence getCharsAfter(CharSequence self) {
    if (tagName != null && !isEmpty()) {
      return "</"+tagName+">";
    }
    return EMPTY;
  }

  /**
   * @return the tagName
   */
  public String getTagName() {
    return tagName;
  }

  /**
   * @param tagName the tagName to set
   */
  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  public String toSource() {
    StringBuilder b = new StringBuilder();
    toHtml(b);
    return b.toString().replaceAll("\n", "<br/>");
  }

  @Override
  public E getElement() {
    return super.getElement();
  }

  @Override
  public String toString() {
    return getElement().getOuterHTML();
  }

}
