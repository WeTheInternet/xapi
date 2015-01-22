/**
 *
 */
package xapi.ui.html.impl;

import static xapi.collect.X_Collect.newStringMap;

import java.util.Arrays;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import elemental.client.Browser;
import elemental.dom.Node;
import elemental.html.StyleElement;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.api.StringTo;
import xapi.ui.api.StyleService;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@SuppressWarnings("rawtypes")
@SingletonDefault(implFor=StyleService.class)
public class StyleServiceDefault <T extends StyleService> implements StyleService<T>{

  private ScheduledCommand printPendingCss;
  private StringTo<String> pendingCss = newStringMap(String.class);
  private StringTo<StyleElement> liveCss = newStringMap(StyleElement.class);

  @Override
  @SuppressWarnings("unchecked")
  public T addCss(String css, int priority) {
    if (printPendingCss == null) {
      css(css);
    }
    String key = Integer.toString(priority);
    String was = pendingCss.get(key);
    if (was == null) {
      pendingCss.put(key, css);
    } else {
      pendingCss.put(key, was+"\n"+css);
    }
    return (T) this;
  }

  protected void css(String css) {
    printPendingCss = new ScheduledCommand() {
      @Override
      public void execute() {
        printCss();
      }
    };
    Scheduler.get().scheduleFinally(printPendingCss);
  }

  private void printCss() {
    printPendingCss = null;
    StringTo<String> pending = pendingCss;
    synchronized(pendingCss) {
      String[] keys = pending.keyArray();
      Arrays.sort(keys);
      for (String key : keys) {
        String css = pending.get(key);
        StyleElement style = getStyleElement(key);
        appendStyle(style, css);
      }
    }
    pendingCss.clear();
  }


  private native void appendStyle(StyleElement style, String css)
  /*-{
    if(style.styleSheet){// IE
      style.styleSheet.cssText = style.styleSheet.cssText + css;
    } else {
      style.appendChild($doc.createTextNode(css));
    }
  }-*/;

  private StyleElement getStyleElement(String priority) {
    StyleElement style = liveCss.get(priority);
    if (style == null) {
      Node head = Browser.getDocument().getElementsByTagName("head").item(0);
      assert head != null : "The host HTML page does not have a <head> element"
          + " which is required by CssService";
      style = Browser.getDocument().createStyleElement();
      style.setAttribute("language", "text/css");
      // need to insert a new element
      String[] keys = liveCss.keyArray();
      try {
        if (keys.length == 0) {
          head.appendChild(style);
        } else {
          Arrays.sort(keys);
          String largest = keys[keys.length-1];
          int distanceFromEnd = largest.compareTo(priority);
          if (distanceFromEnd < 0) {
            head.appendChild(style);
          } else {
            // Find the tag ahead of our priority so we can insert before it
            String current = keys[0];
            int distanceFromBeginning = priority.compareTo(current);
            if (distanceFromBeginning < 0) {
              head.insertBefore(style, liveCss.get(current));
            } else {
              // iterative search
              if (distanceFromBeginning > distanceFromEnd) {
                // search backwards
                current = largest;
                for (int i = keys.length; --i>0;) {
                  String next = keys[i];
                  if (next.compareTo(priority) < 0) {
                    StyleElement winner = liveCss.get(current);
                    head.insertBefore(style, winner);
                    return style;
                  }
                  current = next;
                }
                throw new RuntimeException("Failed to inject stylesheet @ priority "+priority+" into "+liveCss);
              } else {
                // search forwards
                for (int i = 1, m = keys.length; i<m;++i) {
                  current = keys[i];
                  if (current.compareTo(priority) > 0) {
                    StyleElement winner = liveCss.get(current);
                    head.insertBefore(style, winner);
                    return style;
                  }
                }
                throw new RuntimeException("Failed to inject stylesheet @ priority "+priority+" into "+liveCss);
              }
            }
          }
        }
      }finally {
        liveCss.put(priority, style);
      }
    }
    return style;
  }

}
