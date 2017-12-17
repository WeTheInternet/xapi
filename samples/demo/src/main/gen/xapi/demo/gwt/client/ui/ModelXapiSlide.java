package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;


import xapi.collect.api.IntTo;
import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;
import xapi.model.api.ModelKey;

public interface ModelXapiSlide extends Model{

   static KeyBuilder newKey () {
    return XAPI_SLIDE_KEY_BUILDER.out1();
  }

  String MODEL_XAPI_SLIDE = "xapiSlide";

  Out1<KeyBuilder> XAPI_SLIDE_KEY_BUILDER = forType(MODEL_XAPI_SLIDE);

  Out1<ModelBuilder<ModelXapiSlide>> XAPI_SLIDE_MODEL_BUILDER = 
      ()->
        build(XAPI_SLIDE_KEY_BUILDER.out1(),
        ()->create(ModelXapiSlide.class));


    IntTo<ModelKey> getItems () ;

    IntTo<ModelKey> setItems (IntTo<ModelKey> items) ;

    String getTitle () ;

    String setTitle (String title) ;

}
