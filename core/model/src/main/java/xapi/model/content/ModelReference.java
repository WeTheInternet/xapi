package xapi.model.content;

import xapi.model.X_Model;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelBuilder;
import xapi.model.user.ModelUser;
import xapi.util.api.ProvidesValue;

import java.util.function.Supplier;

/**
 * Created by james on 25/10/15.
 */
public interface ModelReference extends ModelText, HasAuthor {

  String MODEL_REFERENCE_TYPE = "reference";

  ProvidesValue<KeyBuilder> CONTENT_KEY_BUILDER = KeyBuilder.forType(MODEL_REFERENCE_TYPE);

  ProvidesValue<ModelBuilder<ModelContent>> CONTENT_MODEL_BUILDER = new ProvidesValue<ModelBuilder<ModelContent>>() {
    @Override
    public ModelBuilder<ModelContent> get() {
      return ModelBuilder.build(CONTENT_KEY_BUILDER.get(), new Supplier<ModelContent>() {
                                  @Override
                                  public ModelContent get() {
                                    return X_Model.create(ModelContent.class);
                                  }
                                })
          .withProperty("related", new ModelContent[0])
          .withProperty("children", new ModelContent[0])
          ;
    }
  };


  String getReference();
  ModelReference setReference(String reference);

  String getQuoteText();
  ModelReference setQuoteText(String quoteText);

  ModelUser getQuoted();
  ModelReference setQuoted(ModelUser quoted);

}
