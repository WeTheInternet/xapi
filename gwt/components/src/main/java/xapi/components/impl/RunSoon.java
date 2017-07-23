package xapi.components.impl;

import elemental2.core.Function;
import elemental2.core.JsObject;
import elemental2.core.Reflect;
import elemental2.dom.DomGlobal;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import xapi.fu.Do;
import xapi.time.impl.RunOnce;

import com.google.gwt.core.client.JavaScriptObject;

/**
 *
 * An eager javascript scheduling mechanism,
 * which will run its tasks as soon in the event queue as possible,
 * ensuring all functions, including those from javascript,
 * will flush the GWT scheduler, ensuring normal operation of other schedulers.
 *
 * Do not send long-running, CPU-intensive tasks here,
 * as you might jank up your UI thread pretty badly.
 *
 * The primary use case for RunSoon is a way for code which might be
 * invoked outside of GWT runtime scope, like custom element constructors,
 * we want to be able to schedule something to run "as soon as absolutely possible".
 *
 * When running inside $entry (see {@link com.google.gwt.core.client.impl.Impl#entry(JavaScriptObject)},
 * we can use {@link com.google.gwt.core.client.Scheduler#scheduleFinally(com.google.gwt.core.client.Scheduler.ScheduledCommand)}
 * to get code to run "after current execution, but before returning to event loop".
 *
 * sheduleFinally will always run sooner than a RunSoon,
 * as RunSoon will run in the soonest fresh event loop;
 * thus, if you need to update the UI in time to prevent jank,
 * either you need to defer the mutation of the DOM until all tasks are complete,
 * or you need to stick to scheduleFinally, to finish all operations before giving the UI thread time to update.
 *
 * The best use case for this is when you want some raw javascript,
 * which may not be $entry wrapped, to be able to defer something with the shortest break possible,
 * without having already encapsulated the code with something that checks and runs tasks from a queue.
 *
 *
 * When running in raw javascript, if we can't pre-wrap some callback,
 * we can rely on other means to get back on the event loop before any other processing occurs
 * (faster than setTimeout; may or may not beat the UI thread);
 * a task using $wnd.runSoon(function(){...}) will run after GWT finalies,
 * but before any use of setTimeout / scheduleDeferred,
 * plus it will kindly flush the GWT event queue,
 * in case somebody else was forgetting to clear the finally stack
 * (that is, they ran some callback without a wrapper, and that callback
 * tried to use scheduleFinally to defer something, but it wasn't run,
 * or they tried to use scheduleDeferred, but the queue was not flushed)
 * TODO consider logging this as a visible / tolerable error state).
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/15/17.
 */

@JsType(isNative = true, name = "runSoon", namespace = JsPackage.GLOBAL)
public class RunSoon extends Function {

    @JsFunction
    interface RunLaterCallback {
        void execute();
    }

    public RunSoon() {
        // RunSoon is a function that lives in another window.
        // we will create it as such, natively...
        super("a",
            "if (typeof a === 'number') { " +
                "var was = this.tasksByHandle[a];" +
                "this.clearImmediate(a);" +
                "return !!was;" +
            "} else {" + // we expect a function then some arguments, so 1st arg will not be a number
                "return this.setImmediate(arguments);" +
            "}"
        );
    }

    @JsOverlay
    private static final RunSoon INSTANCE = installPolyfill(Js.uncheckedCast(DomGlobal.window));

    @JsOverlay
    public static RunSoon runner() {
        return INSTANCE;
    }

    // The actual native JS implementation we wire up in our polyfill
    @JsProperty(name = "setImmediate")
    public native Function setImmediate();
    @JsProperty(name = "runNow")
    public native Function runNow();
    @JsProperty(name = "clearImmediate")
    public native Function clearImmediate();
    @JsProperty
    public final native JsObject getTasksByHandle();

    @JsOverlay
    protected static RunSoon installPolyfill(JsObject global) {
        if (global.propertyIsEnumerable("runSoon")) {
            // already installed
            return (RunSoon) Reflect.get(global, "runSoon");
        }
        final RunSoon scheduler = RunSoonBinder.bindTo(global);
        assert scheduler.enableWatchdog();
        // Return the bound scheduler
        return scheduler;
    }

    @JsOverlay
    private boolean enableWatchdog() {
        // Called only from an assert; TODO: install a watchdog
        // who ensures that all tasks are being cleared, so we can
        // log error cases / user agents when there are any detectable issues
        return true;
    }

