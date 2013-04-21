package xapi.collect.impl;

import xapi.util.X_Runtime;
import xapi.util.X_Util;
import xapi.util.api.ConvertsValue;
import xapi.util.api.Pair;

public class AbstractMultiInitMap <Key, Value, Params> extends InitMapDefault<Pair<Key, Params>,Value> {

  private transient volatile Params params;

  private boolean clearState;


  protected static <Key, Value> ConvertsValue<Pair<Key, Value>, String> adapt(final ConvertsValue<Key, String> keyConverter) {
    return new ConvertsValue<Pair<Key,Value>,String>() {
      @Override
      public String convert(Pair<Key,Value> from) {
        return keyConverter.convert(from.get0());
      }
    };
  }

  @SuppressWarnings("unchecked") //ALWAYS_NULL is erased to object; anything is safe
  public AbstractMultiInitMap(ConvertsValue<Key, String> keyConverter) {
    super(AbstractMultiInitMap.<Key, Params>adapt(keyConverter), ALWAYS_NULL);
    clearState = true;
  }
  public AbstractMultiInitMap(ConvertsValue<Key, String> keyConverter, ConvertsValue<Pair<Key,Params>,Value> valueConverter) {
    super(AbstractMultiInitMap.<Key, Params>adapt(keyConverter), valueConverter);
    clearState = true;
  }

  public Value get(Key key, Params params) {
    if (X_Runtime.isMultithreaded()) {
      synchronized (getLock(key)) {
        return doGet(key, params);
      }
    }else {
      return doGet(key, params);
    }
  }

  private Value doGet(Key key, Params params) {
    this.params = params;
    try {
      return super.get(X_Util.pairOf(key, params));
    }finally {
      if (isClearState())
        this.params = null;
    }
  }

  protected boolean isClearState() {
    return clearState;
  }

  public void setClearState(boolean clearImmediately) {
    this.clearState = clearImmediately;
  }

  @Override
  public final Value initialize(Pair<Key, Params> params) {
    Value value = null;
    try {
      value = valueProvider.convert(params);
    } catch (Throwable e) {
      logInitError(params, e);
    }
    if (value == null)
      value = initialize(params.get0(), params.get1() == null ? defaultParams() : params.get1());
    return value;
  }

  protected void logInitError(Pair<Key,Params> params, Throwable e) {
    // Can't use X_Log, as this class is used for run-once semantics in our injectors.
    System.err.println("Init error in "+getClass().getName()+" for "+params);
    if (X_Runtime.isDebug()) {
      while (e != null) {
        e.printStackTrace();
        e = e.getCause();
      }
    }
  }

  protected Params defaultParams() {
    return params;
  }

  protected Value initialize(Key key, Params params) {
    return null;
  }

}
