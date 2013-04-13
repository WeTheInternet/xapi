package xapi.model.user;

import xapi.annotation.model.FieldName;
import xapi.annotation.model.FieldValidator;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.Key;
import xapi.annotation.model.Persistent;
import xapi.annotation.model.Serializable;
import xapi.model.api.Model;
import xapi.util.validators.ChecksStringNotEmpty;

@Persistent
@Serializable
public interface ModelUser extends Model {

  interface A extends Model{
    String getB();
  }
  @Key
  @FieldName(name="id")
  @GetterFor(name="id")
  @FieldValidator(validators=ChecksStringNotEmpty.class)
  String uuid();


  A getA();
  @FieldValidator(validators=ChecksStringNotEmpty.class)
  String getEmail();
  String getFirstName();
  String getLastName();

  ModelUser setEmail(String email);
  ModelUser setFirstName(String firstName);
  ModelUser setLastName(String lastName);


  boolean isValid();
}
