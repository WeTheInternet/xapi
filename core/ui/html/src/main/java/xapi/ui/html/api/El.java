package xapi.ui.html.api;

import xapi.annotation.common.Property;
import xapi.annotation.compile.Import;
import xapi.ui.autoui.api.Action;

/**
 * Shorthand for an Element.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public @interface El {

  String DEFAULT_ACCESSOR = "from.$name()";
  String DIV = "div";

  Style[] style() default {};

  String[] className() default {};

  Property[] properties() default {};

  String tag() default DIV;

  // Some convenience methods for overriding #properties();
  String id() default "";
  String src() default "";
  String type() default "";
  String href() default "";

  String accessor() default DEFAULT_ACCESSOR;

  Action[] onClick() default {};
  Action[] onMouseOver() default {};
  Action[] onMouseOut() default {};
  Action[] onFocus() default {};
  Action[] onBlur() default {};
  Action[] onKeyDown() default {};
  Action[] onKeyUp() default {};
  Action[] onKeyPress() default {};

  Import[] imports() default {};

  Class<?>[] useToHtml() default {};

  HtmlTemplate[] inherit() default {};

  /**
   *  Default element content:
   * <pre>
   *   &lt;div>&lt;/div>
   * </pre>
   */
  String[] html() default "";
}
