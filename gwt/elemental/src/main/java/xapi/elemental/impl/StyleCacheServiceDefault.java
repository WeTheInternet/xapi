package xapi.elemental.impl;

import elemental.client.Browser;
import elemental.html.StyleElement;
import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringDictionary;
import xapi.elemental.api.StyleCacheService;
import xapi.log.X_Log;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/6/16.
 */
@SingletonDefault(implFor = StyleCacheService.class)
public class StyleCacheServiceDefault implements StyleCacheService {

  private StringDictionary<StyleElement> styleElems = X_Collect.newDictionary(StyleElement.class);

  @Override
  public StyleElement injectStyle(
      Class<? extends ClientBundle> bundle, Class<? extends CssResource>... styles
  ) {
    String key = toKey(bundle, styles);
    StyleElement style = styleElems.getValue(key);
    if (style == null) {
      style = generateStyle(bundle, styles);
      styleElems.setValue(key, style);
    }
    return (StyleElement) style.cloneNode(true); // we return clones, so shadowDom can efficiently inject style tags
  }

  protected StyleElement generateStyle(Class<? extends ClientBundle> bundle, Class<? extends CssResource> ... styles) {
    // First, we must find an instance of the bundle.  Since it is too late to GWT.create, we must assume there is a constant field.
    ClientBundle resource;
    findResource:
    try {
      for (Field field : bundle.getFields()) {
        if (field.getType() == bundle) {
          resource = (ClientBundle) field.get(null);
          break findResource;
        }
      }
      X_Log.warn(getClass(), "Unable to find resource instance; runtime style injection will fail.");
      return null;
    } catch (Exception e) {
      X_Log.error(getClass(), "Attempting to perform runtime injection of CSS resources, but the class ", bundle,
          " was not enhanced to support reflection.  Try invoking GwtReflect.magicClass(", bundle.getName(), ".class)");
      return null;
    }
    ClassTo<CssResource> resources = X_Collect.newClassMap(CssResource.class);
    searchStyle:
    for (Class<? extends CssResource> style : styles) {
      for (Method method : bundle.getMethods()) {
        if (method.getReturnType() == style) {
          try {
            resources.put(style, (CssResource) method.invoke(resource));
            continue searchStyle;
          } catch (Exception e) {
            X_Log.error(getClass(), "Attempt to inject resource ", style, " from ", bundle, " using method ", method.getName(), "failed", e);
          }
        }
      }
      // exact type match failed, settle for an assignable match
      for (Method method : bundle.getMethods()) {
        if (style.isAssignableFrom(method.getReturnType())) {
          try {
            resources.put(style, (CssResource) method.invoke(resource));
            continue searchStyle;
          } catch (Exception e) {
            X_Log.error(getClass(), "Attempt to inject resource ", style, " from ", bundle, " using method ", method.getName(), "failed", e);
          }
        }
      }

      X_Log.warn(getClass(), "Unable to find an instance of ", style, " in the methods of ", bundle,". It will be skipped");

    }
    // Now that we have all the unique style instances, lets condense them into unique results (since we allowed assignable matches)

    ClassTo<CssResource> unique = X_Collect.newClassMap(CssResource.class);
    for (CssResource css : resources.values()) {
      unique.put(css.getClass(), css);
    }
    resources.clear();
    String s = "";
    for (CssResource cssResource : unique.values()) {
      s += cssResource.getText();
    }
    unique.clear();

    final StyleElement styleElem = createStyle(s);

    return styleElem;
  }

  @Override
  public void registerStyle(
      Class<? extends ClientBundle> bundle, String css, Class<? extends CssResource>... styles
  ) {
    String key = toKey(bundle, styles);
    final StyleElement style = createStyle(css);
    styleElems.setValue(key, style);
  }

  private StyleElement createStyle(String css) {
    final StyleElement style = Browser.getDocument().createStyleElement();
    style.setTextContent(css);
    return style;
  }

  private String toKey(Class<? extends ClientBundle> bundle, Class<? extends CssResource>[] styles) {
    String s = bundle.getName();
    String[] names = new String[styles.length];
    for (int i = styles.length; i-->0;) {
      names[i] = styles[i].getName();
    }
    Arrays.sort(names);
    for (String name : names) {
      s += ":" + name;
    }

    return s;
  }
}
