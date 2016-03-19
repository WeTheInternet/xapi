package xapi.javac.dev.model;

import xapi.annotation.api.XApi;

import javax.lang.model.element.Element;
import java.io.Serializable;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */
public class XApiInjectionConfiguration implements Serializable {

  private final XApi settings;
  private final Element element;
  private final String name;

  public XApiInjectionConfiguration(XApi settings, String name, Element element) {
    this.settings = settings;
    this.element = element;
    this.name = name;
  }

  public XApi getSettings() {
    return settings;
  }

  public Element getElement() {
    return element;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof XApiInjectionConfiguration && ((XApiInjectionConfiguration)obj).name.equals(name);
  }

  @Override
  public String toString() {
    return "XApi injection for " + name+" : "+ settings;
  }

  public String getName() {
    return name;
  }
}
