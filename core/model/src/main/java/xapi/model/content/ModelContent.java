package xapi.model.content;


import xapi.model.X_Model;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelBuilder;
import xapi.util.api.ProvidesValue;

import java.util.function.Supplier;

public interface ModelContent extends ModelText, HasVotes, HasAuthor {

  String MODEL_CONTENT_TYPE = "content";

  ProvidesValue<KeyBuilder> CONTENT_KEY_BUILDER = new ProvidesValue<KeyBuilder>() {
    @Override
    public KeyBuilder get() {
      return KeyBuilder.build(MODEL_CONTENT_TYPE);
    }
  };

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

  /**
   * @return an array of nodes which are related to this content,
   * but not directly contained by this content.
   */
  ModelContent[] getRelated();
  ModelContent setRelated(ModelContent[] related);

  /**
   * @return an array of direct child nodes.
   */
  ModelContent[] getChildren();
  ModelContent setChildren(ModelContent[] children);

  String getPermaLink();
  ModelContent setPermaLink(String permalink);

}
