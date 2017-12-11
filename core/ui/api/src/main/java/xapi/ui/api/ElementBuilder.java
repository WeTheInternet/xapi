package xapi.ui.api;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.util.X_Debug;
import xapi.util.X_String;
import xapi.util.impl.LazyProvider;

import static xapi.collect.X_Collect.newStringMap;

import javax.inject.Provider;

/**
 * Created by james on 16/10/15.
 */
public abstract class ElementBuilder <E> extends NodeBuilder<E> {

  private static volatile int idSeed = 1;
  private static final Object sync = new Object();

  private int seed;

  public String ensureId() {
    if (X_String.isEmpty(id)) {
      synchronized (sync) {
        seed = idSeed++;
      }
      setId(generateId(seed));
    }
    return id;
  }

  public static String getDefaultPrefix() {
    return System.getProperty("data.attr.prefix", "xapi-");
  }

  protected String generateId(int seed) {
    return "ele_"+seed;
  }

  private String id;

  public String getId() {
    return id;
  }

  public String getId(boolean forceCreate) {
    if (forceCreate) {
      ensureId();
    }
    return id;
  }

  @SuppressWarnings("unused")
  public abstract class StyleApplier extends AttributeBuilder implements Stylizer<NodeBuilder<E>> {

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
    public Stylizer<NodeBuilder<E>> applyStyle(
        NodeBuilder<E> element,
        String key,
        String value) {
      if (element.el == null) {
        StyleApplier attr = init();
        attr.setStyle(key, value);
      } else {
        if (value == null) {
          removeStyle(element.el, key);
        } else {
          setStyle(element.el, key, value);
        }
      }
      return this;
    }

    public void setStyle(String key, String value) {
      if (value == null) {
        styles.remove(key);
      } else {
        init();
        styles.put(key, newAttributeBuilder(key+":"+value+";"));
      }
    }

    @SuppressWarnings("unchecked" )
    private StyleApplier init() {
      el = null;// reset our string so that we recompute it next time
      NodeBuilder.AttributeBuilder attr = attributes.get("style");
      if (attr == null) {
        attributes.put("style", (attr=this));
      } else {
        if (!(attr instanceof ElementBuilder.StyleApplier)) {
        assert false : "Only use the setStyle method to set the 'style' attribute";
        throw X_Debug.recommendAssertions();
        }
      }
      return (StyleApplier) attr;
    }

    protected void setValue(String value) {
      E element = ElementBuilder.this.el;
      if (element == null) {
        init();
        clearAll();
        styles.clear();
        addValue(value);
      } else {

        attributeApplier.get().setAttribute("style", value);
      }
    }

    public void addValue(String value) {

      for (String part : value.split(";")) {
        String[] parts = part.trim().split(":");
        assert parts.length == 2 : "Malformed style string: "+value
            +"; expected format: key:value;key:value;...";
        stylize(parts[0], parts[1]);
      }
    }

    public StyleApplier stylize(String key, String value) {
      E element = ElementBuilder.this.el;
      if (element == null) {
        setStyle(key.toLowerCase(), value);
      } else {
        setStyle(element, key.replace("[-]", ""), value);
      }
      return this;
    }

    protected abstract void removeStyle(E element, String key);
    protected abstract void setStyle(E element, String key, String value);

    public StyleApplier setDisplay(String display) {
      stylize("display", display);
      return this;
    }

    public StyleApplier setPosition(String position) {
      stylize("position", position);
      return this;
    }

    public StyleApplier setLeft(String left) {
      stylize("left", left);
      return this;
    }

    public StyleApplier setTop(String top) {
      stylize("top", top);
      return this;
    }

    public StyleApplier setBottom(String bottom) {
      stylize("bottom", bottom);
      return this;
    }

    public StyleApplier setRight(String right) {
      stylize("right", right);
      return this;
    }

    public StyleApplier setWidth(String width) {
      stylize("width", width);
      return this;
    }

    public StyleApplier setHeight(String width) {
      stylize("width", width);
      return this;
    }

    public StyleApplier setMaxWidth(String width) {
      stylize("max-Width", width);
      return this;
    }

    public StyleApplier setMaxHeight(String width) {
      stylize("max-Width", width);
      return this;
    }

    public StyleApplier setMinWidth(String width) {
      stylize("min-Width", width);
      return this;
    }

    public StyleApplier setMinHeight(String width) {
      stylize("min-Width", width);
      return this;
    }

    public StyleApplier setVerticalAlign(String vAlign) {
      stylize("vertical-Align", vAlign);
      return this;
    }

    public StyleApplier setHorizontalAlign(String hAlign) {
      stylize("text-Align", hAlign);
      return this;
    }

    public StyleApplier setOverflow(String overflow) {
      stylize("overflow", overflow);
      return this;
    }

    public StyleApplier setOverflowX(String overflowX) {
      stylize("overflow-X", overflowX);
      return this;
    }

    public StyleApplier setOverflowY(String overflowY) {
      stylize("overflow-Y", overflowY);
      return this;
    }

    public StyleApplier setMargin(String margin) {
      stylize("margin", margin);
      return this;
    }

