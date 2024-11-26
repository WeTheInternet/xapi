package xapi.components.api;

import xapi.fu.Do;
import xapi.fu.In1;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;

public class LoggingCallback <T, E extends Exception> implements Callback<T, E> {

	private final In1<T> callback;

	public LoggingCallback(In1<T> callback) {
		this.callback = callback;
		assert callback != null : "Do not send null callbacks to LoggingCallback";
  }

	@Override
	public void onFailure(E reason) {
		GWT.log(getFailureText(reason), reason);
	}

	protected String getFailureText(E reason) {
	  return "An unexpected exception occurred";
  }

	@Override
	public void onSuccess(T result) {
		callback.in(result);
	}

	public static Callback<Void, Exception> voidCallback(Do onLoaded) {
	  return new LoggingCallback<>(onLoaded.ignores1());
  }

}
