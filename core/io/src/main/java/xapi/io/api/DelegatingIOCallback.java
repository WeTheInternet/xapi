/**
 *
 */
package xapi.io.api;

import xapi.util.X_Debug;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class DelegatingIOCallback <V> implements IOCallback<V>{

  private SuccessHandler<V> success;
  private ErrorHandler<Throwable> failure;
  private boolean cancel;

  public DelegatingIOCallback(final SuccessHandler<V> success) {
    this.success = success;
  }

  public DelegatingIOCallback(final SuccessHandler<V> success, final ErrorHandler<Throwable> failure) {
    this.success = success;
    this.failure = failure;
  }

  public static ErrorHandler<Throwable> failHandler(SuccessHandler<?> callback) {
    if (callback instanceof ErrorHandler) {
      return (ErrorHandler)callback;
    }
    return new ErrorHandler<Throwable>() {
      @Override
      public void onError(Throwable e) {
        X_Debug.rethrow(e);
      }
    };
  }

  @Override
  public void onCancel() {
    success = null;
    failure = null;
    cancel = true;
  }

  @Override
  public boolean isCancelled() {
    return cancel;
  }

  @Override
  public void onError(final Throwable e) {
    if (failure != null) {
      failure.onError(e);
    } else {
      X_Debug.rethrow(e);
    }
  }

  @Override
  public void onSuccess(final V t) {
    if (success != null) {
      success.onSuccess(t);
    }
  }

}
