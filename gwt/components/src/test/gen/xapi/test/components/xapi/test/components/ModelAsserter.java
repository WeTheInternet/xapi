package xapi.test.components.xapi.test.components;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;


import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;

public interface ModelAsserter extends Model{

  public String MODEL_ASSERTER = "asserter";

  public Out1<KeyBuilder> ASSERTER_KEY_BUILDER = forType(MODEL_ASSERTER);

  public Out1<ModelBuilder<ModelAsserter>> ASSERTER_MODEL_BUILDER = 
      ()->
        build(ASSERTER_KEY_BUILDER.out1(),
        ()->create(ModelAsserter.class));


  abstract String getTemplate () ;

  abstract String setTemplate (String template) ;

  abstract String getTag () ;

  abstract String setTag (String tag) ;

}
