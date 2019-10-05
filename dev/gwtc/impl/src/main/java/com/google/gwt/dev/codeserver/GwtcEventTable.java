package com.google.gwt.dev.codeserver;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.In1;
import xapi.fu.In2Out1;
import xapi.util.api.Destroyable;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.codeserver.JobEvent;
import com.google.gwt.dev.codeserver.JobEvent.Status;
import com.google.gwt.dev.codeserver.JobEventTable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/18/17.
 */
public class GwtcEventTable extends JobEventTable {
    final StringTo<In1<JobEvent>> successListeners = X_Collect.newStringMap(In1.class);
    final StringTo<In1<JobEvent>> failListeners = X_Collect.newStringMap(In1.class);
    final StringTo<In1<JobEvent>> cleanupListeners = X_Collect.newStringMap(In1.class);
    final StringTo<In1<JobEvent>> progressListeners = X_Collect.newStringMap(In1.class);

    @Override
    public synchronized JobEvent getCompilingJobEvent() {
        return super.getCompilingJobEvent();
    }

    @Override
    public synchronized void publish(JobEvent event, TreeLogger logger) {
        final String id = event.getInputModuleName();
        super.publish(event, logger);
        try {

            switch (event.getStatus()) {
                case COMPILING:
                    progressListeners.getMaybe(id)
                        .readIfPresent(callback -> callback.in(event));
                    break;
                case SERVING:
                    successListeners.getMaybe(id)
                        .readIfPresent(callback -> callback.in(event));
                    break;
                case ERROR:
                    failListeners.getMaybe(id)
                        .readIfPresent(callback -> callback.in(event));
                    break;
                case GONE:
                    cleanupListeners.getMaybe(id)
                        .readIfPresent(callback -> callback.in(event));
                    cleanup(progressListeners.get(id));
                    cleanup(successListeners.get(id));
                    cleanup(failListeners.get(id));
                    break;
            }
        } finally {
            synchronized (progressListeners) {
                progressListeners.notifyAll();
            }
        }
    }

    private void cleanup(In1<JobEvent> callback) {
        if (callback instanceof Destroyable) {
            ((Destroyable) callback).destroy();
        }

    }

    public void listenForEvents(String moduleName, In1<JobEvent> callback) {
        listenForEvents(moduleName, Status.SERVING, callback);
    }

    public void listenForEvents(String moduleName, Status status, In1<JobEvent> callback) {
        listenForEvents(moduleName, status, true, callback);
    }

    public void listenForEvents(String moduleName, Status status, boolean onlyOnce, In1<JobEvent> callback) {
        final In2Out1<String, In1<JobEvent>, In1<JobEvent>> factory = (k, v) -> {
            if (v == null) {
                return onlyOnce ? callback.onlyOnce() : callback;
            }
            // useAfterMe will actually remove entities that have already been run,
            // so there's no need to worry about cleaning up / removing callbacks;
            // once they've been run, the next time a callback for that module
            // is received, the old callback will know it has been used, and elide itself.
            return v.useAfterMe(onlyOnce ? callback.onlyOnce() : callback);
        };
        if (status == null) {
            progressListeners.compute(moduleName, factory);
            successListeners.compute(moduleName, factory);
            failListeners.compute(moduleName, factory);
            cleanupListeners.compute(moduleName, factory);
            return;
        }
        switch (status) {
            case COMPILING:
                progressListeners.compute(moduleName, factory);
                break;
            case SERVING:
                successListeners.compute(moduleName, factory);
                break;
            case ERROR:
                failListeners.compute(moduleName, factory);
                break;
            case GONE:
                cleanupListeners.compute(moduleName, factory);
                break;
        }
    }
}
