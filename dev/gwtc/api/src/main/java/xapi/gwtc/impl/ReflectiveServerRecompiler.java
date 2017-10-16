package xapi.gwtc.impl;

import xapi.fu.In1;
import xapi.gwtc.api.IsRecompiler;
import xapi.gwtc.api.ServerRecompiler;
import xapi.log.X_Log;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/10/17.
 */
public class ReflectiveServerRecompiler implements ServerRecompiler {

    private final Object foreignRecompiler;

    public ReflectiveServerRecompiler(Object foreignRecompiler) {
        this.foreignRecompiler = foreignRecompiler;
    }

    @Override
    public void useServer(In1<IsRecompiler> callback) {
        try {
            callback.getClass().getMethod("in", Object.class).invoke(callback,
                proxyCompiler(callback));
        } catch (IllegalAccessException e) {
            X_Log.error(ReflectiveServerRecompiler.class, "Not allowed to use reflection", e);
        } catch (InvocationTargetException e) {
            X_Log.error(ReflectiveServerRecompiler.class, "Failure in callback", e);
        } catch (NoSuchMethodException e) {
            X_Log.error(ReflectiveServerRecompiler.class, "Cannot find In1.in?", e);
        }
    }

    private Object proxyCompiler(In1<IsRecompiler> callback) {
        return new ReflectiveRecompiler(callback);
    }
}
