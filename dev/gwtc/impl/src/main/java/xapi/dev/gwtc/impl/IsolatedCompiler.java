package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcService;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.util.api.Destroyable;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/28/17.
 */
public class IsolatedCompiler implements Runnable, Destroyable {

    private final String[] args;
    private final GwtcJobMonitor remoteMonitor;
    private GwtcJobManagerImpl manager;

    public IsolatedCompiler(Object remoteMonitor, String[] args) {
        this.remoteMonitor = (GwtcJobMonitor) remoteMonitor;
        this.args = args;
    }

    @Override
    public void run() {
        GwtcService compiler = X_Inject.instance(GwtcService.class);
        manager = new GwtcJobManagerImpl(compiler);
        try {
            manager.runJob(remoteMonitor, args);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void destroy() {
        X_Log.info(IsolatedCompiler.class, "Destroying manager");
        if (manager != null) {
            manager.die(remoteMonitor);
        }
    }
}
