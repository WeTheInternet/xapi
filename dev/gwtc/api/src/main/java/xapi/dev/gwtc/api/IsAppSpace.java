package xapi.dev.gwtc.api;

import java.io.File;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/20/17.
 */
public interface IsAppSpace {

    File getSpeedTracerLogFile();

    File getUnitCacheDir();

    File getCompileDir(int compileId);
}
