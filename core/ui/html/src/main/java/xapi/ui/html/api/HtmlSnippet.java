package xapi.ui.html.api;

import static xapi.collect.X_Collect.newStringMap;

import java.io.IOException;

import javax.inject.Provider;

import xapi.annotation.common.Property;
import xapi.collect.api.StringTo;
import xapi.dev.source.DomBuffer;
import xapi.log.X_Log;
import xapi.source.write.MappedTemplate;
import xapi.ui.api.StyleService;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.html.api.Style.AlignHorizontal;
import xapi.ui.html.api.Style.AlignVertical;
import xapi.ui.html.api.Style.BoxSizing;
import xapi.ui.html.api.Style.Clear;
import xapi.ui.html.api.Style.Cursor;
import xapi.ui.html.api.Style.Display;
import xapi.ui.html.api.Style.Floats;
import xapi.ui.html.api.Style.FontStyle;
import xapi.ui.html.api.Style.FontWeight;
import xapi.ui.html.api.Style.Overflow;
import xapi.ui.html.api.Style.Position;
import xapi.ui.html.api.Style.Rgb;
import xapi.ui.html.api.Style.Transition;
import xapi.ui.html.api.Style.Unit;
import xapi.ui.html.api.Style.UnitType;
import xapi.util.api.ConvertsValue;
import xapi.util.impl.LazyProvider;

public class HtmlSnippet <T> implements ConvertsValue<T, String> {

  protected static final Runnable NO_OP = new Runnable() {
    @Override public void run() {}
  };
  private final Provider<ConvertsValue<T, DomBuffer>> generator;

  public HtmlSnippet(Provider<ConvertsValue<T, DomBuffer>> generator) {
    this.generator = new LazyProvider<>(generator);
  }

  public HtmlSnippet(
      final Html html,
      final BeanValueProvider values,
      final StyleService<?> context
    ) {
    assert html != null : "Do not send null @Html to HtmlSnippet!";
    assert values != null : "Do not send null BeanValueProvider to HtmlSnippet!";
    generator = new LazyProvider<>(new Provider<ConvertsValue<T, DomBuffer>>() {
      @Override
      public ConvertsValue<T, DomBuffer> get() {
        return new ConvertsValue<T, DomBuffer>() {
          @Override
          public DomBuffer convert(T from) {
            DomBuffer buffer = newBuffer(html, from);
            Iterable<String> keys = values.getChildKeys();
            for (El el : html.body()) {
              DomBuffer child = newChild(buffer, el);
              for (Property prop : el.properties()) {
                child.setAttribute(prop.name(), toValue(values, keys, prop.value(), from));
              }
              for (Style style : el.style()) {
                toStyleSheet(style, context);
              }
              for (String clsName : el.className()) {
                child.addClassName(toValue(values, keys, clsName, from));
              }

              for (String html : el.html()) {
                MappedTemplate m = new MappedTemplate(html, keys);
                StringTo<Object> vals = newStringMap(Object.class);
                values.fillMap("", m, vals, from);
                child.append(m.applyMap(vals.entries()));
              }
            }

            return buffer;
          }



        };
      }
    });
  }

  protected String toValue(BeanValueProvider values, Iterable<String> keys, String template, T from) {
    MappedTemplate m = new MappedTemplate(template, keys);
    StringTo<Object> vals = newStringMap(Object.class);
    values.fillMap("", m, vals, from);
    return m.applyMap(vals.entries());
  }

  private void toStyleSheet(Style style, StyleService<?> context) {
    StringBuilder sheet = new StringBuilder();

    String[] names = style.names();
    for (int i = 0, m = names.length; i < m; ++i) {
      if (i > 0) {
        sheet.append(", ");
      }
      sheet.append(names[i]);
    }
    if (names.length > 0) {
      sheet.append("{\n");
    }

    appendTo(sheet, style);

    if (names.length > 0) {
      sheet.append("}\n");
    }
    context.addCss(sheet.toString(), style.priority());
  }

  public static void appendTo(Appendable sheet, Style style) {
    try {
      doAppend(sheet, style);
    } catch (IOException e) {
      X_Log.error(HtmlSnippet.class, "Error rendering",style,e);
    }
  }

