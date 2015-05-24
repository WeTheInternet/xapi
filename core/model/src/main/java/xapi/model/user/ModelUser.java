package xapi.model.user;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.FieldValidator;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.annotation.model.Serializable;
import xapi.annotation.model.ServerToClient;
import xapi.model.api.Model;
import xapi.util.validators.ChecksStringNotEmpty;

@IsModel(
    modelType = "user"
    ,persistence = @Persistent(strategy=PersistenceStrategy.Remote)
    ,serializable = @Serializable(
        clientToServer=@ClientToServer(encrypted=true)
        ,serverToClient = @ServerToClient(encrypted=true)
      )
    )
public interface ModelUser extends Model {

  @GetterFor("id")
  @FieldValidator(validators=ChecksStringNotEmpty.class)
  String id();


  @FieldValidator(validators=ChecksStringNotEmpty.class)
  String getEmail();

  @FieldValidator(validators=ChecksStringNotEmpty.class)
  String getFirstName();

  @FieldValidator(validators=ChecksStringNotEmpty.class)
  String getLastName();

  ModelUser setEmail(String email);
  ModelUser setFirstName(String firstName);
  ModelUser setLastName(String lastName);
  ModelUser setId(String id);

  boolean isValid();
}
