package xapi.gwt.ui.autoui.api;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public @interface Css {

  /**
   * @return any {@link Style} properties to set.
   */
  Style[] style() default {};

  /**
   * @return any {@link ClientBundle} implementors which must be instantiated and made available.
   */
  Class<? extends ClientBundle>[] resources() default {};
  
  /**
   * @return Any text to turn into css; 
   * for ClientBundle references, like the following:
   * <pre>
   * interface MyCss extends {@link CssResource} {
   * String myStyle();
   * }
   * interface MyClass extends {@link ClientBundle} {
   * MyCss myCss();
   * }
   * </pre>
   * {myCss.myStyle} matches the stylename provided.
   * 
   * 
   */
  String value() default "";
  
  /**
   * @return the insertion priority of the css (negative values inserted higher in the document)
   */
  int priority() default 0;
}
