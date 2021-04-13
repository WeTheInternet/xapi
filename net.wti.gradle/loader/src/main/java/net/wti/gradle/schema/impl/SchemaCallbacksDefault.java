package net.wti.gradle.schema.impl;

import net.wti.gradle.schema.api.HasAllProjects;
import net.wti.gradle.schema.api.HasPath;
import net.wti.gradle.schema.api.SchemaCallbacks;
import net.wti.gradle.schema.api.SchemaPlatform;
import net.wti.gradle.schema.api.SchemaModule;
import net.wti.gradle.schema.api.SchemaProject;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.In2;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-06-14 @ 1:21 a.m..
 */
public class SchemaCallbacksDefault implements SchemaCallbacks {

    private static final int RECURSION_LIMIT = 1024;
    private final LinkedHashMap<String, In1<SchemaProject>> projectCallbacks = new LinkedHashMap<>();

    public SchemaCallbacksDefault() {

    }

    @Override
    public void perModule(In1<SchemaModule> callback) {
        perProject(proj -> proj.forAllModules(callback));
    }

    @Override
    public void perPlatform(In1<SchemaPlatform> callback) {
        perProject(proj -> proj.forAllPlatforms(callback));
    }

    @Override
    public void perPlatformAndModule(In2<SchemaPlatform, SchemaModule> callback) {
        perProject(proj -> proj.forAllPlatformsAndModules(callback));
    }

    @Override
    public void perProject(In1<SchemaProject> callback) {
        final In1<SchemaProject> singleServe = singleServe(callback);
        projectCallbacks.merge("*", singleServe, In1::useAfterMe);
    }

    @Override
    public void forProject(
        In1Out1<CharSequence, Boolean> key, In1<SchemaProject> callback
    ) {
        SchemaCallbacks.super.forProject(key, callback);
    }

    private <T extends HasPath> In1<T> singleServe(In1<T> callback) {
        final HashSet<String> seen = new HashSet<>();
        return candidate -> {
            if (seen.add(candidate.getPath())) {
                callback.in(candidate);
            }
        };
    }

    @Override
    public void flushCallbacks(HasAllProjects map) {
        int recursionSickness = RECURSION_LIMIT;
        do {
            if (recursionSickness--<=0) {
               throw new IllegalStateException("Callbacks for projects " + projectCallbacks.keySet() +" not called within " + RECURSION_LIMIT + " iterations");
            }
            map.getAllProjects().forEach(project -> {
                final In1<SchemaProject> task = projectCallbacks.remove(project.getPath());
                if (task != null) {
                    task.in(project);
                }
            });
            flushAllProjects(map);
        } while (!isDone());
    }

    protected boolean isDone() {
        if (projectCallbacks.isEmpty()) {
            return true;
        }
        final Set<String> keys = projectCallbacks.keySet();
        return keys.size() == 1 && keys.contains("*");
    }

    private void flushAllProjects(HasAllProjects map) {
        In1<SchemaProject> current = null;
        In1<SchemaProject> task = projectCallbacks.get("*");
        int recursionSickness = RECURSION_LIMIT;
        while (task != current) {
            current = task;
            if (recursionSickness--<=0) {
                throw new IllegalArgumentException("Recursive all-projects callback blindly adding new all-projects callbacks; took " + RECURSION_LIMIT + " iterations to fail.");
            }
            map.getAllProjects().forEach(task.toConsumer());
            task = projectCallbacks.get("*");
        }
    }
}
