package xapi.components.impl;

import elemental.dom.Element;
import xapi.components.api.OnWebComponentAttributeChanged;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.In3;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.function.Consumer;

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

	public static native JavaScriptObject wrapDo(Do task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    task.@xapi.fu.Do::done()();
	  });
	 }-*/;

  	public static native <T> JavaScriptObject wrapConsumer(Consumer<T> task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    task.@java.util.function.Consumer::accept(Ljava/lang/Object;)(arguments[0]);
	  });
	 }-*/;

	public static native <T> JavaScriptObject wrapIn1(In1<T> task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    task.@In1::in(Ljava/lang/Object;)(arguments[0]);
	  });
	 }-*/;

	@SuppressWarnings("rawtypes")
	public static native <T> JavaScriptObject wrapConsumerOfThis(Consumer<T> task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    task.@java.util.function.Consumer::accept(Ljava/lang/Object;)(this);
	  });
	 }-*/;

	public static native <T> JavaScriptObject wrapInputOfThis(In1<T> task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    task.@In1::in(Ljava/lang/Object;)(this);
	  });
	 }-*/;

	public static native JavaScriptObject wrapInputOfThis(In2<?, ?> task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    task.@In2::in(Ljava/lang/Object;Ljava/lang/Object;)(this, this);
	  });
	 }-*/;

	public static native <T extends Element> JavaScriptObject wrapWebComponentChangeHandler(OnWebComponentAttributeChanged<T> task)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(name, oldVal, newVal){
	    task.@xapi.components.api.OnWebComponentAttributeChanged::onAttributeChanged(*)(this, name, oldVal, newVal);
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

	public static native JavaScriptObject wrapInput(In1<Object[]> value)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    var jsArr = $wnd.Array.prototype.slice.call(arguments);
	    var javaArr = @xapi.fu.X_Fu::newObjectArray(I)(jsArr.length);
	    for (var i = jsArr.length;i-->0;) {
	      javaArr[i] = jsArr[i];
	    }
	    value.@In1::in(Ljava/lang/Object;)(javaArr);
	  });
	}-*/;

	public static native JavaScriptObject wrapInput(In2<?, Object[]> value)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    var jsArr = $wnd.Array.prototype.slice.call(arguments);
	    var javaArr = @xapi.fu.X_Fu::newObjectArray(I)(jsArr.length);
	    for (var i = jsArr.length;i-->0;) {
	      javaArr[i] = jsArr[i];
	    }
	    value.@In2::in(Ljava/lang/Object;Ljava/lang/Object;)(this, javaArr);
	  });
	}-*/;

	public static native JavaScriptObject wrapInput(In3<?, ?, Object[]> value)
	/*-{
	  return @JsFunctionSupport::maybeEnter(*)(function(){
	    var jsArr = $wnd.Array.prototype.slice.call(arguments);
	    var javaArr = @xapi.fu.X_Fu::newObjectArray(I)(jsArr.length);
	    for (var i = jsArr.length;i-->0;) {
	      javaArr[i] = jsArr[i];
	    }
	    value.@In3::in(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)(this, this, javaArr);
	  });
	}-*/;
}
