package xapi.test.components.xapi.test.components;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;
import xapi.util.X_Util;

public interface AsserterComponent <Node, El extends Node> extends IsModelComponent<
    Node,
    El,
    ModelAsserter
  >{

  public default String getModelType () {
    return "Asserter";
  }

  public default ModelAsserter createModel () {
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
