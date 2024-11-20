package com.google.gwt.dev.codeserver;

import xapi.dev.gwtc.api.IsAppSpace;

import com.google.gwt.dev.MinimalRebuildCacheManager;
import com.google.gwt.dev.codeserver.JobEvent.Status;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/20/17.
 */
public class RecompileRunner extends JobRunner {

    private final GwtcEventTable table;
    private final String moduleName;
    private final IsAppSpace app;

    public RecompileRunner(
        MinimalRebuildCacheManager minimalRebuildCacheManager,
        String moduleName,
        IsAppSpace app
    ) {
        this(new GwtcEventTable(), minimalRebuildCacheManager, moduleName, app);
    }

    public RecompileRunner(
        GwtcEventTable table,
        MinimalRebuildCacheManager minimalRebuildCacheManager,
        String moduleName,
        IsAppSpace app
    ) {
        super(table, minimalRebuildCacheManager);
        this.table = table;
        this.moduleName = moduleName;
        this.app = app;
    }

    public IsAppSpace getApp() {
        return app;
    }

    public String getModuleName() {
        return moduleName;
    }

    public GwtcEventTable getTable() {
        return table;
    }

    public boolean isRunning() {
        final JobEvent job = getTable().getCompilingJobEvent();
        if (job == null) {
            return false;
        }
        return job.getStatus() == Status.COMPILING;
    }

    public void waitForStateChange() throws InterruptedException {
        synchronized (table.progressListeners) {
            table.progressListeners.wait();
        }
    }
}
