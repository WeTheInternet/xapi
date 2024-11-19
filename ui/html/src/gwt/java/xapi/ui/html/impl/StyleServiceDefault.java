/**
 *
 */
package xapi.ui.html.impl;

import elemental.client.Browser;
import elemental.dom.Node;
import elemental.html.StyleElement;
import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.platform.GwtPlatform;
import xapi.ui.api.StyleService;
import xapi.ui.style.StyleServiceAbstract;

import java.util.Arrays;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@SuppressWarnings("rawtypes")
@SingletonDefault(
  implFor = StyleService.class)
@GwtPlatform
public class StyleServiceDefault extends StyleServiceAbstract<StyleElement> {

  @Override
  protected StringTo<StyleElement> liveCssMap() {
    return X_Collect.newStringMap(StyleElement.class);
  }

  @Override
  protected StyleElement prioritizedStyle(String priority) {
    final Node head = Browser.getDocument().getElementsByTagName("head")
        .item(0);
    assert head != null : "The host HTML page does not have a <head> element"
        + " which is required by CssService";
    final StyleElement style = Browser.getDocument().createStyleElement();
    style.setAttribute("language", "text/css");
    // need to insert a new element
    final String[] keys = liveCss.keyArray();
    if (keys.length == 0) {
      head.appendChild(style);
    } else {
      Arrays.sort(keys);
      final String largest = keys[keys.length - 1];
      final int distanceFromEnd = largest.compareTo(priority);
      if (distanceFromEnd < 0) {
        head.appendChild(style);
      } else {
        // Find the tag ahead of our priority so we can insert before it
        String current = keys[0];
        final int distanceFromBeginning = priority.compareTo(current);
        if (distanceFromBeginning < 0) {
          head.insertBefore(style, liveCss.get(current));
        } else {
          // iterative search
          if (distanceFromBeginning > distanceFromEnd) {
            // search backwards
            current = largest;
            for (int i = keys.length; --i > 0; ) {
              final String next = keys[i];
              if (next.compareTo(priority) < 0) {
                final StyleElement winner = liveCss.get(current);
                head.insertBefore(style, winner);
                return style;
              }
              current = next;
            }
            throw new RuntimeException(
                "Failed to inject stylesheet @ priority " + priority
                    + " into " + liveCss);
          } else {
            // search forwards
            for (int i = 1, m = keys.length; i < m; ++i) {
              current = keys[i];
              if (current.compareTo(priority) > 0) {
                final StyleElement winner = liveCss.get(current);
                head.insertBefore(style, winner);
                return style;
              }
            }
            throw new RuntimeException(
                "Failed to inject stylesheet @ priority " + priority
                    + " into " + liveCss);
          }
        }
      }
    }
    return style;
  }

}
