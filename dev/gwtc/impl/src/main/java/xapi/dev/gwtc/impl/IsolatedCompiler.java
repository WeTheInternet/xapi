package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcService;
import xapi.inject.X_Inject;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/28/17.
 */
public class IsolatedCompiler implements Runnable {

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
            manager.parseArgs(remoteMonitor, args);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
