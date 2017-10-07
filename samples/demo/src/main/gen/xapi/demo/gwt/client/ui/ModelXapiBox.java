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

  public String MODEL_XAPI_BOX = "xapiBox";

  public Out1<KeyBuilder> XAPI_BOX_KEY_BUILDER = forType(MODEL_XAPI_BOX);

  public Out1<ModelBuilder<ModelXapiBox>> XAPI_BOX_MODEL_BUILDER = 
      ()->
        build(XAPI_BOX_KEY_BUILDER.out1(),
        ()->create(ModelXapiBox.class));


  abstract String getId () ;

  abstract String setId (String id) ;

  abstract IntTo<ModelXapiText> getText () ;

  abstract IntTo<ModelXapiText> setText (IntTo<ModelXapiText> text) ;

  abstract String getTitle () ;

  abstract String setTitle (String title) ;

  abstract BoxPosition getPosition () ;

  abstract BoxPosition setPosition (BoxPosition position) ;

  abstract BoxSize getSize () ;

  abstract BoxSize setSize (BoxSize size) ;

}