    public StyleApplier setMarginLeft(String marginLeft) {
      stylize("margin-Left", marginLeft);
      return this;
    }

    public StyleApplier setMarginRight(String marginRight) {
      stylize("margin-Right", marginRight);
      return this;
    }

    public StyleApplier setMarginTop(String marginTop) {
      stylize("margin-Top", marginTop);
      return this;
    }

    public StyleApplier setMarginBottom(String marginBottom) {
      stylize("margin-Bottom", marginBottom);
      return this;
    }

    public StyleApplier setPadding(String padding) {
      stylize("padding", padding);
      return this;
    }

    public StyleApplier setPaddingLeft(String paddingLeft) {
      stylize("padding-Left", paddingLeft);
      return this;
    }

    public StyleApplier setPaddingRight(String paddingRight) {
      stylize("padding-Right", paddingRight);
      return this;
    }

    public StyleApplier setPaddingTop(String paddingTop) {
      stylize("padding-Top", paddingTop);
      return this;
    }

    public StyleApplier setPaddingBottom(String paddingBottom) {
      stylize("padding-Bottom", paddingBottom);
      return this;
    }

    public StyleApplier setBorderRadius(String borderRadius) {
      stylize("border-Radius", borderRadius);
      return this;
    }

    public StyleApplier setBorderTopLeftRadius(String borderTopLeftRadius) {
      stylize("border-Top-Left-Radius", borderTopLeftRadius);
      return this;
    }

    public StyleApplier setBorderTopRightRadius(String borderTopRightRadius) {
      stylize("border-Top-Right-Radius", borderTopRightRadius);
      return this;
    }

    public StyleApplier setBorderBottomLeftRadius(String borderBottomLeftRadius) {
      stylize("border-Bottom-Left-Radius", borderBottomLeftRadius);
      return this;
    }

    public StyleApplier setBorderBottomRightRadius(String borderBottomRightRadius) {
      stylize("border-Bottom-Right-Radius", borderBottomRightRadius);
      return this;
    }

    public StyleApplier setBorder(String border) {
      stylize("border", border);
      return this;
    }

    public StyleApplier setBorderLeft(String borderLeft) {
      stylize("border-Left", borderLeft);
      return this;
    }

    public StyleApplier setBorderRight(String borderRight) {
      stylize("border-Right", borderRight);
      return this;
    }

    public StyleApplier setBorderTop(String borderTop) {
      stylize("border-Top", borderTop);
      return this;
    }

    public StyleApplier setBorderBottom(String borderBottom) {
      stylize("border-Bottom", borderBottom);
      return this;
    }

    public StyleApplier setBorderStyle(String borderStyle) {
      stylize("border-Style", borderStyle);
      return this;
    }

    public StyleApplier setBorderLeftStyle(String borderLeftStyle) {
      stylize("border-Left-Style", borderLeftStyle);
      return this;
    }

    public StyleApplier setBorderRightStyle(String borderRightStyle) {
      stylize("border-Right-Style", borderRightStyle);
      return this;
    }

    public StyleApplier setBorderTopStyle(String borderTopStyle) {
      stylize("border-Top-Style", borderTopStyle);
      return this;
    }

    public StyleApplier setBorderBottomStyle(String borderBottomStyle) {
      stylize("border-Bottom-Style", borderBottomStyle);
      return this;
    }

    public StyleApplier setBorderWidth(String borderWidth) {
      stylize("border-Width", borderWidth);
      return this;
    }

    public StyleApplier setBorderLeftWidth(String borderLeftWidth) {
      stylize("border-Left-Width", borderLeftWidth);
      return this;
    }

    public StyleApplier setBorderRightWidth(String borderRightWidth) {
      stylize("border-Right-Width", borderRightWidth);
      return this;
    }

    public StyleApplier setBorderTopWidth(String borderTopWidth) {
      stylize("border-Top-Width", borderTopWidth);
      return this;
    }

    public StyleApplier setBorderBottomWidth(String borderBottomWidth) {
      stylize("border-Bottom-Width", borderBottomWidth);
      return this;
    }

    public StyleApplier setBorderColor(String borderColor) {
      stylize("border-Color", borderColor);
      return this;
    }

    public StyleApplier setBorderLeftColor(String borderLeftColor) {
      stylize("border-Left-Color", borderLeftColor);
      return this;
    }

    public StyleApplier setBorderRightColor(String borderRightColor) {
      stylize("border-Right-Color", borderRightColor);
      return this;
    }

    public StyleApplier setBorderTopColor(String borderTopColor) {
      stylize("border-Top-Color", borderTopColor);
      return this;
    }

