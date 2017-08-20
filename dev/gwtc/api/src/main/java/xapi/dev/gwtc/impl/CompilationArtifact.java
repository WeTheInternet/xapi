package xapi.dev.gwtc.impl;

import java.util.Map;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.dev.codeserver.CompileStrategy;

/**
 * An artifact describing the directories and configuration of the current compilation.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/6/17.
 */
public class CompilationArtifact extends Artifact<CompilationArtifact> {

    protected String uri;
    protected String warDir;
    protected String workDir;
    protected String deployDir;
    protected String extraDir;
    protected String genDir;
    protected String logFile;
    protected String sourceDir;
    protected Map<String, String> userAgentMap;
    protected int port;
    private CompileStrategy strategy;

    public CompilationArtifact() {
        super(MetaLinker.class);
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (warDir != null ? warDir.hashCode() : 0);
        result = 31 * result + (workDir != null ? workDir.hashCode() : 0);
        result = 31 * result + (deployDir != null ? deployDir.hashCode() : 0);
        result = 31 * result + (extraDir != null ? extraDir.hashCode() : 0);
        result = 31 * result + (genDir != null ? genDir.hashCode() : 0);
        result = 31 * result + (logFile != null ? logFile.hashCode() : 0);
        result = 31 * result + (sourceDir != null ? sourceDir.hashCode() : 0);
        result = 31 * result + (userAgentMap != null ? userAgentMap.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (strategy != null ? strategy.hashCode() : 0);
        return result;
    }

    @Override
    protected int compareToComparableArtifact(CompilationArtifact o) {
        int diff = compare(warDir, o.warDir);
        if (diff != 0) {
            return diff;
        }
        diff = compare(deployDir, o.deployDir);
        if (diff != 0) {
            return diff;
        }

        return 0;
    }

    private int compare(String a, String b) {
        if (a == null) {
            if (b == null) {
                return 0;
            }
            return -1;
        } else if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    @Override
    protected Class<CompilationArtifact> getComparableArtifactType() {
        return CompilationArtifact.class;
    }
}
