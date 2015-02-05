package xapi.ui.html.api;

import static xapi.collect.X_Collect.newStringMap;

import java.io.IOException;

import javax.inject.Provider;

import com.google.gwt.reflect.shared.GwtReflect;

import xapi.annotation.common.Property;
import xapi.collect.api.StringTo;
import xapi.dev.source.DomBuffer;
import xapi.log.X_Log;
import xapi.source.write.MappedTemplate;
import xapi.ui.api.StyleService;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.html.api.Style.AlignHorizontal;
import xapi.ui.html.api.Style.AlignVertical;
import xapi.ui.html.api.Style.BorderStyle;
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

  public static void appendTo(final Appendable sheet, final Style style) {
    try {
      doAppend(sheet, style);
    } catch (final IOException e) {
      X_Log.error(HtmlSnippet.class, "Error rendering",style,e);
    }
  }
  public static void doAppend(final Appendable sheet, final Style style) throws IOException {

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
      final Class<? extends FontFamily>[] fonts = style.fontFamily();
      final StringBuilder b = new StringBuilder();
      for (int i = 0, m = fonts.length; i < m; i ++ ) {
        if (i > 0) {
          b.append(", ");
        }
        try {
          if (fonts[i].isInterface()) {
            final Object value = GwtReflect.invoke(fonts[i], "name", new Class<?>[0], null);
            b.append(value);
          } else {
            b.append(fonts[i].newInstance().name());
          }
        } catch (final Throwable e) {
          X_Log.warn(HtmlSnippet.class, "Error loading font family for "+fonts[i], e);
        }
      }
      sheet.append("font-family").append(":").append(b).append(";");
    }

    final Transition[] transitions = style.transition();
    if (transitions.length > 0) {
      final StringBuilder b = new StringBuilder();
      for (int i = 0, m = transitions.length; i < m; i ++) {
        if (i > 0) {
          b.append(", ");
        }
        final Transition transition = transitions[i];
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

    for (final Property prop : style.properties()) {
      append(prop.name(), sheet, prop.value());
    }

    final Unit[] borderRadius = style.borderRadius();
    if (borderRadius.length > 0) {
      sheet.append("border-radius:");
      for (final Unit unit : borderRadius) {
        sheet.append(" ").append(toString(unit));
      }
      sheet.append(";");
    }

    append("border", "width", sheet, style.borderWidth(), style.borderWidthTop(),
        style.borderWidthRight(), style.borderWidthBottom(), style.borderWidthLeft());

    if (style.borderStyle().length > 0) {
      sheet.append("border-style: ");
      for (final BorderStyle borderStyle : style.borderStyle()) {
        sheet.append(borderStyle.styleName()).append(' ');
      }
      sheet.append(";");
    }

    if (style.borderColor().length > 0) {
      sheet.append("border-color: ");
      for (final Rgb borderColor : style.borderColor()) {
        sheet.append(toColor(borderColor)).append(' ');
      }
      sheet.append(";");
    }
  }

  private static void append(final String type, final Appendable sheet, final String value) throws IOException {
    sheet
    .append(type)
    .append(":")
    .append(value)
    .append(";");
  }

  private static void append(final String type, final Appendable sheet, final Unit unit) throws IOException {
    if (unit.type() != UnitType.Auto) {
      sheet
      .append(type)
      .append(":")
      .append(toString(unit))
      .append(";");
    }
  }

  private static void append(final String type, final Appendable sheet, final Unit[] all,
      final Unit top, final Unit right, final Unit bottom, final Unit left) throws IOException {
    append(type, "", sheet, all, top, right, bottom, left);
  }

  private static void append(final String type0, final String type1, final Appendable sheet, final Unit unit) throws IOException {
    if (unit.type() != UnitType.Auto) {
      sheet
      .append(type0)
      .append(type1)
      .append(":")
      .append(toString(unit))
      .append(";");
    }
  }

  private static void append(final String type, String typeSuffix, final Appendable sheet, final Unit[] all,
      final Unit top, final Unit right, final Unit bottom, final Unit left) throws IOException {
    if (all.length > 0) {
      sheet.append(type).append(":");
      for (final Unit unit : all) {
        sheet.append(toString(unit));
      }
      sheet.append(";");
    }
    if (typeSuffix.length() > 0 && typeSuffix.charAt(0) != '-') {
      typeSuffix="-"+typeSuffix;
    }
    append(type,"-top"+typeSuffix, sheet, top);
    append(type,"-right"+typeSuffix, sheet, right);
    append(type,"-bottom"+typeSuffix, sheet, bottom);
    append(type,"-left"+typeSuffix, sheet, left);
  }

  private static void appendColor(final String type, final Appendable sheet, final Rgb rgb) throws IOException {
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

  private static String toColor(final Rgb rgb) {
    if (rgb.opacity() == 0xff) {
      return "#"+toHexString(rgb.r())+toHexString(rgb.g())+toHexString(rgb.b());
    } else {
      return "rgba("+rgb.r()+","+rgb.g()+","+rgb.b()+","+rgb.opacity()+")";
    }
  }

  private static String toHexString(final int r) {
    final String s = Integer.toHexString(r);
    return s.length() == 1 ? "0"+s: s;
  }

  private static String toString(final Unit unit) {
    final String important = unit.important() ? " !important" : "";
    switch (unit.type()) {
    case Auto:
      return "auto" + important;
    case Pct:
      return unit.value() + "%"+ important;
    case Em:
      return unit.value() + "em" + important;
    case Px:
      return unit.value() + "px" + important;
    default:
      throw new UnsupportedOperationException("Type "+unit+" not supported");
    }
  }

  protected static final Runnable NO_OP = new Runnable() {
    @Override public void run() {}
  };

  private final Provider<ConvertsValue<T, DomBuffer>> generator;

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
          public DomBuffer convert(final T from) {
            final DomBuffer buffer = newBuffer(html, from);
            final Iterable<String> keys = values.getChildKeys();
            for (final El el : html.body()) {
              final DomBuffer child = newChild(buffer, el);
              for (final Property prop : el.properties()) {
                child.setAttribute(prop.name(), toValue(values, keys, prop.value(), from));
              }
              for (final Style style : el.style()) {
                toStyleSheet(style, context);
              }
              for (final String clsName : el.className()) {
                child.addClassName(toValue(values, keys, clsName, from));
              }

              for (final String html : el.html()) {
                final MappedTemplate m = new MappedTemplate(html, keys);
                final StringTo<Object> vals = newStringMap(Object.class);
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

  public HtmlSnippet(final Provider<ConvertsValue<T, DomBuffer>> generator) {
    this.generator = new LazyProvider<>(generator);
  }

  @Override
  public String convert(final T from) {
    return toBuffer(from).toString();
  }

  public DomBuffer toBuffer(final T from) {
    return generator.get().convert(from);
  }

  protected DomBuffer newBuffer(final Html html, final T from) {
    return new DomBuffer();
  }

  protected DomBuffer newChild(final DomBuffer buffer, final El el) {
    return buffer.makeTag(el.tag()).setNewLine(false);
  }

  protected String toValue(final BeanValueProvider values, final Iterable<String> keys, final String template, final T from) {
    final MappedTemplate m = new MappedTemplate(template, keys);
    final StringTo<Object> vals = newStringMap(Object.class);
    values.fillMap("", m, vals, from);
    return m.applyMap(vals.entries());
  }

  private void toStyleSheet(final Style style, final StyleService<?> context) {
    final StringBuilder sheet = new StringBuilder();

    final String[] names = style.names();
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

}
