package xapi.gradle.api;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import xapi.gradle.task.SourceJar;

/**
 * This type is a container for all the "standard jar tasks" that a java project might use,
 * as well as a container for additional types of jars that you may want to produce
 * (i.e. add your own jar type).
 *
 * In the future we may make this a named domain object set,
 * so it can be configured via:
 * xapi {
 *     jars {
 *         gwt {
 *             // gets or creates a gwt jar
 *         }
 *     }
 * }
 * See org.gradle.api.internal.tasks.DefaultSourceSetContainer for inspiration
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 1:14 AM.
 */
public class AllJars {

    public static final String SOURCE_JAR = "sourceJar";
    private final TaskProvider<SourceJar> sourceJar;
    private ArchiveType mainType = DefaultArchiveTypes.MAIN;

    public AllJars(Project project) {

        this.sourceJar = project.getTasks().register(
            SOURCE_JAR,
            SourceJar.class);

    }

    public TaskProvider<SourceJar> getSources() {
        return sourceJar;
    }

    public ArchiveType getMainType() {
        return mainType;
    }
}
