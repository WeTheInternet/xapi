package xapi.ui.edit;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;
import xapi.util.X_Util;

public interface InputTextComponent <El> extends IsModelComponent< El, ModelInputText>{

   default String getModelType () {
    return "InputText";
  }

   default ModelInputText createModel () {
    return create(ModelInputText.class);
  }

  default String getValue() {
    return getModel().getValue();
  }

  default InputTextComponent setValue(String value) {
    String was = getModel().getValue();
    if (X_Util.notEqual(was, value)) {
      was = normalizeChange(was, value);
      getModel().setValue(value);
    }
    return this;
  }

  default String normalizeChange(String is, String want) {
    return want;
  }

}
