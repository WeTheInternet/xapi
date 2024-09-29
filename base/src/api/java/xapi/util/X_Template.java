package xapi.util;

import xapi.annotation.compile.Reference;
import xapi.annotation.compile.MagicMethod;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/2/16.
 */
public class X_Template {

  @MagicMethod(
      generator = @Reference(
          typeName = "xapi.javac.dev.template.TemplateInjector"
      )
  )
  @SuppressWarnings("unchecked")
  public static <T> T processTemplate(String template) {
    return (T)template; // replaced with generated code
  }

  private X_Template(){}
}
