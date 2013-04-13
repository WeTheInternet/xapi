package xapi.model.content;

public interface HasComments {

  ModelComment[] getComments();
  ModelContent setComments(ModelComment[] comments);

}
