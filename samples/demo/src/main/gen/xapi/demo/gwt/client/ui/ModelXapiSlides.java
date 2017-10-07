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

  public String MODEL_XAPI_SLIDES = "xapiSlides";

  public Out1<KeyBuilder> XAPI_SLIDES_KEY_BUILDER = forType(MODEL_XAPI_SLIDES);

  public Out1<ModelBuilder<ModelXapiSlides>> XAPI_SLIDES_MODEL_BUILDER = 
      ()->
        build(XAPI_SLIDES_KEY_BUILDER.out1(),
        ()->create(ModelXapiSlides.class));


  abstract ModelXapiSlide getShowing () ;

  abstract ModelXapiSlide setShowing (ModelXapiSlide showing) ;

  abstract ModelKey getNext () ;

  abstract ModelKey setNext (ModelKey next) ;

}
