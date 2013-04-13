package xapi.dev.model;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.Key;
import xapi.annotation.model.Persistent;
import xapi.annotation.model.Serializable;
import xapi.annotation.model.ServerToClient;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.source.api.IsType;
import xapi.util.X_String;

public class ModelField {

  class ModelMethod {
    String fieldName;
    IsType returnType;
    String methodName;
  }

  class GetterMethod extends ModelMethod{
   boolean toArray;
   boolean toCollection;
   boolean toMap;
   boolean toHasValues;
   boolean toIterable;
  }

  class SetterMethod extends ModelMethod{
    IsType[] params;
    public boolean fluent;
    boolean fromArray;
    boolean fromCollection;
    boolean fromMap;
    boolean fromHasValues;
    boolean fromIterable;
    boolean firesEvents;
  }

  class DeleterMethod extends ModelMethod{
    IsType[] params;
    public boolean fluent;
    boolean fromArray;
    boolean fromCollection;
    boolean fromMap;
    boolean fromHasValues;
    boolean fromIterable;
    boolean firesEvents;
  }

  class QueryMethod extends ModelMethod{
    IsType[] params;
    public boolean fluent;
    boolean fromArray;
    boolean fromCollection;
    boolean fromMap;
    boolean fromHasValues;
    boolean fromIterable;
    boolean firesEvents;
  }

  class ActionMethod {
    IsType returnType;
    String methodName;
    IsType[] params;
  }

  private final String name;
  private String type;

  private ClientToServer clientToServer;
  private ServerToClient serverToClient;
  private Key key;
  private Persistent persistent;
  private Serializable serializable;

  private boolean listType;
  private boolean mapType;
  private boolean publicSetter;
  private boolean publicAdder;
  private boolean publicRemover;
  private boolean publicClear;

  private final Fifo<GetterMethod> getters;
  private final Fifo<SetterMethod> setters;
  private final Fifo<ActionMethod> actions;

  public ModelField(String name) {
    this.name = name;
    getters = X_Collect.newFifo();
    setters = X_Collect.newFifo();
    actions = X_Collect.newFifo();
  }

  /**
   * @return the clientToServer
   */
  public ClientToServer getClientToServer() {
    return clientToServer;
  }
  /**
   * @param clientToServer the clientToServer to set
   * @return
   */
  public ModelField setClientToServer(ClientToServer clientToServer) {
    this.clientToServer = clientToServer;
    return this;
  }
  /**
   * @return the key
   */
  public Key getKey() {
    return key;
  }
  /**
   * @param key the key to set
   * @return
   */
  public ModelField setKey(Key key) {
    this.key = key;
    return this;
  }
  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
  /**
   * @return the serverToClient
   */
  public ServerToClient getServerToClient() {
    return serverToClient;
  }
  /**
   * @param serverToClient the serverToClient to set
   * @return
   */
  public ModelField setServerToClient(ServerToClient serverToClient) {
    this.serverToClient = serverToClient;
    return this;
  }
  /**
   * @return the serializable
   */
  public Serializable getSerializable() {
    return serializable;
  }
  /**
   * @param serializable the serializable to set
   * @return
   */
  public ModelField setSerializable(Serializable serializable) {
    this.serializable = serializable;
    return this;
  }
  /**
   * @return the persistent
   */
  public Persistent getPersistent() {
    return persistent;
  }
  /**
   * @param persistent the persistent to set
   * @return
   */
  public ModelField setPersistent(Persistent persistent) {
    this.persistent = persistent;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this ? true :
      obj instanceof ModelField ? name.equals(((ModelField)obj).name) : false;
  }
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * @return the publicClear
   */
  public boolean isPublicClear() {
    return publicClear;
  }

  /**
   * @param publicClear the publicClear to set
   * @return
   */
  public ModelField setPublicClear(boolean publicClear) {
    this.publicClear = publicClear;
    return this;
  }

  /**
   * @return the publicRemover
   */
  public boolean isPublicRemover() {
    return publicRemover;
  }

  /**
   * @param publicRemover the publicRemover to set
   * @return
   */
  public ModelField setPublicRemover(boolean publicRemover) {
    this.publicRemover = publicRemover;
    return this;
  }

  /**
   * @return the publicAdder
   */
  public boolean isPublicAdder() {
    return publicAdder;
  }

  /**
   * @param publicAdder the publicAdder to set
   * @return
   */
  public ModelField setPublicAdder(boolean publicAdder) {
    this.publicAdder = publicAdder;
    return this;
  }

  /**
   * @return the publicSetter
   */
  public boolean isPublicSetter() {
    return publicSetter;
  }

  /**
   * @param publicSetter the publicSetter to set
   * @return
   */
  public ModelField setPublicSetter(boolean publicSetter) {
    this.publicSetter = publicSetter;
    return this;
  }

  /**
   * @return the mapType
   */
  public boolean isMapType() {
    return mapType;
  }

  /**
   * @param mapType the mapType to set
   * @return
   */
  public ModelField setMapType(boolean mapType) {
    this.mapType = mapType;
    return this;
  }

  /**
   * @return the listType
   */
  public boolean isListType() {
    return listType;
  }

  /**
   * @param listType the listType to set
   * @return
   */
  public ModelField setListType(boolean listType) {
    this.listType = listType;
    return this;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }



  public GetterMethod addGetter(IsType returns, String methodName) {
    GetterMethod mthd = new GetterMethod();
    mthd.returnType = returns;
    mthd.methodName = methodName;
    getters.give(mthd);
    return mthd;
  }

  public SetterMethod addSetter(IsType returns, String methodName, IsType[] parameters) {
    SetterMethod mthd = new SetterMethod();
    mthd.returnType = returns;
    mthd.methodName = methodName;
    mthd.params = parameters;
    setters.give(mthd);
    mthd.fluent = mthd.returnType.getQualifiedName().equals(type);
    return mthd;
  }

  public void addAction(IsType returns, String methodName, IsType[] parameters) {
    ActionMethod mthd = new ActionMethod();
    mthd.returnType = returns;
    mthd.methodName = methodName;
    mthd.params = parameters;
    actions.give(mthd);
  }

  public Iterable<GetterMethod> getGetters() {
    return getters.forEach();
  }


}
