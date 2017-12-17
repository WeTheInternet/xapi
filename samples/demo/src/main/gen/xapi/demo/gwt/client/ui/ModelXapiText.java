package xapi.demo.gwt.client.ui;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;


import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;

public interface ModelXapiText extends Model{

   static KeyBuilder newKey () {
    return XAPI_TEXT_KEY_BUILDER.out1();
  }

  String MODEL_XAPI_TEXT = "xapiText";

  Out1<KeyBuilder> XAPI_TEXT_KEY_BUILDER = forType(MODEL_XAPI_TEXT);

  Out1<ModelBuilder<ModelXapiText>> XAPI_TEXT_MODEL_BUILDER = 
      ()->
        build(XAPI_TEXT_KEY_BUILDER.out1(),
        ()->create(ModelXapiText.class));


    String getValue () ;

    String setValue (String value) ;

    int[] getFormat () ;

    int[] setFormat (int[] format) ;

}
