package xapi.model.content;


public interface ModelContent extends ModelText, HasVotes{

  String MODEL_CONTENT_TYPE = "content";

  ModelContent[] getRelated();
  ModelContent setRelated(ModelContent[] related);


}
