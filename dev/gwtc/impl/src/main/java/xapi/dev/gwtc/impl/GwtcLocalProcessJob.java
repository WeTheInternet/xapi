package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.gwtc.api.GwtManifest;

import java.net.URLClassLoader;

/**
 * A Gwtc job that runs in the current JVM.  It should be launched in a (preferable) isolated classloader,
 * and will communicate across the classworlds using a reflection-based {@link GwtcJobMonitorImpl}
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public class GwtcLocalProcessJob extends GwtcJob {

    private final GwtcJobMonitor monitor;

    public GwtcLocalProcessJob(GwtManifest manifest, URLClassLoader classpath, String gwtHome) {
        super(manifest);
        this.monitor = GwtcJobMonitorImpl.newMonitor(classpath);
    }

    @Override
    public GwtcJobMonitor getMonitor() {
        return monitor;
    }
}
