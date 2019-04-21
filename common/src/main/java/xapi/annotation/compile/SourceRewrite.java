package xapi.annotation.compile;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */
public @interface SourceRewrite {

  Resource template() default @Resource(value = "", required = false);

  Reference reference() default @Reference;

  Reference generator() default @Reference/*(type=RewriteFromTemplate)*/;

  String newSource() default ""; // hacky, but it will work.

  boolean doNotRewrite() default false;

  /**
   * Rebase the target of this rewrite into a new package.
   *
   * Useful if you want compiled output to coexist with generated output,
   * or if you want to customize classes in protected packages, like java.*
   */
  String rebase() default "";
}
