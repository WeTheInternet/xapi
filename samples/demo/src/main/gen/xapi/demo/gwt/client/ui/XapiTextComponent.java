package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;

public interface XapiTextComponent <Node, El extends Node> extends IsModelComponent<
    Node,
    El,
    ModelXapiText
  >{

  public default String getModelType () {
    return "XapiText";
  }

  public default ModelXapiText createModel () {
    return create(ModelXapiText.class);
  }

}
