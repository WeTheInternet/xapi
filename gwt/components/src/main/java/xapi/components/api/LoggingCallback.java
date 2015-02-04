package xapi.components.api;

import java.util.function.Consumer;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class LoggingCallback <T, E extends Exception> implements Callback<T, E> {

	private final Consumer<T> callback;

	public LoggingCallback(Consumer<T> callback) {
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
		callback.accept(result);
	}

	public static Callback<Void, Exception> voidCallback(ScheduledCommand onLoaded) {
	  return new LoggingCallback<>((v)->onLoaded.execute());
  }

}
