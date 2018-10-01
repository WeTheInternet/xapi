package xapi.server.api;

import xapi.fu.In2;
import xapi.scope.request.RequestScope;
import xapi.time.X_Time;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/28/18 @ 12:57 AM.
 */
public interface CrudEndpoint <Req extends RequestScope> extends XapiEndpoint <Req> {

    @Override
    default void serviceRequest(String path, Req req, String payload, In2<Req, Throwable> callback) {
        // Hm.... consider automatically getting off-thread?
        X_Time.runLater(()->{
            switch (req.getMethod().toUpperCase()) {
                case "GET":
                    doGet(path, req, payload, callback);
                    break;
                case "POST":
                    doPost(path, req, payload, callback);
                    break;
                case "PUT":
                    doPut(path, req, payload, callback);
                    break;
                case "DELETE":
                    doDelete(path, req, payload, callback);
                    break;
                default:
                    callback.in(req, unsupported(req, path));
            }
        });
    }

    default void doDelete(String path, Req req, String payload, In2<Req, Throwable> callback) {
        callback.in(req, unsupported(req, path));
    }
    default void doGet(String path, Req req, String payload, In2<Req, Throwable> callback) {
        callback.in(req, unsupported(req, path));
    }
    default void doPost(String path, Req req, String payload, In2<Req, Throwable> callback) {
        callback.in(req, unsupported(req, path));
    }
    default void doPut(String path, Req req, String payload, In2<Req, Throwable> callback) {
        callback.in(req, unsupported(req, path));
    }

    default Throwable unsupported(Req req, String path) {
        return new UnsupportedOperationException(req.getMethod() + " not supported.");
    }
}