    @JsOverlay
    public final int doSchedule(Do task) {
        final RunLaterCallback once = RunOnce.runOnce(task)::done;
        final Object result = setImmediate().call(this, once);
        return Js.castToInt(result);
    }

    @JsOverlay
    public final boolean doCancel(int task) {
        boolean hadTask = getTasksByHandle().hasOwnProperty("" + task);
        clearImmediate().call(this, task);
        return hadTask;
    }

    @JsOverlay
    public final Object doFinish(int task) {
        return runNow().call(this, task);
    }

    @JsOverlay
    public static boolean cancel(int task) {
        return INSTANCE.doCancel(task);
    }

    @JsOverlay
    public static int schedule(Do task) {
        return INSTANCE.doSchedule(task);
    }

    @JsOverlay
    public static Object finish(int task) {
        return INSTANCE.doFinish(task);
    }

    @JsOverlay
    public final Function asFunction() {
        return Js.uncheckedCast(this);
    }

    @JsOverlay
    public final JsObject asObject() {
        return Js.uncheckedCast(this);
    }

    @JsOverlay
    public final void finishAll(int ... handles) {
        for (int handle : handles) {
            finish(handle);
        }

    }
    @JsOverlay
    public final void flush(boolean drain) {
        final JsObject tasks = getTasksByHandle();
        String[] names = JsObject.getOwnPropertyNames(tasks);
        do {
            for (String key : names) {
                doFinish(Integer.parseInt(key));
            }
        } while(drain && (names = JsObject.getOwnPropertyNames(tasks)).length > 0);
    }

}

class RunSoonBinder {

