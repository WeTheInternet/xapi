package xapi.elemental.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.elemental.X_Elemental;
import xapi.elemental.api.UiElementWeb;
import xapi.ui.api.UiBuilder;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
@InstanceDefault(implFor = UiBuilder.class)
public class UiElementWebBuilder <E extends UiElementWeb> extends UiBuilder <E> {

  @Override
  protected E initialize(E inst) {
    return super.initialize(inst);
  }

  @Override
  protected E instantiate() {
    return X_Elemental.toElement("<" + getType() + "></"+getType()+">");
  }
}
