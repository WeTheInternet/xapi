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

  public String MODEL_XAPI_TEXT = "xapiText";

  public Out1<KeyBuilder> XAPI_TEXT_KEY_BUILDER = forType(MODEL_XAPI_TEXT);

  public Out1<ModelBuilder<ModelXapiText>> XAPI_TEXT_MODEL_BUILDER = 
      ()->
        build(XAPI_TEXT_KEY_BUILDER.out1(),
        ()->create(ModelXapiText.class));


  abstract String getValue () ;

  abstract String setValue (String value) ;

  abstract int[] getFormat () ;

  abstract int[] setFormat (int[] format) ;

}
