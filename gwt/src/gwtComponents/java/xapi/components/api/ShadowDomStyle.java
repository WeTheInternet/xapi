package xapi.components.api;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/8/16.
 */
//@Repeatable(value=ShadowDomStyles.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ShadowDomStyle {
  /**
   * Specify the client bundle interface.
   * <p>
   * If you wish to control which resource is used,
   * use {@link ShadowDomStyle#resourceInstanceExpression()}
   * to pass in a fully qualified java expression that the generator can use
   * to get access to an instance of the resource.
   * <p>
   * If that field is not set,
   * then the generator will look in your ClientBundle type,
   * and if it has a field that is its own type,
   * then that field will be used.
   * <p>
   * Otherwise, the generator will GWT.create its own copy of the resource.
   * <p>
   * In any event, this resource must contain all instances of {@link CssResource}
   * that are specified in the {@link ShadowDomStyle#styles()} method.
   */
  Class<? extends ClientBundle> bundle() default ClientBundle.class;

  /**
   * Use to specify a fully qualified java expression which returns an instance of
   * the ClientBundle class specified in {@link ShadowDomStyle#bundle()}.
   * <p>
   * This string must be suitable for injection into generated code.
   * <p>
   * If the default value of "" is used, then an instance of the resource
   * will be searched for in the fields of that class,
   * and as a final fallback, the generator will GWT.create a copy of its own.
   * <p>
   * This only needs to be set if you also want external access to the bundle instance.
   */
  String resourceInstanceExpression() default "";

  /**
   * Return an array of {@link CssResource} classes that are to be injected into the shadow dom.
   * The default option will include all css resources.
   * <p>
   * All of these resources must exist in the bundle interface defined in {@link ShadowDomStyle#bundle()}.
   * <p>
   * In the future, it may be possible to optimize implementations to only import parts of a css resource;
   * if the styles are only used in shadow DOM templates, and are not accessed in code at all
   * (i.e., if you do not have an external instance of the resource class), then it would be
   * safe to strip these resources down to exclude any unused css classes.
   */
  Class<? extends CssResource>[] styles() default CssResource.class; // matches all css resources

  /**
   * Optionally include some raw css.  This should only be used if you have a very tiny amount of css,
   * or if you run into any bugs in the gss compiler.
   */
  String[] css() default {};

  /**
   * Return an array of classes which themselves have {@link ShadowDomStyle} annotations to use.
   * <p>
   * Importing from another annotated type is an easy way to share reusable styles.
   */
  Class[] styleImports() default {};
}
