package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;


import xapi.collect.api.IntTo;
import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;

public interface ModelXapiBox extends Model{

   static KeyBuilder newKey () {
    return XAPI_BOX_KEY_BUILDER.out1();
  }

  String MODEL_XAPI_BOX = "xapiBox";

  Out1<KeyBuilder> XAPI_BOX_KEY_BUILDER = forType(MODEL_XAPI_BOX);

  Out1<ModelBuilder<ModelXapiBox>> XAPI_BOX_MODEL_BUILDER = 
      ()->
        build(XAPI_BOX_KEY_BUILDER.out1(),
        ()->create(ModelXapiBox.class));


    String getId () ;

    String setId (String id) ;

    IntTo<ModelXapiText> getText () ;

    IntTo<ModelXapiText> setText (IntTo<ModelXapiText> text) ;

    String getTitle () ;

    String setTitle (String title) ;

    BoxPosition getPosition () ;

    BoxPosition setPosition (BoxPosition position) ;

    BoxSize getSize () ;

    BoxSize setSize (BoxSize size) ;

}
