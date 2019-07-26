package xapi.dev.model;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.FieldValidator;
import xapi.annotation.model.Key;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.annotation.model.Serializable;
import xapi.annotation.model.SerializationStrategy;
import xapi.annotation.model.ServerToClient;
import xapi.collect.impl.SimpleFifo;
import xapi.fu.Maybe;
import xapi.fu.X_Fu;
import xapi.log.X_Log;
import xapi.source.api.IsParameterizedType;
import xapi.source.api.IsType;
import xapi.source.api.IsTypeArgument;
import xapi.validate.ValidatesValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class ModelField implements java.io.Serializable {

  private static final long serialVersionUID = -1697272589093249083L;


  class ModelMethod implements java.io.Serializable {
    private static final long serialVersionUID = -7865206427761038557L;
    String fieldName;
    // hm... this needs to be something that has type arguments, not type parameters...
    // IsTypeWithArgs?  IsTypeParameterized?
    IsType returnType;
    String methodName;
    public IsType[] generics;
    String[] componentGetter;

    public void addComponentGetter(int index, String val) {
      if (componentGetter == null) {
        componentGetter = (String[]) X_Fu.newArray(String.class, index+1);
      } else if (componentGetter.length < index+1) {
        componentGetter = X_Fu.copy(componentGetter, index+1);
      }
      componentGetter[index] = val;
    }
  }

  class GetterMethod extends ModelMethod{
    private static final long serialVersionUID = -7494752742665409918L;
   boolean toArray;
   boolean toCollection;
   boolean toMap;
   boolean toHasValues;
   boolean toIterable;
  }

  class SetterMethod extends ModelMethod{
    private static final long serialVersionUID = -345008117582324787L;
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
    private static final long serialVersionUID = 7402005778480651359L;
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
    private static final long serialVersionUID = -4769829367905793190L;
    IsType[] params;
    public boolean fluent;
    boolean fromArray;
    boolean fromCollection;
    boolean fromMap;
    boolean fromHasValues;
    boolean fromIterable;
    boolean firesEvents;
  }

  class ActionMethod implements java.io.Serializable {
    IsType returnType;
    String methodName;
    String fieldName;
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
  private boolean rememberDefault;
  private boolean publicSetter;
  private boolean publicAdder;
  private boolean publicRemover;
  private boolean publicClear;
  private boolean c2sEnabled = true;
  private boolean s2cEnabled = true;
  private boolean c2sEncrypted = false;
  private boolean s2cEncrypted = false;
  private boolean obfuscated = false;
  private SerializationStrategy c2sSerializer = SerializationStrategy.ProtoStream;
  private SerializationStrategy s2cSerializer = SerializationStrategy.ProtoStream;
  private PersistenceStrategy persistenceStrategy;
  private final ArrayList<Class<? extends ValidatesValue<?>>> validators = new ArrayList<>();


  private final SimpleFifo<GetterMethod> getters;
  private final SimpleFifo<SetterMethod> setters;
  private final SimpleFifo<ActionMethod> actions;

  public ModelField(final String name) {
    this.name = name;
    getters = new SimpleFifo<>();
    setters = new SimpleFifo<>();
    actions = new SimpleFifo<>();
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
  public ModelField setClientToServer(final ClientToServer clientToServer) {
    this.clientToServer = clientToServer;
    if (clientToServer != null) {
      c2sEnabled = clientToServer.enabled();
      c2sEncrypted = clientToServer.enabled();
      c2sSerializer = clientToServer.serializer();
    }
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
  public ModelField setKey(final Key key) {
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
  public ModelField setServerToClient(final ServerToClient serverToClient) {
    this.serverToClient = serverToClient;
    if (serverToClient != null) {
      s2cEnabled = serverToClient.enabled();
      s2cEncrypted = serverToClient.encrypted();
      s2cSerializer = serverToClient.serializer();
    }
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
  public ModelField setSerializable(final Serializable serializable) {
    this.serializable = serializable;
    if (serializable != null) {
      obfuscated = serializable.obfuscated();
      setClientToServer(serializable.clientToServer());
      setServerToClient(serializable.serverToClient());
    }
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
  public ModelField setPersistent(final Persistent persistent) {
    this.persistent = persistent;
    if (persistent != null) {
      persistenceStrategy = persistent.strategy();
    }
    return this;
  }

  @Override
  public boolean equals(final Object obj) {
    return obj == this ? true :
      obj instanceof ModelField ? name.equals(((ModelField)obj).name) : false;
  }
  @Override
  public int hashCode() {
    return name.hashCode();
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
  public void setType(final String type) {
    this.type = type;
  }

  public GetterMethod addGetter(
      IsType sourceType, final IsType returns, final String propertyName,
      final String methodName, final Annotation[] annotations, IsTypeArgument[] generics
  ) {
    final GetterMethod mthd = new GetterMethod();
    mthd.returnType = returns;
    final Maybe<IsParameterizedType> params = returns.ifParameterized();
    // Leaving previous hacks commented out for one commit; will delete later.
//    if (params.isPresent()) {
//      mthd.generics = params.get().getBounds()
//          .<IsType>map(X_Fu.weakener())
//          .toArray(IsType[]::new);
//    } else {
//      mthd.generics = new IsType[0];
//    }
    // TODO: move this egregious hack to some kind of service,
    // so we can at least generalize this nastiness (until we can expose better metadata)
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().getSimpleName().equals("ComponentType")) {
        int index = 0;
        try {
          Object result = annotation.annotationType().getMethod("index").invoke(annotation);
          index = (Integer) result;
        } catch (IllegalAccessException | InvocationTargetException e) {
          X_Log.error(ModelField.class, "Unexpected exception", e);
        } catch (NoSuchMethodException ignored) {
          // index is optional; default is 0
        }
        try {
          String val = (String) annotation.annotationType().getMethod("value").invoke(annotation);
          mthd.addComponentGetter(index, val);
        } catch (IllegalAccessException | InvocationTargetException e) {
          X_Log.error(ModelField.class, "Unexpected exception", e);
        } catch (NoSuchMethodException e) {
          X_Log.error(ModelField.class, annotation.annotationType(), " should have method `String value()`", e);
        }
        break;
      }
    }

//    if ("children".equals(propertyName) && sourceType.getQualifiedName().equals("xapi.ui.api.component.ComponentList")) {
//      mthd.componentGetter = "getChildType()";
//    }
    mthd.generics = generics;
    mthd.fieldName = propertyName;
    mthd.methodName = methodName;
    getters.give(mthd);
    addAnnotations(annotations);
    return mthd;
  }

  /**
   * @param annotations
   */
  private void addAnnotations(final Annotation[] annotations) {
    for (final Annotation anno : annotations) {
      if (anno instanceof Serializable) {
        final Serializable serializable = (Serializable) anno;

        obfuscated = serializable.obfuscated();

        final ClientToServer c2s = serializable.clientToServer();
        c2sSerializer = c2s.serializer();
        c2sEnabled = c2s.enabled();
        c2sEncrypted = c2s.encrypted();

        final ServerToClient s2c = serializable.serverToClient();
        s2cSerializer = s2c.serializer();
        s2cEnabled = s2c.enabled();
        s2cEncrypted = s2c.encrypted();

      } else if (anno instanceof Persistent) {
        final Persistent persistent = (Persistent) anno;
        persistenceStrategy = persistent.strategy();
      } else if (anno instanceof FieldValidator) {
        final FieldValidator validator = (FieldValidator) anno;
        for (final Class<? extends ValidatesValue<?>> validatesValue : validator.validators()) {
          validators.add(validatesValue);
        }
      } else {
        X_Log.trace(getClass(), "Unhandled annotation ",anno+" in ModelManifest.MethodData.addAnnotatons");
      }
    }
  }

  public SetterMethod addSetter(final IsType returns, final String propertyName,
      final String methodName, final Annotation[] annotations, final IsType[] parameters) {
    final SetterMethod mthd = new SetterMethod();
    mthd.returnType = returns;
    mthd.fieldName = propertyName;
    mthd.methodName = methodName;
    mthd.params = parameters;
    setters.give(mthd);
    mthd.fluent = mthd.returnType.getQualifiedName().equals(type);
    addAnnotations(annotations);
    return mthd;
  }

  public void addAction(final IsType returns, final String propertyName,
      final String methodName, final Annotation[] annotations, final IsType[] parameters) {
    final ActionMethod mthd = new ActionMethod();
    mthd.returnType = returns;
    mthd.fieldName = propertyName;
    mthd.methodName = methodName;
    mthd.params = parameters;
    actions.give(mthd);
    addAnnotations(annotations);
  }

  public Iterable<GetterMethod> getGetters() {
    return getters.forEach();
  }

  public Iterable<SetterMethod> getSetters() {
    return setters.forEach();
  }

  public Iterable<ActionMethod> getActions() {
    return actions.forEach();
  }

  public boolean isC2sEnabled() {
    return c2sEnabled;
  }

  public boolean isS2cEnabled() {
    return s2cEnabled;
  }

  public boolean isC2sEncrypted() {
    return c2sEncrypted;
  }

  public boolean isS2cEncrypted() {
    return s2cEncrypted;
  }

  public boolean isObfuscated() {
    return obfuscated;
  }

  @Override
  public String toString() {
    return type+" "+name;
  }

  /**
   * @return -> c2sSerializer
   */
  public SerializationStrategy getC2sSerializer() {
    return c2sSerializer;
  }

  /**
   * @return -> s2cSerializer
   */
  public SerializationStrategy getS2cSerializer() {
    return s2cSerializer;
  }

  /**
   * @return -> persistenceStrategy
   */
  public PersistenceStrategy getPersistenceStrategy() {
    return persistenceStrategy;
  }

  /**
   * @return -> validators
   */
  @SuppressWarnings("unchecked")
  public Class<? extends ValidatesValue<?>>[] getValidators() {
    return validators.toArray(new Class[validators.size()]);
  }

  public boolean isListType() {
    return listType;
  }

  public void setListType(boolean listType) {
    this.listType = listType;
  }

  public boolean isMapType() {
    return mapType;
  }

  public void setMapType(boolean mapType) {
    this.mapType = mapType;
  }

  public boolean isRememberDefault() {
    return rememberDefault;
  }

  public void setRememberDefault(boolean rememberDefault) {
    this.rememberDefault = rememberDefault;
  }
}
