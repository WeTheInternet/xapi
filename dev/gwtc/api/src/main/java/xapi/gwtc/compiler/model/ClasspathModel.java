package xapi.gwtc.compiler.model;

import xapi.annotation.model.DeleterFor;
import xapi.annotation.model.GetterFor;
import xapi.annotation.model.SetterFor;
import xapi.model.api.Model;

public interface ClasspathModel extends Model {

  @GetterFor("dependencies")
  String[] dependencies();
  @GetterFor("sources")
  String[] sources();

  @SetterFor("dependencies")
  void addDependency(String source);
  @DeleterFor("dependencies")
  void removeDependency(String source);

  @SetterFor("sources")
  void addSource(String source);
  @DeleterFor("sources")
  void removeSource(String source);

}
