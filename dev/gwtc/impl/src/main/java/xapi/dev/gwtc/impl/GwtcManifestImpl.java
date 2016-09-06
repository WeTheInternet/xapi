package xapi.dev.gwtc.impl;

import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;
import xapi.util.X_Util;

import java.io.IOException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/27/16.
 */
public class GwtcManifestImpl extends GwtManifest {

    public GwtcManifestImpl(String moduleName) {
        super(moduleName);
    }

    @Override
    protected String normalizeGenDir(String genDir) {
        java.io.File f;
        if (genDir == null) {
            f = new java.io.File(getWorkDir(), GEN_PREFIX);
        } else {
            f = new java.io.File(genDir);
        }
        boolean result = f.exists() || f.mkdirs();
        assert result : "Unable to create parent directories of " + f;
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            X_Log.warn(getClass(), "Invalid generated code directory ", f);
            throw X_Util.rethrow(e);
        }
    }

    @Override
    protected String normalizeWorkDir(String workDir) {
        if (workDir == null) {
            try {
                java.io.File f = java.io.File.createTempFile("GwtcTmp", "Compile");
                f.delete();
                f.mkdirs();
                workDir = f.getAbsolutePath();
                setWorkDir(workDir);
            } catch (IOException e) {
                X_Log.error(getClass(), "Unable to create a work dir in temp directory", e);
                throw X_Util.rethrow(e);
            }
        }
        return super.normalizeWorkDir(workDir);
    }
}
