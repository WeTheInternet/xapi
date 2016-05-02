package xapi.ui.api;

import xapi.ui.service.UiService;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public interface UiFeature {

  default boolean isSingleton() {
    return false;
  }

  default void initialize(UiService service) {}

}
