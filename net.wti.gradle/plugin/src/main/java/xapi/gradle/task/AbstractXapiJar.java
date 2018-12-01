package xapi.gradle.task;

import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.bundling.Jar;
import xapi.gradle.api.ArchiveType;
import xapi.gradle.api.DefaultArchiveType;
import xapi.gradle.api.HasArchiveType;

import java.util.*;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/5/18 @ 12:34 AM.
 */
public class AbstractXapiJar extends Jar implements HasArchiveType {

    @Input
    private ArchiveType archiveType;

    @Input
    private boolean includeAll;

    @Override
    public ArchiveType getArchiveType() {
        return archiveType != null ? archiveType :
            getArchivePath().getAbsolutePath().contains("test") ?
                DefaultArchiveType.TEST :
                DefaultArchiveType.MAIN;
    }

    @Override
    public void setArchiveType(ArchiveType type) {
        this.archiveType = type;
    }

    protected Set<ArchiveAndTaskType> children = new LinkedHashSet<>();
    protected Set<ArchiveType> childTypes = new LinkedHashSet<>();

    /**
     * Used to add auto-wiring of `from tasks.getBy...` for archive types.
     *
     *
     *
     * @param types
     */
    protected void addArchiveTypes(ArchiveType ... types) {
        // Prefer a linked identity hashset
        for (ArchiveType type : types) {
            childTypes.add(type);
            for (Class<? extends Task> taskType : type.getTaskTypes()) {
                addTypedTask(type, taskType);
            }
        }
    }

    protected void addTypedTask(ArchiveType type, Class<? extends Task> taskType) {
        // Each pair of archive type and task type gets it's own `from(task)` CopySpec to manipulate.
        // Should one of two competing archive types use includeAll, the includeAll will win (includeAll is the "hammer").
        if (!children.add(ArchiveAndTaskType.from(type, taskType))) {
            return;
        }
        getProject().getTasks().withType(taskType)
            .all((Task t)->
                // This copies the task output to our inputs, binds dependsOn, and supplies our copy spec.
                from(t, spec -> {
                    if (isIncludeAll() || type.isIncludeAll()) {
                        // no filtering needed for an includeAll type;
                        // this is appropriate for composite assemblies
                        return;
                    }
                    if (t instanceof AbstractXapiJar) {
                        if (((AbstractXapiJar) t).isIncludeAll()) {
                            return;
                        }
                    }
                    final String[] fileTypes = type.getFileTypes();
                    for (String fileType : fileTypes) {
                        spec.include(
                            fileType.contains("/") ? fileType :
                                // transform ".class" into ant path equivalent: **/*.class
                                "**/*" + fileType);
                    }
                }
            ));
    }

    public boolean isIncludeAll() {
        return includeAll || getArchiveType().isIncludeAll();
    }

    public void setIncludeAll(boolean includeAll) {
        this.includeAll = includeAll;
    }
}
