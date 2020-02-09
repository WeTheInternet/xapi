package xapi.server.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.Classpath;
import xapi.fu.Immutable;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.gwtc.api.CompiledDirectory;
import xapi.inject.X_Inject;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.PrimitiveSerializer;
import xapi.scope.request.RequestScope;
import xapi.scope.spi.RequestLike;
import xapi.server.api.Route.RouteType;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static java.io.File.separator;

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

    default In1Out1Unsafe<String,InputStream> getModuleLoader(RequestScope<?, ?> scope) {
        return mn -> {
            final RequestLike req = scope.getRequest();
            final String moduleName = req.getHeader("X-Gwt-Module", Out1.null1());
            final String moduleVersion = req.getHeader("X-Gwt-Version", Immutable.immutable1("xapi"));

            File f = new File(
                getContentRoot() + separator +
                    "WEB-INF" + separator +
                    "deploy" + separator +
                    mn + separator +
                    "XapiModelLinker",
                moduleVersion + ".rpc"
            );
            if (!f.exists()) {
                f = new File(f.getParentFile(), "xapi.rpc"); // default "HEAD" file
            }
            if (!f.exists()) {
                // need to actually get the gwt compile from the server.
                XapiServer server = scope.get(XapiServer.class);
                final ModelGwtc gwt = server.getWebApp().getGwtModules().getMaybe(moduleName)
                    .ifAbsentThrow(() -> new IllegalStateException("No such Gwt module " + moduleName))
                    .get();
                Mutable<CompiledDirectory> directory = new Mutable<>();
                gwt.getCompiledDirectory((dir, error) -> {
                    directory.set(dir);
                });
                String deployDir = directory.block().getDeployDir();
                f = new File(deployDir + separator + mn + separator + "XapiModelLinker", moduleVersion + ".rpc");
                if (!f.exists()) {
                    f = new File(f.getParentFile(), "xapi.rpc"); // default "HEAD" file
                }
            }
            return new FileInputStream(f);
        };
    }


    StringTo<Boolean> getAbsolutes();
    WebApp setAbsolutes(StringTo<Boolean> absolutes);
    default StringTo<Boolean> getOrCreateAllowAbsolutes() {
        return getOrCreate(this::getAbsolutes, Out1.out1Deferred(X_Collect::newStringMap, Boolean.class), this::setAbsolutes);
    }

    default void allowAbsolute(String payload, Boolean allowAbsolute) {
        getOrCreateAllowAbsolutes().put(payload, allowAbsolute);

    }
    default boolean allowAbsolute(String payload) {
        final StringTo<Boolean> map = getOrCreateAllowAbsolutes();
        final String[] segments = payload.split("/");
        ChainBuilder<String> path = Chain.startChain();
        for (int l = 0; l < segments.length; l++ ) {
            path.add(segments[l]);
            String test = path.join("/");
            if (map.has(test)) {
                return map.get(test);
            }
        }
        return false;
    }
}
class WebAppIdHolder {
    static int lastId;
    static PrimitiveSerializer serializer = X_Inject.instance(PrimitiveSerializer.class);

    public static String newId() {
        return serializer.serializeLong(lastId++);
    }
}
