package xapi.gradle.task;

import org.gradle.api.Task;
import xapi.gradle.api.ArchiveType;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/5/18 @ 1:13 AM.
 */
public class ArchiveAndTaskType {
    private final Class<? extends Task> taskType;
    private final ArchiveType archiveType;

    public ArchiveAndTaskType(Class<? extends Task> taskType, ArchiveType archiveType) {
        this.taskType = taskType;
        this.archiveType = archiveType;
    }

    public static ArchiveAndTaskType from(ArchiveType archiveType, Class<? extends Task> taskType) {
        return new ArchiveAndTaskType(taskType, archiveType);
    }

    public Class<? extends Task> getTaskType() {
        return taskType;
    }

    public ArchiveType getArchiveType() {
        return archiveType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final ArchiveAndTaskType taskPair = (ArchiveAndTaskType) o;

        if (!taskType.equals(taskPair.taskType))
            return false;
        return archiveType.equals(taskPair.archiveType);
    }

    @Override
    public int hashCode() {
        int result = taskType.hashCode();
        result = 31 * result + archiveType.hashCode();
        return result;
    }
}
