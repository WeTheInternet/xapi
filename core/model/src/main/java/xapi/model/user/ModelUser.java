package xapi.model.user;

import xapi.annotation.model.FieldValidator;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.IsModel;
import xapi.model.api.Model;
import xapi.util.validators.ChecksStringNotEmpty;

@IsModel
public interface ModelUser extends Model {

  @GetterFor("id")
  @FieldValidator(validators=ChecksStringNotEmpty.class)
  String id();


  @FieldValidator(validators=ChecksStringNotEmpty.class)
  String getEmail();
  String getFirstName();
  String getLastName();

  ModelUser setEmail(String email);
  ModelUser setFirstName(String firstName);
  ModelUser setLastName(String lastName);


  boolean isValid();
}
