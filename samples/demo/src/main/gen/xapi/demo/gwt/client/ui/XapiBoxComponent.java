package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;

public interface XapiBoxComponent <El> extends IsModelComponent< El, ModelXapiBox>{

   default String getModelType () {
    return "XapiBox";
  }

   default ModelXapiBox createModel () {
    return create(ModelXapiBox.class);
  }

}
