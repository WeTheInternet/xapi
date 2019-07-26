package xapi.model.content;

import xapi.model.user.ModelUser;

public interface HasAuthor {

  ModelUser getAuthor();
  HasAuthor setAuthor(ModelUser user);

}
