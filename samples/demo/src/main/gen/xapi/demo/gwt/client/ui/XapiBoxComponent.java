package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;

public interface XapiBoxComponent <Node, El extends Node> extends IsModelComponent<
    Node,
    El,
    ModelXapiBox
  >{

  public default String getModelType () {
    return "XapiBox";
  }

  public default ModelXapiBox createModel () {
    return create(ModelXapiBox.class);
  }

}
