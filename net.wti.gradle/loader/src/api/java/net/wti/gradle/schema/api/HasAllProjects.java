package net.wti.gradle.schema.api;

import net.wti.gradle.api.MinimalProjectView;
import xapi.fu.Do;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.data.SetLike;
import xapi.fu.itr.SizedIterable;

import java.io.File;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-06-14 @ 3:54 a.m..
 */
public interface HasAllProjects {

    SchemaCallbacks getCallbacks();

    default void flushWork() {
        getCallbacks().flushCallbacks(this);
    }

    SetLike<SchemaProject> getAllProjects();

    String getGroup();

    void setGroup(String group);

    void setVersion(String version);

    String getVersion();

    Lazy<SchemaIndex> getIndexProvider();

    MinimalProjectView getView();

    File getRootSchemaFile();

    default Maybe<SchemaProject> findProject(String path) {
        final String gradlePath = path.startsWith(":") ? path : ":" + path;
        final SizedIterable<SchemaProject> results = getAllProjects().filter(proj -> proj.getPathGradle().equals(
                gradlePath)).counted();
        if (results.isEmpty()) {
            return Maybe.not();
        }
        assert results.size() == 1 : "Multiple SchemaProject match " + path;
        return Maybe.immutable(results.first());
    }

    void whenResolved(Do job);

    void resolve();
}
