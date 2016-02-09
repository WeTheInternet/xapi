package xapi.elemental.api;

import elemental.html.StyleElement;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/6/16.
 */
public interface StyleCacheService {

  StyleElement injectStyle(
      Class<? extends ClientBundle> bundle, Class<? extends CssResource> ... styles
  );

  void registerStyle(
      Class<? extends ClientBundle> bundle, String css, Class<? extends CssResource>... styles
  );
}
