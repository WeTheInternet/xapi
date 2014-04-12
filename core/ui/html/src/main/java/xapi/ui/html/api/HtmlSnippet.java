package xapi.ui.html.api;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import xapi.annotation.common.Property;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.HtmlBuffer;
import xapi.log.X_Log;
import xapi.source.write.MappedTemplate;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.html.api.Style.Unit;
import xapi.ui.html.api.Style.UnitType;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ProvidesValue;
import xapi.util.impl.LazyProvider;

public class HtmlSnippet <T> implements ConvertsValue<T, String> {

  private final Provider<ConvertsValue<T, DomBuffer>> generator;

  public HtmlSnippet(Provider<ConvertsValue<T, DomBuffer>> generator) {
    this.generator = new LazyProvider<>(generator);
  }
      
  public HtmlSnippet(
      final Html html,
      final BeanValueProvider values,
      final HtmlBuffer context
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
            for (El el : html.elements()) {
              DomBuffer child = newChild(buffer, el);
              for (Property prop : el.properties()) {
                child.setAttribute(prop.name(), toValue(values, keys, prop.value(), from));
              }
              for (Style style : el.style()) {
                toStyleSheet(style, context);
              }
              
              for (String html : el.html()) {
                MappedTemplate m = new MappedTemplate(html, keys);
                Map<String, Object> vals = new LinkedHashMap<>();
                values.fillMap("", m, vals, from);
                child.append(m.applyMap(vals));
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
    Map<String, Object> vals = new LinkedHashMap<>();
    values.fillMap("", m, vals, from);
    return m.applyMap(vals);
  }
  
  private void toStyleSheet(Style style, HtmlBuffer context) {
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
    context.getHead().addStylesheet(sheet.toString());
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
    
  }

  private static void append(String string, Appendable sheet, Unit unit) throws IOException {
    if (unit.type() != UnitType.Auto) {
      sheet
        .append(string)
        .append(':')
        .append(toString(unit))
        .append(";");
    }
  }

  private static String toString(Unit unit) {
    switch (unit.type()) {
    case Auto:
      return "auto";
    case Pct:
      return (100*unit.value()) + "%";
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
