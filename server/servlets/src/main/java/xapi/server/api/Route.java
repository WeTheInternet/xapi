package xapi.server.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.In2;
import xapi.fu.log.Log;
import xapi.fu.log.Log.LogLevel;
import xapi.model.api.Model;
import xapi.scope.api.HasRequestContext;
import xapi.scope.request.RequestScope;
import xapi.scope.spi.RequestLike;
import xapi.scope.spi.ResponseLike;
import xapi.source.write.Template;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public interface Route extends Model {
    String[] DEFAULT_TEMPLATE_KEYS = {
        "$static", "$user", "$api",
        // TODO: actually have proper nested variable fields
        "$user.name", "$user.id", "$user.pageId"
    };

    enum RouteType {
        Text, Gwt {
            @Override
            public boolean isBlocking() {
                return true;
            }
        }, Callback, File, Directory, Template, Service, Reroute;

        public boolean isBlocking() {
            return false;
        }
    }

    default <Req extends HasRequestContext> void serveWithContext(String path, Req request, In2<Req, Throwable> callback) {

        if (!validate(path, request, callback)) {
            return;
        }
        XapiServer server = request.getContext().getScope().get(XapiServer.class);
        final String payload = getPayload();
        switch (getRouteType()) {
            case Reroute:
                server.reroute(request, payload, callback);
                return;
        }
        callback.in(request, new NotConfiguredCorrectly("Type not handled: " + getRouteType()));
    }

    default <Req> boolean validate(String path, Req request, In2<Req, Throwable> callback) {
        RouteType type = getRouteType();
        if (type == null) {
            getOrCreateLog().log(getClass(), LogLevel.WARN,
                "No route type specified; bailing ", this);
            callback.in(request, new NotConfiguredCorrectly("No route type in " + this));
            return false;
        }
        final String payload = getPayload();
        if (payload == null && type != RouteType.Template) {
            getOrCreateLog().log(getClass(), LogLevel.WARN,
                "No payload specified; bailing ", this);
            callback.in(request, new NotConfiguredCorrectly("No payload in " + this));
            return false;
        }
        return true;
    }

    default <Req extends RequestLike, Resp extends ResponseLike> void serve(String path, RequestScope<Req, Resp> request, In2<RequestScope<Req, Resp>, Throwable> callback) {
        if (!validate(path, request, callback)) {
            return;
        }
        XapiServer server = request.get(XapiServer.class);
        final String payload = getPayload();
        switch (getRouteType()) {
            case Text:
                server.writeText(request, payload, callback);
                return;
            case Gwt:
                server.writeGwtJs(request, payload, callback);
                return;
            case Callback:
                server.writeCallback(request, payload, callback);
                return;
            case File:
                server.writeFile(request, payload, callback);
                return;
            case Directory:
                server.writeDirectory(request, payload, callback);
                return;
            case Template:
                server.writeTemplate(request, payload, callback);
                return;
            case Service:
                server.writeService(path, request, payload, callback);
                return;
        }
        callback.in(request, new NotConfiguredCorrectly("Type not handled: " + getRouteType()));
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

    Template getTemplate();

    default Template getOrCreateTemplate() {
        Template template = getTemplate();
        if (template == null) {
            synchronized (this) {
                template = getTemplate();
                if (template == null) {
                    String payload = getPayload();
                    template = new Template(payload, DEFAULT_TEMPLATE_KEYS);
                    setTemplate(template);
                }
            }
        }
        return template;
    }

    Route setTemplate(Template template);

    String getPath();

    Route setPath(String path);

    IntTo<String> getMethods();

    default Route setMethods(String ... path) {
        return setMethods(X_Collect.asList(path));
    }

    Route setMethods(IntTo<String> path);

    default IntTo<String> methods() {
        return getOrCreateList(String.class, this::getMethods, this::setMethods);
    }

    default double matches(String url) {
        final String path = getPath();
        if (url.equals(path)) {
            return 1.;
        }
        // Do some pattern matching here...
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
                        // need to properly test...  repo/** matching repo/net
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
                    if (yourParts.length <= yourInd) {
                        if (myInd == myParts.length-1) {
                            return score / 10.;
                        } else {
                            return 0.;
                        }
                    }
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
