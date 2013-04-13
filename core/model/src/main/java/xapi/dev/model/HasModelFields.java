package xapi.dev.model;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.Serializable;
import xapi.annotation.model.ServerToClient;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.collect.api.StringTo;

public class HasModelFields {

  private Serializable defaultSerializable;
  private ClientToServer defaultToServer;
  private ServerToClient defaultToClient;

  StringTo<ModelField> fields = X_Collect.newStringMap(ModelField.class);

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
   * @return the defaultSerializable
   */
  public Serializable getDefaultSerializable() {
    return defaultSerializable;
  }

  /**
   * @param defaultSerializable the defaultSerializable to set
   */
  public void setDefaultSerializable(Serializable defaultSerializable) {
    this.defaultSerializable = defaultSerializable;
  }

  /**
   * @return the defaultToServer
   */
  public ClientToServer getDefaultToServer() {
    return defaultToServer;
  }

  /**
   * @param defaultToServer the defaultToServer to set
   */
  public void setDefaultToServer(ClientToServer defaultToServer) {
    this.defaultToServer = defaultToServer;
  }

  /**
   * @return the defaultToClient
   */
  public ServerToClient getDefaultToClient() {
    return defaultToClient;
  }

  /**
   * @param defaultToClient the defaultToClient to set
   */
  public void setDefaultToClient(ServerToClient defaultToClient) {
    this.defaultToClient = defaultToClient;
  }

}
