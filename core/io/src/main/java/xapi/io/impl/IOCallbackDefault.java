package xapi.io.impl;

import xapi.io.api.IOCallback;

public class IOCallbackDefault <V> implements IOCallback<V>{

  @Override
  public void onSuccess(V t) {
    
  }

  @Override
  public void onError(Throwable e) {
    
  }

  @Override
  public void onCancel() {
    
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

}
