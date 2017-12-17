package xapi.ui.edit;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;


import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;

public interface ModelInputText extends Model{

   static KeyBuilder newKey () {
    return INPUT_TEXT_KEY_BUILDER.out1();
  }

  String MODEL_INPUT_TEXT = "inputText";

  Out1<KeyBuilder> INPUT_TEXT_KEY_BUILDER = forType(MODEL_INPUT_TEXT);

  Out1<ModelBuilder<ModelInputText>> INPUT_TEXT_MODEL_BUILDER = 
      ()->
        build(INPUT_TEXT_KEY_BUILDER.out1(),
        ()->create(ModelInputText.class));


   abstract String getValue () ;

   abstract String setValue (String value) ;

   abstract String getTitle () ;

   abstract String setTitle (String title) ;

   abstract String getPlaceholder () ;

   abstract String setPlaceholder (String placeholder) ;

}
