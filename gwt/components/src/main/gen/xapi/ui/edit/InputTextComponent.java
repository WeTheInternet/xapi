package xapi.ui.edit;


import xapi.ui.api.component.IsModelComponent;
import xapi.util.X_Util;

public interface InputTextComponent <Node, El extends Node> extends IsModelComponent<
    Node,
    El,
    ModelInputText
  >{

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
