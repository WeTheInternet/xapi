package xapi.mvn.model;

import xapi.model.api.Model;

public interface MvnList <M extends Model> extends Model{

  M[] elements();
  MvnList<M> elements(M[] elements);

}