    /**
     * Adapted from https://raw.githubusercontent.com/YuzuJS/setImmediate/master/setImmediate.js
     * with modifications so we can manually flush the queue earlier than "sometime later",
     * and still safely run each task only once (we eagerly delete tasks before we run them).
     */
    static native RunSoon bindTo(JsObject global)
    /*-{
        "use strict";

        if (global.runSoon) {
          // already polyfilled
          return global.runSoon;
        }
        // If supported, we should attach to the prototype of global, since that is where setTimeout et al. live.
        var attachTo = $wnd.Object.getPrototypeOf && $wnd.Object.getPrototypeOf(global);
        attachTo = attachTo && attachTo.setTimeout ? attachTo : global;

        function runSoon(a) {
          if (typeof a === "number") {
            return clearImmediate(a);
          } else {
            return setImmediate.apply(this, arguments);
          }
        }

        attachTo.runSoon = runSoon;
        var nextHandle = 1; // Spec says greater than zero
        var tasksByHandle = attachTo.runSoon.tasksByHandle = {};
        var currentlyRunningATask = false;
        var doc = global.document;
        var registerImmediate;

        function setImmediate(callback) {
          // Callback can either be a function or a string
          if (typeof callback !== "function") {
            callback = new Function("" + callback);
          }
          // Copy function arguments
          var args = new Array(arguments.length - 1);
          for (var i = 0; i < args.length; i++) {
            args[i] = arguments[i + 1];
          }
          // Store and register the task, ensuring all scheduled tasks trigger GWT scheduler
          callback = @JsFunctionSupport::maybeEnter(*)(callback);
          var task = { callback: callback, args: args };
          tasksByHandle[nextHandle] = task;
          registerImmediate(nextHandle);
          return nextHandle++;
        }

        function clearImmediate(handle) {
          var wasQueued = !!tasksByHandle[handle];
          delete tasksByHandle[handle];
          return wasQueued;
        }

        function runNow(handle) {
          var task = tasksByHandle[handle];
          if (task) {
            delete tasksByHandle[handle];
            return run(task);
          }
        }

        function run(task) {
          var callback = task.callback;
          var args = task.args;
          switch (args.length) {
            case 0:
              return callback();
            case 1:
              return callback(args[0]);
            case 2:
              return callback(args[0], args[1]);
            case 3:
              return callback(args[0], args[1], args[2]);
            default:
              return callback.apply(undefined, args);
          }
        }

        function runIfPresent(handle) {
          // From the spec: "Wait until any invocations of this algorithm started before this one have completed."
          // So if we're currently running a task, we'll need to delay this invocation.
          if (currentlyRunningATask) {
            registerImmediate(handle);
          } else {
            var task = tasksByHandle[handle];
            if (task) {
              // we want to clear the task immediately
              clearImmediate(handle);
              currentlyRunningATask = true;
              try {
                run(task);
              } finally {
                currentlyRunningATask = false;
              }
            }
          }
        }

        function installNextTickImplementation() {
          // really not necessary since we aren't running in node, but, whatevs
          registerImmediate = function(handle) {
            process.nextTick(function () { runIfPresent(handle); });
          };
        }

        function installSetImmediateImplementation() {
          registerImmediate = function(handle) {
            global.setImmediate(runIfPresent, handle);
          };
        }

        function canUsePostMessage() {
          // The test against `importScripts` prevents this implementation from being installed inside a web worker,
          // where `global.postMessage` means something completely different and can't be used for this purpose.
          if (global.postMessage && !global.importScripts) {
            var postMessageIsAsynchronous = true;
            var oldOnMessage = global.onmessage;
            global.onmessage = function() {
              postMessageIsAsynchronous = false;
            };
            global.postMessage("", "*");
            global.onmessage = oldOnMessage;
            return postMessageIsAsynchronous;
          }
        }

        function installPostMessageImplementation() {
          // Installs an event handler on `global` for the `message` event: see
          // * https://developer.mozilla.org/en/DOM/window.postMessage
          // * http://www.whatwg.org/specs/web-apps/current-work/multipage/comms.html#crossDocumentMessages

          var messagePrefix = "setImmediate$" + Math.random() + "$";
          var onGlobalMessage = function(event) {
            if (event.source === global &&
              typeof event.data === "string" &&
              event.data.indexOf(messagePrefix) === 0) {
              runIfPresent(+event.data.slice(messagePrefix.length));
            }
          };

          if (global.addEventListener) {
            global.addEventListener("message", onGlobalMessage, false);
          } else {
            global.attachEvent("onmessage", onGlobalMessage);
          }

          registerImmediate = function(handle) {
            var scrp = 'postMessage("'+messagePrefix + handle + '", "*");';
            global.eval(scrp);
          };
        }

        function installMessageChannelImplementation() {
          var channel = new MessageChannel();
          channel.port1.onmessage = function(event) {
            var handle = event.data;
            runIfPresent(handle);
          };

          registerImmediate = function(handle) {
            channel.port2.postMessage(handle);
          };
        }

        function installReadyStateChangeImplementation() {
          var html = doc.documentElement;
          registerImmediate = function(handle) {
            // Create a <script> element; its readystatechange event will be fired asynchronously once it is inserted
            // into the document. Do so, thus queuing up the task. Remember to clean up once it's been called.
            var script = doc.createElement("script");
            script.onreadystatechange = function () {
              runIfPresent(handle);
              script.onreadystatechange = null;
              html.removeChild(script);
              script = null;
            };
            html.appendChild(script);
          };
        }

        function installSetTimeoutImplementation() {
          registerImmediate = function(handle) {
            setTimeout(runIfPresent, 0, handle);
          };
        }

        // We prefer process.nextTick as it runs sooner than setImmediate
        // (yes, yes...  their names suggest the opposite of the actual timing)
        if (global.process && global.process.nextTick) {
          // For Node.js before 0.9.  Also, net
          installNextTickImplementation();
        } else if (global.setImmediate) {
          // IE10+, or whatever polyfill client is using
          installSetImmediateImplementation();
        } else if (canUsePostMessage()) {
          // For non-IE10 modern browsers
          installPostMessageImplementation();
        } else if (global.MessageChannel) {
          // For web workers, where supported
          installMessageChannelImplementation();
        } else if (doc && "onreadystatechange" in doc.createElement("script")) {
          // For IE 6–8
          installReadyStateChangeImplementation();
        } else {
          // For older browsers
          installSetTimeoutImplementation();
        }
//
//        // for javascript, we'll expose a function, so you can just do window.runSoon(()=>...);
        // We are attempting to do this by extending Function and calling super with some script...
//        function runSoon(t) {
//          if (typeof t === "number") {
//            clearImmediate(t);
//          } else {
//            setImmediate(t);
//          }
//        }

        runSoon.runNow = runNow;
        runSoon.setImmediate = setImmediate;
        runSoon.clearImmediate = clearImmediate;

        return runSoon;
    }-*/;

}
