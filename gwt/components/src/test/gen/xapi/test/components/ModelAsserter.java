package xapi.test.components;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;


import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;

public interface ModelAsserter extends Model{

   static KeyBuilder newKey () {
    return ASSERTER_KEY_BUILDER.out1();
  }

   static ModelBuilder<ModelAsserter> modelBuilder () {
    return ASSERTER_MODEL_BUILDER.out1();
  }

   static ModelAsserter newModel () {
    return modelBuilder().buildModel();
  }

  String MODEL_ASSERTER = "asserter";

  Out1<KeyBuilder> ASSERTER_KEY_BUILDER = forType(MODEL_ASSERTER);

  Out1<ModelBuilder<ModelAsserter>> ASSERTER_MODEL_BUILDER = 
      ()->build(newKey(), ()->create(ModelAsserter.class));


    String getTemplate () ;

    String setTemplate (String template) ;

    String getTag () ;

    String setTag (String tag) ;

}
