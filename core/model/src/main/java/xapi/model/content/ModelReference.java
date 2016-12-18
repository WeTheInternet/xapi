package xapi.model.content;

import xapi.fu.Out1;
import xapi.model.X_Model;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelBuilder;
import xapi.model.user.ModelUser;

/**
 * Created by james on 25/10/15.
 */
public interface ModelReference extends ModelText, HasAuthor {

  String MODEL_REFERENCE_TYPE = "reference";

  Out1<KeyBuilder> CONTENT_KEY_BUILDER = KeyBuilder.forType(MODEL_REFERENCE_TYPE);

  Out1<ModelBuilder<ModelContent>> CONTENT_MODEL_BUILDER =
      ()->ModelBuilder
          .build(CONTENT_KEY_BUILDER.out1(), ()->X_Model.create(ModelContent.class))
          .withProperty("related", new ModelContent[0])
          .withProperty("children", new ModelContent[0]);

  String getReference();
  ModelReference setReference(String reference);

  String getQuoteText();
  ModelReference setQuoteText(String quoteText);

  ModelUser getQuoted();
  ModelReference setQuoted(ModelUser quoted);

}
