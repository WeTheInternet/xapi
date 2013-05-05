package xapi.model.impl;


import xapi.annotation.inject.InstanceDefault;
import xapi.model.api.ModelKey;

@InstanceDefault(implFor=ModelKey.class)
public class ModelKeyDefault implements ModelKey{


  private ModelKey parentKey;
  private String kind;

  private String namespace;

  private long id;
  private String name;

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public String getKind() {
    return kind;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public ModelKey getParent() {
    return parentKey;
  }

  @Override
  public boolean isComplete() {
    return name!=null || id > 0;
  }

  @Override
  public ModelKey getChild(String kind, long id) {
    return null;
  }

  @Override
  public ModelKey getChild(String kind, String id) {
    return null;
  }

}
