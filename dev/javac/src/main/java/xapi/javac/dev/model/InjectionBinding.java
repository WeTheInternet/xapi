package xapi.javac.dev.model;

import java.io.Serializable;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */
public class InjectionBinding implements Serializable {

  private final String injectionResult;
  private final String injectionType;

  public InjectionBinding(String injectionType, String injectionResult) {
    this.injectionResult = injectionResult;
    this.injectionType = injectionType;
  }

  public String getInjectionType() {
    return injectionType;
  }

  public String getInjectionResult() {
    return injectionResult;
  }
}
