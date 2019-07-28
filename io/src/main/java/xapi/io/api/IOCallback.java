package xapi.io.api;

import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

public interface IOCallback <V> extends SuccessHandler<V>, ErrorHandler<Throwable>{

  void onCancel();

  boolean isCancelled();

  @Override
  void onError(final Throwable e);

}
