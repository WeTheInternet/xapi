package xapi.gwtc.api;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.scope.X_Scope;
import xapi.time.X_Time;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/25/16.
 */
public interface ServerRecompiler {

    enum TargetThread {
        CALLER, COMPILER, NEW
    }

    void useServer(In1<IsRecompiler> callback);

    default void useServer(TargetThread runOnTarget, Long millisToWait, In2<IsRecompiler, Throwable> callback) {
        switch (runOnTarget) {
            case CALLER:
                runOnCaller(millisToWait, callback);
                break;
            case COMPILER:
                runOnCompiler(callback);
                break;
            case NEW:
                runOnNewThread(callback);
                break;
        }
    }

    default void runOnNewThread(In2<IsRecompiler, Throwable> callback) {
        final Do inherit = X_Scope.inheritScope();
        X_Time.runLater(()->{
            inherit.done();
            useServer(comp-> {
                callback.in(comp, null);
            });
        });
    }

    default void runOnCompiler(In2<IsRecompiler, Throwable> callback) {

    }

    default void runOnCaller(Long millisToWait, In2<IsRecompiler, Throwable> callback) {

    }
}
