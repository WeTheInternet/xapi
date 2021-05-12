package xapi.io.impl;

import xapi.io.api.IOCallback;
import xapi.debug.X_Debug;

public class IOCallbackDefault <V> implements IOCallback<V>{

  @Override
  public void onSuccess(V t) {
    
  }

  @Override
  public void onError(Throwable e) {
    X_Debug.rethrow(e);
  }

  @Override
  public void onCancel() {
    
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

}
