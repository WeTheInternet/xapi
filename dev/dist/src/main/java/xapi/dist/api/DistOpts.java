package xapi.dist.api;

import xapi.args.KwArgProcessorBase;
import xapi.fu.itr.SizedIterable;

import java.io.File;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class DistOpts extends KwArgProcessorBase {

    private final ArgHandlerEntryPoints dist = new ArgHandlerEntryPoints();
    private final ArgHandlerPackageFilter filters = new ArgHandlerPackageFilter();
    private final ArgHandlerOutputDir output = new ArgHandlerOutputDir();
    private final ArgHandlerWorkDir work = new ArgHandlerWorkDir();

    public DistOpts() {
        registerHandler(dist);
        registerHandler(filters);
        registerHandler(output);
        registerHandler(work);
    }

    public SizedIterable<String> getEntryPoints() {
        return dist.getDistEntryPoints();
    }

    public void addPackageFilter(String name) {
        filters.setString(name);
    }

    public SizedIterable<String> getPackageFilters() {
        return filters.getPackageFilters();
    }

    public void addEntryPoint(String name) {
        dist.setString(name);
    }

    public File getOutputDir() {
        return output.getFile();
    }

    public File getWorkDir() {
        return work.getFile();
    }

    public void setOutputDir(File outputDir) {
        if (!outputDir.exists()) {
            final boolean result = outputDir.mkdirs();
            assert result : "Unable to create outputDir " + outputDir;
        }
        output.setFile(outputDir);
    }

    public void setWorkDir(File workDir) {
        if (!workDir.exists()) {
            final boolean result = workDir.mkdirs();
            assert result : "Unable to create workDir " + workDir;
        }
        work.setFile(workDir);
    }

}
