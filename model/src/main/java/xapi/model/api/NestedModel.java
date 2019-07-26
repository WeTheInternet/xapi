package xapi.model.api;

public interface NestedModel extends Model{

  //nested types
  Model child(String propName);
  Model parent();
  
}
