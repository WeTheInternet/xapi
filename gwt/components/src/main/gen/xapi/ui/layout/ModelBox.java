package xapi.ui.layout;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;


import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;

public interface ModelBox extends Model{

   static KeyBuilder newKey () {
    return BOX_KEY_BUILDER.out1();
  }

  String MODEL_BOX = "box";

  Out1<KeyBuilder> BOX_KEY_BUILDER = forType(MODEL_BOX);

  Out1<ModelBuilder<ModelBox>> BOX_MODEL_BUILDER = 
      ()->
        build(BOX_KEY_BUILDER.out1(),
        ()->create(ModelBox.class));


   abstract Model getChild () ;

   abstract Model setChild (Model child) ;

}
