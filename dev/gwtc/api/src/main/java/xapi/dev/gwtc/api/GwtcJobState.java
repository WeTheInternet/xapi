package xapi.dev.gwtc.api;

import xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage;
import xapi.fu.In2Out1;
import xapi.gwtc.api.GwtManifest;

import java.util.concurrent.TimeUnit;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/20/17.
 */
public interface GwtcJobState {
    boolean isRunning();

    boolean isFailed();

    boolean isSuccess();

    In2Out1<Integer, TimeUnit, Integer> getBlocker();

    void destroy();

    void onStart(GwtManifest manifest);

    boolean isReusable(GwtManifest manifest);

    IsAppSpace getAppSpace();

    String getModuleName();

    String getModuleShortName();

    static boolean isComplete(CompileMessage state) {
        if (state == null) {
            return false;
        }
        return state.isComplete();
    }
}
