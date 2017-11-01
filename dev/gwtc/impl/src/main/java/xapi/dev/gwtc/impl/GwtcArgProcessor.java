package xapi.dev.gwtc.impl;

import xapi.args.ArgHandlerFile;
import xapi.args.KwArgProcessorBase;
import xapi.dev.gwtc.api.GwtcJobMonitor;

import java.io.File;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/20/17.
 */
public class GwtcArgProcessor extends KwArgProcessorBase {

    protected static class LogFileHandler extends ArgHandlerFile {
        final String tag = GwtcJobMonitor.ARG_LOG_FILE;

        @Override
        public String getPurpose() {
            return "Specify the file to redirect our logs to";
        }

        @Override
        public String getTag() {
            return tag;
        }

    }

    protected static class UnitCacheFileHandler extends ArgHandlerFile {
        final String tag = GwtcJobMonitor.ARG_UNIT_CACHE_DIR;

        @Override
        public String getPurpose() {
            return "Specify the (optional) location to use for gwt unit cache (be sure you clean / change this regularly!)";
        }

        @Override
        public String getTag() {
            return tag;
        }

    }

    private final LogFileHandler logFileHandler;
    private final UnitCacheFileHandler unitCacheFileHandler;

    public GwtcArgProcessor() {
        logFileHandler = new LogFileHandler();
        unitCacheFileHandler = new UnitCacheFileHandler();
        registerHandler(logFileHandler);
        registerHandler(unitCacheFileHandler);
    }

    public File getLogFile() {
        return logFileHandler.getFile();
    }

    public File getUnitCacheDir() {
        return unitCacheFileHandler.getFile();
    }
}
