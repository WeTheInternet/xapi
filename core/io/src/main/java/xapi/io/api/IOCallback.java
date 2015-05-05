package xapi.io.api;

import xapi.log.X_Log;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

public interface IOCallback <V> extends SuccessHandler<V>, ErrorHandler<Throwable>{

  default void onCancel() {}

  default boolean isCancelled() {
    return false;
  }

  @Override
  default void onError(final Throwable e) {
    X_Log.error("IOCallback error", this, e);
  }

}
