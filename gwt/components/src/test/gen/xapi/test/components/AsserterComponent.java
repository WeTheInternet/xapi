package xapi.test.components;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;
import xapi.util.X_Util;

public interface AsserterComponent <El> extends IsModelComponent< El, ModelAsserter>{

  String TAG_NAME = "xapi-asserter";

   default String getModelType () {
    return "Asserter";
  }

   default ModelAsserter createModel () {
    return create(ModelAsserter.class);
  }

  default String getTemplate() {
    return getModel().getTemplate();
  }

  default AsserterComponent setTemplate(String value) {
    String was = getModel().getTemplate();
    if (X_Util.notEqual(was, value)) {
      was = normalizeChange(was, value);
    }
    getModel().setTemplate(value);
    return this;
  }

  default String normalizeChange(String is, String want) {
    return want;
  }

}
