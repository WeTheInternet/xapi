/**
 *
 */
package xapi.ui.html.api;

import xapi.annotation.compile.Import;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
//@Target({}) // consider not allowing anywhere except inside other annotations.
public @interface HtmlTemplate {

  String
  // Looping feature on hold for now
//      KEY_FOR = "$for",
//      KEY_LOOP_START = "{{",
//      KEY_LOOP_END = "}}",
      KEY_PARENT = "$parent",
      KEY_CHILDREN = "$children",
      KEY_CONTEXT = "$ctx",
      KEY_VALUE = "$value"
      ;

  /**
   * @return true if you want the annotations on the template to be applied.
   *         Leave false (default) to have the template treated as a named
   *         child, to be applied by some other {@link Html}, {@link El} or
   *         {@link HtmlTemplate}.
   */
  boolean inherit() default false;

  boolean wrapsChildren() default false;

  /**
   * @return any string to allow ${bean.naming} references to this template.
   */
  String name() default "";

  /**
   * @return An optional template to be parsed for replacement keys
   */
  String template() default "";

  /**
   * @return The default "global" keys you want to handle.
   * <p>
   * Unless you are implementing your own generator, the only reason to set this
   * variable will be to declare aliases to snippets of code that will be within scope in generated code.
   * <p>
   * To use an alias, use a templateKey in the form of "keyName:scopedObject.doSomething".
   * <p>
   * If you use {@link Import} tags, you can _try_ using short classnames,
   * however a fully qualified reference to type names is recommended.
   * <br/>
   * static imports with unique names is general the safest way to make concise references in template values.
   *
   */
  String[] templateKeys() default { KEY_VALUE, KEY_CHILDREN, KEY_PARENT, KEY_CONTEXT };

  Import[] imports() default {};

  /**
   * @return classes to use to extract {@link Html}, {@link El} and {@link Style} data from.
   */
  Class<?>[] references() default {};
}
