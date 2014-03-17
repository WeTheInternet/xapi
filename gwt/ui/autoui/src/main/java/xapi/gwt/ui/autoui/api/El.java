package xapi.gwt.ui.autoui.api;

import xapi.annotation.common.Property;

/**
 * Shorthand for an Element.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public @interface El {

  Style[] style() default {};
  
  Property[] properties() default {@Property(name="class",value="$name")};
  
  String tag() default "div";
  
  /**
   *  Default element content:
   * <pre>
   *   &lt;div>$value&lt;/div>
   * </pre>
   * where $value is the result of String.valueOf(data), or " " if there is no $value in scope.
   */
  String[] html() default "<$this>$value</$this>";
}
