package xapi.elemental.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.elemental.X_Gwt3;
import xapi.elemental.api.UiElementGwt;
import xapi.ui.api.UiBuilder;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@InstanceDefault(implFor = UiBuilder.class)
public class UiElementGwtBuilder<E extends UiElementGwt> extends UiBuilder <E> {

  @Override
  protected E initialize(E inst) {
    return super.initialize(inst);
  }

  @Override
  protected E instantiate() {
    return (E) UiElementGwt.fromWeb(
          X_Gwt3.toElement("<" + getType() + "></"+getType()+">")
        );
  }
}
