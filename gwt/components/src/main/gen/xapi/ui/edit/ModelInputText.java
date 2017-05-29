package xapi.ui.edit;

import xapi.fu.Out1;
import xapi.model.X_Model;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;

public interface ModelInputText extends Model{

    String TYPE = "input-text";

    String MODEL_INPUT_TEXT_TYPE = "input-text";

    Out1<KeyBuilder> INPUT_TEXT_KEY_BUILDER = KeyBuilder.forType(MODEL_INPUT_TEXT_TYPE);

    Out1<ModelBuilder<ModelInputText>> MODEL_INPUT_TEXT_BUILDER =
        ()->ModelBuilder
            .build(INPUT_TEXT_KEY_BUILDER.out1(), ()-> X_Model.create(ModelInputText.class));

    abstract String getValue () ;

  abstract String setValue (String value) ;

  abstract String getTitle () ;

  abstract String setTitle (String title) ;

  abstract String getPlaceholder () ;

  abstract String setPlaceholder (String placeholder) ;

}
