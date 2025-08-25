package xapi.model.content;

import xapi.annotation.model.KeyOnly;

public interface HasComments {

  ModelComment[] getComments();
  ModelContent setComments(ModelComment[] comments);

}
