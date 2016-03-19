package xapi.javac.dev.model;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.impl.JavacServiceImpl;

import java.io.Serializable;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */
public class InjectionBinding implements Serializable {

  private String injectionResult;
  private String injectionType;

  public InjectionBinding(String injectionType, String injectionResult) {
    this.injectionResult = injectionResult;
    this.injectionType = injectionType;
  }

  public InjectionBinding(JavacService service, VariableTree node) {

  }

  public InjectionBinding(JavacServiceImpl javacService, MethodTree node) {

  }

  public String getInjectionType() {
    return injectionType;
  }

  public String getInjectionResult() {
    return injectionResult;
  }
}
