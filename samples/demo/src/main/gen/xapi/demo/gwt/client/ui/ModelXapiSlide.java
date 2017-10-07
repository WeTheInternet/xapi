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

  public String MODEL_XAPI_SLIDE = "xapiSlide";

  public Out1<KeyBuilder> XAPI_SLIDE_KEY_BUILDER = forType(MODEL_XAPI_SLIDE);

  public Out1<ModelBuilder<ModelXapiSlide>> XAPI_SLIDE_MODEL_BUILDER = 
      ()->
        build(XAPI_SLIDE_KEY_BUILDER.out1(),
        ()->create(ModelXapiSlide.class));


  abstract IntTo<ModelKey> getItems () ;

  abstract IntTo<ModelKey> setItems (IntTo<ModelKey> items) ;

  abstract String getTitle () ;

  abstract String setTitle (String title) ;

}