    public StyleApplier setBorderBottomColor(String borderBottomColor) {
      stylize("border-Bottom-Color", borderBottomColor);
      return this;
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
      if (equalsIgnoreCase("id", name)) {
        id = value;
      }
      attributes.put(name, newAttributeBuilder(value));
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

  protected Provider<AttributeApplier> attributeApplier;
  protected Provider<StyleApplier> stylizer;
  protected final StringTo<AttributeBuilder> attributes;

  public ElementBuilder() {
      this(false);
  }

  public ElementBuilder(boolean searchableChildren) {
    super(searchableChildren);
    attributes = X_Collect.newStringMap(AttributeBuilder.class);
    attributeApplier = new LazyProvider<>(new Provider<AttributeApplier>() {
      @Override
      public AttributeApplier get() {
        return createAttributeApplier();
      }
    });
    stylizer = new LazyProvider<>(new Provider<StyleApplier>() {
      @Override
      public StyleApplier get() {
        return createStyleApplier();
      }
    });
  }

  @Override
  public void cleanup() {
    super.cleanup();
    attributeApplier = null;
    stylizer = null;
    attributes.clear();
  }

  protected abstract StyleApplier createStyleApplier();
  protected AttributeApplier createAttributeApplier() {
    return new ApplyPendingAttribute();
  }

  public ElementBuilder<E> setAttribute(String name, String value) {
    if (equalsIgnoreCase("style", name)) {
      getStyle().setValue(value);
    } else {
      if (equalsIgnoreCase("id", name)) {
        id = value;
      }
      attributeApplier.get().setAttribute(name, value);
    }
    return this;
  }

  private boolean equalsIgnoreCase(String id, String name) {
    return name == null ? id == null : id.equals(name.toLowerCase());
  }

  public ElementBuilder<E> setDataAttribute(String name, String value) {
    return setAttribute("data-"+ withPrefix(name), value);
  }

  public ElementBuilder<E> setClass(String value) {
    setAttribute("class", value);
    return this;
  }


  public ElementBuilder<E> addAttribute(String name, String value) {
    switch(name.toLowerCase()) {
      case "style":
        getStyle().addValue(value);
        break;
      case "id":
        id = value;
        attributeApplier.get().addAttribute(name, value);
        return this;
      case "class":
        AttributeBuilder was = attributes.get(name);
        if (was == null) {
          attributes.put(name, newClassnameBuilder());
        }
      default:
        attributeApplier.get().addAttribute(name, value);
    }
    return this;
  }

  public ElementBuilder<E> addDataAttribute(String name, String value) {
    return addAttribute("data-"+ withPrefix(name), value);
  }

  public String withPrefix(String dataKey) {
    return prefix()+dataKey;
  }

  protected String prefix() {
    return getDefaultPrefix(); // == System.getProperty("data.attr.prefix", "xapi-");
  }

  protected AttributeBuilder newClassnameBuilder() {
    return new ClassnameBuilder();
  }

  public ElementBuilder<E> removeAttribute(String name) {
    attributeApplier.get().removeAttribute(name);
    return this;
  }

  public ElementBuilder<E> setStyle(String name, String value) {
    getStyle().applyStyle(this, name, value);
    return this;
  }

  public String toSource() {
      StringBuilder b = new StringBuilder();
      toHtml(b);
      return b.toString();
  }

  public StyleApplier getStyle() {
    return stylizer.get();
  }

  public ElementBuilder<E> setStyle(String value) {
    setAttribute("style", value);
    return this;
  }

  public ElementBuilder<E> removeStyle(String name) {
    getStyle().applyStyle(this, name, null);
    return this;
  }

  public ElementBuilder<E> setId(String id) {
    this.id = id;
    setAttribute("id", id);
    return this;
  }

  public ElementBuilder<E> setSrc(String src) {
    setAttribute("src", src);
    return this;
  }

  public ElementBuilder<E> setHref(String href) {
    setAttribute("href", href);
    return this;
  }

  public ElementBuilder<E> setTitle(String title) {
    setAttribute("title", title);
    return this;
  }

  public ElementBuilder<E> setSlot(String slot) {
    setAttribute("slot", slot);
    return this;
  }

  public ElementBuilder<E> setValue(String value) {
    setAttribute("value", value);
    return this;
  }

  public ElementBuilder<E> setChecked(String checked) {
    setAttribute("checked", checked);
    return this;
  }

  public ElementBuilder<E> setName(String name) {
    setAttribute("name", name);
    return this;
  }

  public abstract ElementBuilder<E> createNode(String tagName);

  @Override
  public ElementBuilder<E> createChild(String tagName) {
    final ElementBuilder<E> child = createNode(tagName);
    addChild(child, X_String.isEmpty(tagName)); // If we are a document fragment, we need to make new children the target element for future inserts
    return child;
  }

  public ElementBuilder<E> withChild(String value, In1<? super ElementBuilder<E>> childCallback) {
    final ElementBuilder<E> child = createChild(value);
    childCallback.in(child);
    return this;
  }
  public <I1> ElementBuilder<E> withChild1(String value, In2<I1, ? super ElementBuilder<E>> childCallback, I1 in1) {
    return withChild(value, childCallback.provide1(in1));
  }
  public <I2> ElementBuilder<E> withChild2(String value, In2<? super ElementBuilder<E>, I2> childCallback, I2 in2) {
    return withChild(value, childCallback.provide2(in2));
  }

  public boolean isEmpty() {
    return this.attributes.isEmpty() && isChildrenEmpty();
  }
}
