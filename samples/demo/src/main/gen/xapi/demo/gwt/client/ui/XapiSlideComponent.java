package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;

public interface XapiSlideComponent <Node, El extends Node> extends IsModelComponent<
    Node,
    El,
    ModelXapiSlide
  >{

  public default String getModelType () {
    return "XapiSlide";
  }

  public default ModelXapiSlide createModel () {
    return create(ModelXapiSlide.class);
  }

}
