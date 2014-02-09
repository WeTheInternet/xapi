package xapi.dev.model;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.Key;
import xapi.annotation.model.Persistent;
import xapi.annotation.model.Serializable;
import xapi.annotation.model.ServerToClient;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.collect.api.StringTo;
import xapi.collect.impl.StringToAbstract;
import xapi.model.api.Model;
import xapi.model.api.NestedModel;
import xapi.model.api.PersistentModel;
import xapi.source.api.HasQualifiedName;

public class HasModelFields {

//  private Serializable defaultSerializable;
  private ClientToServer defaultToServer;
  private ServerToClient defaultToClient;

  StringTo<ModelField> fields = new StringToAbstract<ModelField>();
  private Persistent defaultPersistence;
  private Key key;

  public ModelField getOrMakeField(String field) {
    assert field.length() > 0 : "Cannot have a field named \"\"";
    ModelField f = fields.get(field);
    if (f == null) {
      f = new ModelField(field);
      fields.put(field, f);
    }
    return f;
  }

  public Iterable<ModelField> getAllFields() {
    return fields.values();
  }

  public Iterable<ModelField> getAllPublicSetters() {
    Fifo<ModelField> fifo = X_Collect.newFifo();
    for (ModelField field : fields.values())
      if (field.isPublicSetter())
        fifo.give(field);
    return fifo.forEach();
  }

  public Iterable<ModelField> getAllPublicAdders() {
    Fifo<ModelField> fifo = X_Collect.newFifo();
    for (ModelField field : fields.values())
      if (field.isPublicAdder())
        fifo.give(field);
    return fifo.forEach();
  }

  public Iterable<ModelField> getAllPublicRemovers(){
    Fifo<ModelField> fifo = X_Collect.newFifo();
    for (ModelField field : fields.values())
      if (field.isPublicRemover())
        fifo.give(field);
    return fifo.forEach();
  }

  public Iterable<ModelField> getAllPublicClears() {
    Fifo<ModelField> fifo = X_Collect.newFifo();
    for (ModelField field : fields.values())
      if (field.isPublicClear())
        fifo.give(field);
    return fifo.forEach();
  }


  public Iterable<ModelField> getAllSerializable() {
    Fifo<ModelField> fifo = X_Collect.newFifo();
    for (ModelField field : fields.values()) {
      Serializable serial = field.getSerializable();
      if (serial == null) {
        ClientToServer c2s = field.getClientToServer();
        if (c2s != null && c2s.enabled()) {
          fifo.give(field);
          continue;
        }
        ServerToClient s2c = field.getServerToClient();
        if (s2c != null && s2c.enabled()) {
          fifo.give(field);
          continue;
        }
      } else {
        // class is marked serializable
        fifo.give(field);
      }
    }

    return fifo.forEach();
  }

  /**
   * @param defaultSerializable the default Serializable policy to set
   */
  public void setDefaultSerializable(Serializable defaultSerializable) {
    if (defaultSerializable == null) {
      defaultToClient = null;
      defaultToServer = null;
    } else {
      defaultToClient = defaultSerializable.serverToClient();
      defaultToServer = defaultSerializable.clientToServer();
    }
  }

  /**
   * @return the defaultToServer
   */
  public ClientToServer getDefaultToServer() {
    return defaultToServer;
  }


  /**
   * @return the defaultToClient
   */
  public ServerToClient getDefaultToClient() {
    return defaultToClient;
  }

  public void setDefaultPersistence(Persistent persist) {
    this.defaultPersistence = persist;
  }

  public void setKey(Key key) {
    this.key = key;
  }

  public static boolean isPersistentModel(HasQualifiedName type) {
    return type.getQualifiedName().equals(PersistentModel.class.getName());
  }
  public static boolean isNestedModel(HasQualifiedName type) {
    return type.getQualifiedName().equals(NestedModel.class.getName());
  }
  public static boolean isModel(HasQualifiedName type) {
    return type.getQualifiedName().equals(Model.class.getName())
        ||isPersistentModel(type)||isNestedModel(type);
  }

}
