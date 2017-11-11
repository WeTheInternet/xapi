package xapi.dev.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.model.api.Model;
import xapi.util.X_String;

/**
 * Describes a (currently maven) module's sources in terms of:
 * a single, non-null output directory:
 * output
 *
 * with four sets of nullable list-inputs:
 * sources[],
 * resources[],
 * testSources[],
 * testResources[]
 *
 * and one nullable staging output directory:
 * staging,
 * which will be the output root for final builds,
 * derived from pom property "xapi.staging" (default to ./src/main/staging)
 *
 * TODO: consider doing /src/staging/* where * = sources+resources | sed s/main/staging/
 * translation: /src/main/java + /src/main/resources = /src/staging/java, /src/staging/resources.
 *
 * We will go with a single directory for initial implementation,
 * since it is likely faster for other tools to only have to consider one source root.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 11/7/17.
 */
public interface ProjectSources extends Model {

    String getOutput();
    void setOutput(String output);
    default String output() {
        return X_String.firstNotEmpty(getOutput(), "./target/classes");
    }

    String getStaging();
    void setStaging(String staging);
    default String staging() {
        return X_String.firstNotEmpty(getStaging(), "./src/main/staging");
    }

    IntTo<String> getSources();
    void setSources(IntTo<String> sources);
    default IntTo<String> sources() {
        return getOrCreate(this::getSources, ()-> X_Collect.newList(String.class), this::setSources);
    }

    IntTo<String> getResources();
    void setResources(IntTo<String> resources);
    default IntTo<String> resources() {
        return getOrCreate(this::getResources, ()-> X_Collect.newList(String.class), this::setResources);
    }

    IntTo<String> getTestSources();
    void setTestSources(IntTo<String> testSources);
    default IntTo<String> testSources() {
        return getOrCreate(this::getTestSources, ()-> X_Collect.newList(String.class), this::setTestSources);
    }

    IntTo<String> getTestResources();
    void setTestResources(IntTo<String> testResources);
    default IntTo<String> testResources() {
        return getOrCreate(this::getTestResources, ()-> X_Collect.newList(String.class), this::setTestResources);
    }

}
