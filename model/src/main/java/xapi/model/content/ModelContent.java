package xapi.model.content;


import xapi.annotation.model.KeyOnly;
import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelList;

public interface ModelContent extends ModelText, HasVotes, HasAuthor {

  String MODEL_CONTENT_TYPE = "content";

  Out1<KeyBuilder> CONTENT_KEY_BUILDER = KeyBuilder.forType(MODEL_CONTENT_TYPE);

  /**
   * @return an array of nodes which are related to this content,
   * but not directly contained by this content.
   */
  @KeyOnly(autoSave = true)
  ModelList<ModelContent> getRelated();
  default ModelList<ModelContent> related() {
    return getOrCreateModelList(ModelContent.class, this::getRelated, this::setRelated);
  }
  ModelContent setRelated(ModelList<ModelContent> related);

  /**
   * @return an array of direct child nodes.
   */
  ModelList<ModelContent> getChildren();
  default ModelList<ModelContent> children() {
      return getOrCreateModelList(ModelContent.class, this::getChildren, this::setChildren);
  }
  ModelContent setChildren(ModelList<ModelContent> children);

  String getPermaLink();
  ModelContent setPermaLink(String permalink);

}
