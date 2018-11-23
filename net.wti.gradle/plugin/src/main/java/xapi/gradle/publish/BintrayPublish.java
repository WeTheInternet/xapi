package xapi.gradle.publish;

import com.jfrog.bintray.gradle.BintrayPlugin;
import org.gradle.api.Project;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 12:45 AM.
 */
class BintrayPublish {
    static void publishBintray(Project p) {
        p.getPlugins().apply(BintrayPlugin.class);

    }
}
