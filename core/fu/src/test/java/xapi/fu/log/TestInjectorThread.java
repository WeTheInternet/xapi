package xapi.fu.log;

import xapi.fu.log.Log.LogLevel;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/20/18 @ 11:49 PM.
 */
public class TestInjectorThread extends Thread {

    private static final String SUCCESS_MESSAGE = "Successful";
    private boolean success;
    private TestInjector injector;

    @Override
    public void run() {
        success = LogInjector.inst() instanceof TestInjector;
        injector = (TestInjector) LogInjector.inst();
        LogInjector.DEFAULT.log(LogLevel.WARN, SUCCESS_MESSAGE);
    }

    public boolean isSuccess() {
        return success;
    }

    public TestInjector getInjector() {
        return injector;
    }
}
