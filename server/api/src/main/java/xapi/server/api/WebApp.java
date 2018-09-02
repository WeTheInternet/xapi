package xapi.server.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.Classpath;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.PrimitiveSerializer;
import xapi.server.api.Route.RouteType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public interface WebApp extends Model {

    StringTo<Classpath> getClasspaths();

    WebApp setClasspaths(StringTo<Classpath> classpaths);

    default WebApp addClasspath(String name, Classpath path) {
        StringTo<Classpath> paths = getClasspaths();
        if (paths == null) {
            paths = X_Collect.newStringMap(Classpath.class);
            setClasspaths(paths);
        }
        paths.put(name, path);
        return this;
    }

    StringTo<ModelGwtc> getGwtModules();

    WebApp setGwtModules(StringTo<ModelGwtc> modules);

    default StringTo<ModelGwtc> getOrCreateGwtModules() {
        return getOrCreate(this::getGwtModules, Out1.out1Deferred(X_Collect::newStringMap, ModelGwtc.class), this::setGwtModules);
    }

    default ModelGwtc getGwtModule(String moduleName) {
        return getOrCreateGwtModules()
            .getOrCreate(moduleName, m-> initGwtc(X_Model.create(ModelGwtc.class)));
    }

    default ModelGwtc initGwtc(ModelGwtc modelGwtc) {
        return modelGwtc;
    }

    StringTo<Model> getTemplates();

    IntTo<Route> getRoute();

    boolean isClustered();

    WebApp setClustered(boolean clustered);

    boolean isDestroyed();

    WebApp setDestroyed(boolean destroyed);

    boolean isRunning();

    WebApp setRunning(boolean running);

    boolean isDestroyable();

    WebApp setDestroyable(boolean destroyable);

    boolean isDevMode();

    WebApp setDevMode(boolean devMode);

    int getPort();

    WebApp setPort(int port);

    String getBaseSource();

    WebApp setBaseSource(String source);

    default void shutdown() {
        setRunning(false);
    }

    default void start() {
        setRunning(true);
    }

    void setContentRoot(String root);

    String getContentRoot();

    String getInstanceId();

    /**
     * @return Gets or returns a new instanceId based upon the rather primitive default of
     * "serialize a static incremented long".
     *
     */
    default String instanceId() {
        return getOrMakeInstanceId(WebAppIdHolder::newId);
    }

    /**
     * @param newId A factory for a new Id only used if getInstanceId() returns null
     * @return The current or a new instanceId for this WebApp.
     *
     * Note that serializing WebApp w/out an instanceId would force rehydrated instances to initialize new ids
     */
    default String getOrMakeInstanceId(Out1<String> newId) {
        String instanceId = getInstanceId();
        if (instanceId == null) {
            synchronized (this) {
                // double-checked lock is about as good as default method going to get;
                // TODO: have a utility method for locking that checks for a HasLock interface...
                instanceId = getInstanceId();
                if (instanceId == null) {
                    instanceId = newId == null ? WebAppIdHolder.newId() : newId.out1();
                    setInstanceId(instanceId);
                }
            }
        }
        return instanceId;
    }

    void setInstanceId(String instanceId);

    default void destroy() {
        if (isRunning()) {
            setRunning(false);
        }
        if (isDestroyable()) {
            setDestroyed(true);
        }
    }

    default void addReroute(String from, String to) {
        Route testRoute = X_Model.create(Route.class);
        testRoute.setPath(from);
        testRoute.setRouteType(RouteType.Reroute);
        testRoute.setPayload(to);
        getRoute().add(testRoute);
    }
}
class WebAppIdHolder {
    static int lastId;
    static PrimitiveSerializer serializer = X_Inject.instance(PrimitiveSerializer.class);

    public static String newId() {
        return serializer.serializeLong(lastId++);
    }
}