  public static void doAppend(Appendable sheet, Style style) throws IOException {

    append("left", sheet, style.left());
    append("right", sheet, style.right());
    append("top", sheet, style.top());
    append("bottom", sheet, style.bottom());
    append("width", sheet, style.width());
    append("height", sheet, style.height());
    append("max-height", sheet, style.maxHeight());
    append("max-width", sheet, style.maxWidth());
    append("min-height", sheet, style.minHeight());
    append("min-width", sheet, style.minWidth());

    if (style.boxSizing() != BoxSizing.Inherit) {
      append("box-sizing", sheet, style.boxSizing().styleName);
    }

    if (style.display() != Display.Inherit) {
      append("display", sheet, style.display().styleName());
    }

    if (style.position() != Position.Inherit) {
      append("position", sheet, style.position().styleName());
    }

    if (style.fontStyle() != FontStyle.Inherit) {
      append("font-style", sheet, style.fontStyle().styleName());
    }

    if (style.fontWeight() != FontWeight.Inherit) {
      append("font-weight", sheet, style.fontWeight().styleName());
    }

    if (style.textAlign() != AlignHorizontal.Auto) {
      append("text-align", sheet, style.textAlign().styleName());
    }

    if (style.verticalAign() != AlignVertical.Auto) {
      append("vertical-align", sheet, style.verticalAign().styleName());
    }

    if (style.cursor() != Cursor.Inherit) {
      append("cursor", sheet, style.cursor().styleName());
    }

    if (style.floats() != Floats.Auto) {
      append("float", sheet, style.floats().styleName());
    }

    if (style.clear() != Clear.Auto) {
      append("clear", sheet, style.clear().styleName());
    }

    if (style.opacity() != 1) {
      append("opacity", sheet,Double.toString(style.opacity()));
    }

    if (style.fontFamily().length > 0) {
      Class<? extends FontFamily>[] fonts = style.fontFamily();
      StringBuilder b = new StringBuilder();
      for (int i = 0, m = fonts.length; i < m; i ++ ) {
        if (i > 0) {
          b.append(", ");
        }
        try {
          b.append(fonts[i].newInstance().name());
        } catch (Exception e) {
          X_Log.warn(HtmlSnippet.class, "Error loading font family for "+fonts[i], e);
        }
      }
      sheet.append("font-family").append(":").append(b).append(";");
    }

    Transition[] transitions = style.transition();
    if (transitions.length > 0) {
      StringBuilder b = new StringBuilder();
      for (int i = 0, m = transitions.length; i < m; i ++) {
        if (i > 0) {
          b.append(", ");
        }
        Transition transition = transitions[i];
        b
          .append(transition.value())
          .append(" ")
          .append(transition.time())
          .append(transition.unit())
        ;
      }
      append("transition", sheet, b.toString());
    }

    if (style.color().length > 0) {
      appendColor("color", sheet, style.color()[0]);
    }

    if (style.backgroundColor().length > 0) {
      appendColor("background-color", sheet, style.backgroundColor()[0]);
    }

    append("font-size", sheet, style.fontSize());
    append("line-height", sheet, style.lineHeight());

    append("padding", sheet, style.padding(),
        style.paddingTop(), style.paddingRight(), style.paddingBottom(), style.paddingLeft());

    append("margin", sheet, style.margin(),
        style.marginTop(), style.marginRight(), style.marginBottom(), style.marginLeft());

    if (style.overflow() != Overflow.Inherit) {
      append("overflow", sheet, style.overflow().styleName());
    }

    if (style.overflowX() != Overflow.Inherit) {
      append("overflow-x", sheet, style.overflowX().styleName());
    }

    if (style.overflowY() != Overflow.Inherit) {
      append("overflow-y", sheet, style.overflowY().styleName());
    }

  }

  private static void append(String type, Appendable sheet, String value) throws IOException {
    sheet
        .append(type)
        .append(":")
        .append(value)
        .append(";");
  }

  private static void appendColor(String type, Appendable sheet, Rgb rgb) throws IOException {
    sheet.append(type).append(":");
    if (rgb.opacity() == 0xff) {
      sheet
        .append("#")
        .append(toHexString(rgb.r()))
        .append(toHexString(rgb.g()))
        .append(toHexString(rgb.b()))
      ;
    } else {
      sheet
        .append("rgba(")
        .append(Integer.toString(rgb.r()))
        .append(",")
        .append(Integer.toString(rgb.g()))
        .append(",")
        .append(Integer.toString(rgb.b()))
        .append(",")
        .append(Integer.toString(rgb.opacity()))
        .append(")")
      ;
    }
    sheet.append(";");
  }

  private static String toHexString(int r) {
    String s = Integer.toHexString(r);
    return s.length() == 1 ? "0"+s: s;
  }

  private static void append(String type, Appendable sheet, Unit[] all,
      Unit top, Unit right, Unit bottom, Unit left) throws IOException {
    if (all.length > 0) {
      sheet.append(type).append(":");
      for (Unit unit : all) {
        sheet.append(toString(unit));
      }
      sheet.append(";");
    }
    append(type,"-top", sheet, top);
    append(type,"-right", sheet, right);
    append(type,"-bottom", sheet, bottom);
    append(type,"-left", sheet, left);
  }

  private static void append(String type, Appendable sheet, Unit unit) throws IOException {
    if (unit.type() != UnitType.Auto) {
      sheet
        .append(type)
        .append(":")
        .append(toString(unit))
        .append(";");
    }
  }

  private static void append(String type0, String type1, Appendable sheet, Unit unit) throws IOException {
    if (unit.type() != UnitType.Auto) {
      sheet
      .append(type0)
      .append(type1)
      .append(":")
      .append(toString(unit))
      .append(";");
    }
  }

  private static String toString(Unit unit) {
    switch (unit.type()) {
    case Auto:
      return "auto";
    case Pct:
      return unit.value() + "%";
    case Em:
      return unit.value() + "em";
    case Px:
      return unit.value() + "px";
    default:
      throw new UnsupportedOperationException("Type "+unit+" not supported");
    }
  }

  protected DomBuffer newChild(DomBuffer buffer, El el) {
    return buffer.makeTag(el.tag()).setNewLine(false);
  }

  protected DomBuffer newBuffer(Html html, T from) {
    return new DomBuffer();
  }

  @Override
  public String convert(T from) {
    return toBuffer(from).toString();
  }

  public DomBuffer toBuffer(T from) {
    return generator.get().convert(from);
  }

}
