package xapi.dev.model;

import xapi.annotation.model.*;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.collect.fifo.Fifo;
import xapi.collect.impl.StringToAbstract;
import xapi.model.api.Model;
import xapi.model.api.NestedModel;
import xapi.model.api.PersistentModel;
import xapi.source.api.HasQualifiedName;

public class HasModelFields implements java.io.Serializable {

//  private Serializable defaultSerializable;
  private ClientToServer defaultToServer;
  private ServerToClient defaultToClient;

  StringTo<ModelField> fields = new StringToAbstract<ModelField>(ModelField.class);
  private Persistent defaultPersistence;
  private Key key;

  public ModelField getOrMakeField(final String field) {
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

  public Iterable<ModelField> getAllSerializable() {
    final Fifo<ModelField> fifo = X_Collect.newFifo();
    for (final ModelField field : fields.values()) {
      final Serializable serial = field.getSerializable();
      if (serial == null) {
        final ClientToServer c2s = field.getClientToServer();
        if (c2s != null && c2s.enabled()) {
          fifo.give(field);
          continue;
        }
        final ServerToClient s2c = field.getServerToClient();
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
  public void setDefaultSerializable(final Serializable defaultSerializable) {
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

  public void setDefaultPersistence(final Persistent persist) {
    this.defaultPersistence = persist;
  }

  public void setKey(final Key key) {
    this.key = key;
  }

  public static boolean isPersistentModel(final HasQualifiedName type) {
    return type.getQualifiedName().equals(PersistentModel.class.getName());
  }
  public static boolean isNestedModel(final HasQualifiedName type) {
    return type.getQualifiedName().equals(NestedModel.class.getName());
  }
  public static boolean isModel(final HasQualifiedName type) {
    return type.getQualifiedName().equals(Model.class.getName())
        ||isPersistentModel(type)||isNestedModel(type);
  }

}
