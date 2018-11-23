package xapi.gradle.api;

import org.gradle.api.Named;
import org.gradle.api.tasks.SourceSet;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/15/18 @ 1:00 AM.
 */
public class SourceConfig implements Named {

    private SourceSet mainSource;
    private SourceSet testSource;
    private final String name;

    public SourceConfig(String name) {
        this.name = name;
    }

    public SourceSet getMainSource() {
        return mainSource;
    }

    public void setMainSource(SourceSet mainSource) {
        this.mainSource = mainSource;
    }

    public SourceSet getTestSource() {
        return testSource;
    }

    public void setTestSource(SourceSet testSource) {
        this.testSource = testSource;
    }

    @Override
    public String getName() {
        return name;
    }
}
