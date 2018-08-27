package xapi.server.api;

import xapi.collect.api.InitMap;
import xapi.collect.impl.InitMapDefault;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.scope.api.Scope;
import xapi.scope.request.RequestScope;

import java.util.ServiceLoader;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 8/15/18 @ 4:41 AM.
 */
public abstract class AbstractXapiServer <Request extends RequestScope> implements XapiServer<Request> {

    private final InitMap<String, XapiEndpoint<Request>> endpoints;

    protected AbstractXapiServer() {
        endpoints = new InitMapDefault<>(
            In1Out1.identity(), this::findEndpoint
        );
    }

    @SuppressWarnings("unchecked")
    protected XapiEndpoint<Request> findEndpoint(String name) {
        // this is called during the init process of the underlying map,
        // but we made it protected so it could be overloaded, and that
        // means still want to route all endpoint loading through the caching map.
        if (endpoints.containsKey(name)) {
            return endpoints.get(name);
        }
        try {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final Class<?> cls = cl.loadClass(name);
            // once we have the class, lets inject it...
            Object inst;
            try {
                inst = X_Inject.singleton(cls);
                if (inst == null) {
                    throw new NullPointerException();
                }
                assert XapiEndpoint.class.isInstance(inst) : "Injection result of " + name + ", " + inst.getClass() + " is not a XapiEndpoint" +
                    " (or there is something nefarious happening with your classloader)";
                final XapiEndpoint<Request> endpoint = (XapiEndpoint) inst;
                // we'll cache singletons
                endpoints.put(name, endpoint);
                return endpoint;
            } catch (RuntimeException ignored) {
                // no dice... can we inject an instance?
                try {
                    inst = X_Inject.instance(cls);
                    // huzzah!  we can create instances.  In this case, we'll return the one we just created,
                    // plus create a simple delegate which knows to inject a new endpoint per invocation.
                    final Class<XapiEndpoint<?>> c = Class.class.cast(cls);
                    endpoints.put(name, (path, requestLike, payload, callback) -> {
                        XapiEndpoint realInst = X_Inject.instance(c);
                        realInst.serviceRequest(path, requestLike, payload, callback);
                    });
                    assert XapiEndpoint.class.isInstance(inst) : "Injection result of " + name + ", " + inst.getClass() + " is not a XapiEndpoint" +
                        " (or there is something nefarious happening with your classloader)";
                    return (XapiEndpoint<Request>) inst;
                } catch (RuntimeException stillIgnored) {
                    // STILL no dice... try service loader then give up.
                    for (Object result : ServiceLoader.load(cls, cl)) {
                        // if you loaded through service loader, you are going to be static, and can worry about
                        // state management yourself.
                        // TODO: test some things that use service loader to see what standards are, maybe revise above assumption
                        if (XapiEndpoint.class.isInstance(result)) {
                            final XapiEndpoint<Request> endpoint = (XapiEndpoint) result;
                            endpoints.put(name, endpoint);
                            return endpoint;
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new NotConfiguredCorrectly("Could not load endpoint named " + name);
        }
        return null;
    }


    @Override
    public void registerEndpoint(String name, XapiEndpoint<Request> endpoint) {
        endpoints.put(name, endpoint);
        //
    }

    @Override
    public void registerEndpointFactory(String name, boolean singleton, In1Out1<String, XapiEndpoint<Request>> endpoint) {
        synchronized (endpoints) {
            endpoints.put(name, (path, requestLike, payload, callback) -> {
                final XapiEndpoint realEndpoint = endpoint.io(name);
                if (singleton) {
                    endpoints.put(name, realEndpoint);
                }
                realEndpoint.initialize(requestLike, AbstractXapiServer.this);
                realEndpoint.serviceRequest(path, requestLike, payload, callback);
            });
        }
    }

    protected void initializeEndpoints(Scope scope) {
        X_Log.info(AbstractXapiServer.class, "Installing endpoints into " + scope);
        endpoints.forEach(this::initializeEndpoint, scope);
    }

    protected void initializeEndpoint(String path, XapiEndpoint<Request> endpoint, Scope scope) {
        endpoint.initialize(scope, this);
    }
}
