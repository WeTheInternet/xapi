package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;

public interface XapiSlidesComponent <Node, El extends Node> extends IsModelComponent<
    Node,
    El,
    ModelXapiSlides
  >{

  public default String getModelType () {
    return "XapiSlides";
  }

  public default ModelXapiSlides createModel () {
    return create(ModelXapiSlides.class);
  }

}
