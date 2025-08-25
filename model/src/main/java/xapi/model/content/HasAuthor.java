package xapi.model.content;

import xapi.annotation.model.KeyOnly;
import xapi.model.user.ModelUser;

public interface HasAuthor {

  @KeyOnly
  ModelUser getAuthor();
  HasAuthor setAuthor(ModelUser user);

}
