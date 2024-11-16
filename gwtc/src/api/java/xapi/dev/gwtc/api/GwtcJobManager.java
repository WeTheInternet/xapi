package xapi.dev.gwtc.api;

import xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage;
import xapi.fu.In2;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/4/17.
 */
public interface GwtcJobManager {
    void compileIfNecessary(GwtManifest manifest, In2<CompiledDirectory, Throwable> callback);

    void forceRecompile(GwtManifest manifest, In2<CompiledDirectory, Throwable> callback);

    void blockFor(String moduleName, long timeout, TimeUnit unit) throws TimeoutException;

    CompileMessage getStatus(String moduleName);

    GwtcJob getJob(String moduleName);

    void destroy(GwtcJob existing);
}
