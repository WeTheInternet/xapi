package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;

public interface XapiSlideComponent <El> extends IsModelComponent< El, ModelXapiSlide>{

   default String getModelType () {
    return "XapiSlide";
  }

   default ModelXapiSlide createModel () {
    return create(ModelXapiSlide.class);
  }

}
