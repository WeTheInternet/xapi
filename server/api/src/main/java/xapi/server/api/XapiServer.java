package xapi.server.api;

import xapi.fu.Do;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.In3.In3Unsafe;
import xapi.fu.Mutable;
import xapi.scope.request.RequestLike;
import xapi.scope.request.RequestScope;
import xapi.scope.request.ResponseLike;
import xapi.scope.request.SessionScope;

import java.util.concurrent.locks.LockSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public interface XapiServer <Request extends RequestScope> {

    void inScope(In1Out1<SessionScope, Request> scopeFactory, In3Unsafe<Request, Throwable, Do> callback);

    void start(Do onDone);

    void shutdown(Do onDone);

    void serviceRequest(Request request, In2<Request, Throwable> callback);

    WebApp getWebApp();

    void writeText(Request request, String payload, In2<Request, Throwable> callback);

    void writeFile(Request request, String payload, In2<Request, Throwable>  callback);

    void writeTemplate(Request request, String payload, In2<Request, Throwable>  callback);

    void writeGwtJs(Request request, String payload, In2<Request, Throwable>  callback);

    void writeCallback(Request request, String payload, In2<Request, Throwable>  callback);

    void writeService(String path, Request request, String payload, In2<Request, Throwable>  callback);

    void registerEndpoint(String name, XapiEndpoint<Request> endpoint);

    void registerEndpointFactory(String name, boolean singleton, In1Out1<String, XapiEndpoint<Request>> endpoint);

    default boolean blockFor(Request request, Mutable<Boolean> success, int millis) {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            if (success.out1() != null) {
                return success.out1();
            }
            LockSupport.parkNanos(5_000_000);
        }
        return false;
    }

    /**
     * Called from the thread blocking on the running server.
     *
     * This is an important distinction with regard to cleaning up particular threads.
     * You should probably avoid overriding this unless you know what you are doing...
     */
    void onRelease();
}
