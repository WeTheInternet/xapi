package xapi.server.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServlet;

import xapi.server.ObjectServlet;

/**
 * An annotation for servlets to have their url mappings and general behavior
 * automapped, in our {@link xapi.test.server.TestServer}, and to generate web.xml during
 * mvn process-resources
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface XapiServlet {

  /**
   * Required attribute; this is the string prefix after /xapi in the servlet url.
   *
   * @return - http://host/xapi/${prefix}
   */
  String prefix();

  /**
   * Optional attribute; an HttpServlet class to use as an ancestor to this servlet.
   *
   * This field is available to make any arbitrary class available at build-time.
   *
   * @return - Any class that extends HttpServlet; to send hints to your
   * generator / injector.  Using {@link ObjectServlet} will give you a
   * simple bean-model endpoint for an object.
   * <pre>
   *  if prefix = "Bean",
   *  && servletType == {@link ObjectServlet},
   *  /xapi/Bean/new/id -> new bean (never null)
   *  /xapi/Bean/get/id -> get bean (null if doesn't exist)
   *  /xapi/Bean/has/id -> check bean != null
   *  /xapi/Bean/del/id -> delete bean
   *  /xapi/Bean/set/id/data -> set bean
   *  /xapi/Bean/up/id/data -> update bean
   *
   * </pre>
   */
  Class<? extends HttpServlet> servletType() default ObjectServlet.class;

}
