package xapi.model.content;


import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;

public interface ModelContent extends ModelText, HasVotes, HasAuthor {

  String MODEL_CONTENT_TYPE = "content";

  Out1<KeyBuilder> CONTENT_KEY_BUILDER = KeyBuilder.forType(MODEL_CONTENT_TYPE);

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
