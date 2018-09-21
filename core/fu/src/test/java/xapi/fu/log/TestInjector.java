package xapi.fu.log;

import xapi.fu.log.Log.LogLevel;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/20/18 @ 11:44 PM.
 */
public class TestInjector extends LogInjector {
    private LogLevel level;
    private String msg;

    @Override
    public Log defaultLogger() {
        return ((level, msg) -> {
            this.level = level;
            this.msg = msg;
        });
    }
}
