package xapi.ui.api;

import xapi.ui.api.style.HasStyleResources;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/6/16.
 */
public interface StyleCacheService <Style, Bundle extends HasStyleResources> {

  Style injectStyle(
      Class<? extends Bundle> bundle, Class<?> ... styles
  );

  void registerStyle(
      Class<? extends Bundle> bundle, String css, Class<?>... styles
  );
}
