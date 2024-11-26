package xapi.components.api;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/8/16.
 */
public @interface ShadowDom {
  /**
   * If the template begins with a &lt; < then it is treated as raw DOM markup.
   * Otherwise, it is treated as a uri to a resource file containing the template.
   * <p>
   * If the uri does not begin with / it is treated as relative to the class annotated with @WebComponent
   */
  String[] value();

  /**
   * Getting styles into shadow DOM is a bit contentious;
   * although shadow DOM piercing selectors are functional, they are already deprecated.
   * <p>
   * If you want to manually specify style tags in a template for the shadow DOM, you can,
   * however, you will lose all the nice Gss processing, variables, macros, etc.
   * <p>
   * Using ShadowDomStyle, you may specify a set of client bundle and css resource interfaces,
   * and the generated web component will, upon initialization, inject style tags with the
   * requested css resources found in the client bundle's.
   * We will be injecting clones of the css so that it is only parsed once.
   * <p>
   * In the future, a mechanism can be added to reduce the injected css to only the types used in the shadow dom,
   * however, this can get sticky, if the css classes are applied in code, then the generator will need to know.
   * In this case, an annotation like @DoNotErase might be handy.
   */
  ShadowDomStyle[] styles() default {};

  Class<? extends ShadowDomPlugin>[] plugins() default {};
}
