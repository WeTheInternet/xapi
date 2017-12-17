package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;

public interface XapiTextComponent <El> extends IsModelComponent< El, ModelXapiText>{

   default String getModelType () {
    return "XapiText";
  }

   default ModelXapiText createModel () {
    return create(ModelXapiText.class);
  }

}
