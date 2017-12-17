package xapi.ui.layout;

import static xapi.model.X_Model.create;


import xapi.ui.api.component.IsModelComponent;

public interface BoxComponent <El> extends IsModelComponent< El, ModelBox>{

   default String getModelType () {
    return "Box";
  }

   default ModelBox createModel () {
    return create(ModelBox.class);
  }

  public El getFirstChild();

}
