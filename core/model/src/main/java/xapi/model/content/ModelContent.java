package xapi.model.content;


public interface ModelContent extends ModelText, HasVotes{


  ModelContent[] getRelated();
  ModelContent setRelated(ModelContent[] related);


}
