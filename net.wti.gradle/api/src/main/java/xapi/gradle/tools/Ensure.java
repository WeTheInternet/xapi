package xapi.gradle.tools;

import org.gradle.api.Action;
import org.gradle.api.Project;

/**
 * A tool to use for ensuring a given state has been reached (like project.state.executed).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/14/18 @ 1:07 AM.
 */
public class Ensure {

    public static void projectEvaluated(Project p, Action<? super Project> callback) {
        if (p.getState().getExecuted()) {
            callback.execute(p);
        } else {
            p.afterEvaluate(callback);
        }
    }

}
