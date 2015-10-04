package xapi.components.impl;

import java.util.function.Consumer;

import com.google.gwt.core.client.JavaScriptObject;

import xapi.components.api.OnWebComponentAttributeChanged;

public class JsFunctionSupport {

    public static native JavaScriptObject maybeEnter(JavaScriptObject func)
    /*-{
      return function() {
        if (@com.google.gwt.core.client.impl.Impl::entryDepth) {
            // we are already inside the gwt call stack, just skip the expensive wrapping.
            return func.apply(this, arguments) || null;
        } else {
            return @com.google.gwt.core.client.impl.Impl::entry0(*)(func, this, arguments) || null;
        }
      };
    }-*/;

	public static native JavaScriptObject wrapRunnable(Runnable task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    task.@java.lang.Runnable::run()();
	  });
	 }-*/;

  public static native <T> JavaScriptObject wrapConsumer(Consumer<T> task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    task.@java.util.function.Consumer::accept(Ljava/lang/Object;)(arguments[0]);
	  });
	 }-*/;

	@SuppressWarnings("rawtypes")
	public static native JavaScriptObject wrapConsumerOfThis(Consumer task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    task.@java.util.function.Consumer::accept(Ljava/lang/Object;)(this);
	  });
	 }-*/;

	public static native JavaScriptObject wrapWebComponentChangeHandler(OnWebComponentAttributeChanged task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(name, oldVal, newVal){
	    task.@xapi.components.api.OnWebComponentAttributeChanged::onAttributeChanged(*)(name, oldVal, newVal);
	  });
	 }-*/;

	public static <E> Consumer<E> mergeConsumer(Consumer<E> first, Consumer<E> second) {
	  return e -> {
	    first.accept(e);
	    second.accept(e);
	  };
	}

	public static native JavaScriptObject merge(JavaScriptObject first, JavaScriptObject second)
	/*-{
	  return function() {
	  	first.apply(this, arguments);
	  	second.apply(this, arguments);
	  };
  }-*/;
}
