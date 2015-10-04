package xapi.model.content;

import xapi.model.user.ModelUser;

public interface ModelComment extends ModelContent{

  String getContentId();
  ModelComment setContentId(String contentId);
}
