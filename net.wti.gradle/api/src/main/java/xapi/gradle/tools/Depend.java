package xapi.gradle.tools;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/10/18 @ 3:06 AM.
 */
public class Depend {

    private final String xapiVersion = "0.5.1-SNAPSHOT";
    private final Project source;
    private final boolean composited;
    public Depend(Project source) {
        this.source = source;
        try {
            source.getGradle().includedBuild("xapi");
        } catch (Exception failed) {
            composited = false;
            return;
        }
        composited = true;
    }

    public Dependency onXapi(String project) {
        return onXapi(project.substring(project.lastIndexOf(':')+1), project);
    }
    public Dependency onXapi(String artifact, String project) {
        if (!project.startsWith(":")) {
            project = ":" + project;
        }
        return source.getDependencies().create(
            composited ?
//                source.getGradle().includedBuild(":xapi" + project )
                "net.wetheinter:" + artifact + ":" + xapiVersion
                :
                source.project(project)
        );
    }

}
