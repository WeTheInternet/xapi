package xapi.model.impl;


import java.util.HashMap;
import java.util.Map;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.NestedModel;
import xapi.model.api.PersistentModel;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

@InstanceDefault(implFor=Model.class)
public class AbstractModel implements Model, PersistentModel, NestedModel{

  protected StringTo<Object> map;
  private Model parent;
  private ModelKey key;

  public AbstractModel() {
    this.map = X_Collect.newStringMap(Object.class);
  }

  @Override
  public ModelKey getKey(){
    return key;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(String key) {
    return (T)map.get(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(String key, T dflt) {
    Object o = getProperty(key);
    if (o == null)
      return dflt;
    return (T)o;
  }

  @Override
  public Map<String, Object> getProperties() {
    Map<String,Object> props = emptyMap();
    for (String key : map.keys())
      props.put(key, map.get(key));
    return props;
  }

  protected Map<String,Object> emptyMap() {
    return new HashMap<String, Object>();
  }

  @Override
  public Model setProperty(String key, Object value) {
    try {
      map.put(key, value);
    } catch (Throwable e) {
      X_Log.error(e);
    }
    return this;
  }

  protected AbstractModel createNew() {
    return new AbstractModel();
  }
  @Override
  public Model removeProperty(String key) {
    map.remove(key);
    return this;
  }

  @Override
  public Model child(String propName) {
    Object existing = map.get(propName);
    assert existing == null || existing instanceof Model :
      "You requested a child model with property name "+propName+", but an object of type "
        +existing.getClass()+" already existed in this location: "+existing;
    if (existing == null){
      AbstractModel child = createNew();//ensures subclasses can control child classes.
      child.parent = this;
      existing = child;
      map.put(propName, existing);
    }
    return (Model) existing;
  }


  @Override
  public Model parent() {
    return parent;
  }

  @Override
  public Model cache(SuccessHandler<Model> callback) {
    X_Model.cache().cacheModel(this,callback);
    return this;
  }

  @Override
  public Model persist(SuccessHandler<Model> callback) {
    X_Model.persist(this, callback);
    return this;
  }

  @Override
  public Model delete(SuccessHandler<Model> callback) {
    X_Model.cache().deleteModel(this,callback);
    return this;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Model load(SuccessHandler<Model> callback, boolean useCache) {
    try{
      Model model = X_Model.cache().getModel(getKey().toString());
      callback.onSuccess(model);
    }catch(Exception e){
      if (callback instanceof ErrorHandler)
        ((ErrorHandler) callback).onError(e);
    }
    return this;
  }

  @Override
  public Model flush() {
    return this;
  }

  @Override
  public void clear() {
    map.clear();
  }

}
