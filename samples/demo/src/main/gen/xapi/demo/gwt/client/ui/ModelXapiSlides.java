package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;


import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;
import xapi.model.api.ModelKey;

public interface ModelXapiSlides extends Model{

   static KeyBuilder newKey () {
    return XAPI_SLIDES_KEY_BUILDER.out1();
  }

  String MODEL_XAPI_SLIDES = "xapiSlides";

  Out1<KeyBuilder> XAPI_SLIDES_KEY_BUILDER = forType(MODEL_XAPI_SLIDES);

  Out1<ModelBuilder<ModelXapiSlides>> XAPI_SLIDES_MODEL_BUILDER = 
      ()->
        build(XAPI_SLIDES_KEY_BUILDER.out1(),
        ()->create(ModelXapiSlides.class));


    ModelXapiSlide getShowing () ;

    ModelXapiSlide setShowing (ModelXapiSlide showing) ;

    ModelKey getNext () ;

    ModelKey setNext (ModelKey next) ;

}
