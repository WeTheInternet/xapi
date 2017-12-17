package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;

public interface XapiSlidesComponent <El> extends IsModelComponent< El, ModelXapiSlides>{

   default String getModelType () {
    return "XapiSlides";
  }

   default ModelXapiSlides createModel () {
    return create(ModelXapiSlides.class);
  }

}
