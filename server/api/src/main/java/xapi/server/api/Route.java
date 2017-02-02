package xapi.server.api;

import xapi.fu.In1;
import xapi.fu.Log;
import xapi.fu.Log.LogLevel;
import xapi.model.api.Model;
import xapi.scope.api.RequestScope;
import xapi.util.api.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public interface Route extends Model {

    enum RouteType {
        Text, Gwt, Callback, File
    }

    default <Req extends RequestLike> boolean serve(RequestScope<Req> request, In1<Req> callback) {
        final String payload = getPayload();
        if (payload == null) {
            getOrCreateLog().log(getClass(), LogLevel.WARN,
                "No payload specified; bailing ", this);
            return false;
        }
        RouteType type = getRouteType();
        if (type == null) {
            getOrCreateLog().log(getClass(), LogLevel.WARN,
                "No route type specified; bailing ", this);
            return false;
        }
        XapiServer server = request.get(XapiServer.class);
        switch (type) {
            case Text:
                server.writeText(request, payload, callback);
                return true;
            case Gwt:
                server.writeGwtJs(request, payload, callback);
                return true;
            case Callback:
                server.writeCallback(request, payload, callback);
                return true;
            case File:
                server.writeFile(request, payload, callback);
                return true;

        }
        return false;
    }

    Log getLog();

    Route setLog(Log log);

    default Log getOrCreateLog() {
        return getOrCreate(this::getLog, Log::defaultLogger, this::setLog);
    }

    RouteType getRouteType();

    Route setRouteType(RouteType type);

    String getPayload();

    Route setPayload(String payload);

    String getPath();

    Route setPath(String path);

    default double matches(String url) {
        final String path = getPath();
        if (url.equals(path)) {
            return 1.;
        }
        // Do some patter matching here...
        if (path.contains("*")) {
            // If there is a *, we will return a slightly lower score
            double score = 0.8;
            final String[] myParts = path.split("/");
            final String[] yourParts = url.split("/");
            int myInd = 0, yourInd = 0;
            while (myInd < myParts.length) {
                String mine = myParts[myInd];
                if (mine.contains("*")) {
                    if (mine.equals("**")) {
                        score = score / 8.;
                        if (yourInd >= yourParts.length) {
                            return score;
                        }
                        if (myInd != myParts.length-1) {
                            String next = myParts[myInd+1];
                            String yours;
                            do {
                                if (yourInd == yourParts.length) {
                                    return 0; // failed on text trailing the **
                                }
                                yours = yourParts[yourInd++];
                            } while (!pathEquals(next, yours));
                            myInd++;
                            yourInd--;
                        }
                    } else {
                        if (yourInd >= yourParts.length) {
                            return 0.;
                        }
                        final String yours = yourParts[yourInd];
                        if (!pathEquals(mine, yours)) {
                            return 0.;
                        }
                        score = score / 2.;
                        myInd++;
                        yourInd++;
                    }
                } else {
                    final String yours = yourParts[yourInd];
                    if (!mine.equals(yours)) {
                        return 0.;
                    }
                    myInd++;
                    yourInd++;
                }
            }
            return score;
        }
        return 0.;
    }

    default boolean pathEquals(String next, String yours) {
        if (next.contains("*")) {
            return yours.matches(next.replaceAll("[*]", ".*"));
        } else {
            return next.equals(yours);
        }
    }

}
