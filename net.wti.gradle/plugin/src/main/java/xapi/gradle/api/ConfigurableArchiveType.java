package xapi.gradle.api;

import org.gradle.api.Named;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/27/18 @ 4:19 AM.
 */
public class ConfigurableArchiveType implements Named, ArchiveType {

    private final String name;

    public ConfigurableArchiveType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String name() {
        return name;
    }

    // TODO: add overrides as needed...
}
