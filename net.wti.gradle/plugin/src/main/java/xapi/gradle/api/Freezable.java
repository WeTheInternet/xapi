package xapi.gradle.api;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/5/18 @ 2:37 AM.
 */
public interface Freezable {

    void freeze(Project from, Object ... context);

    default void freezeCheck(boolean frozen) {
        if (frozen) {
            throw new GradleException(this + " was already frozen");
        }
    }
}
