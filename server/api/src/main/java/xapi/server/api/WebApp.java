package xapi.server.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.Out1;
import xapi.model.api.Model;

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

    StringTo<Model> getTemplates();

    IntTo<Route> getRoute();

    boolean isRunning();

    WebApp setRunning(boolean running);

    boolean isDevMode();

    WebApp setDevMode(boolean devMode);

    int getPort();

    WebApp setPort(int port);

    String getSource();

    WebApp setSource(String source);

    default void shutdown() {
        setRunning(false);
    }

    default void start() {
        setRunning(true);
    }

}
