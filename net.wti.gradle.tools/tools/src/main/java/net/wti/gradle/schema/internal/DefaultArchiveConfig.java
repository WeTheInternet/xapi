package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.system.tools.GradleCoerce;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 1:45 PM.
 */
public class DefaultArchiveConfig implements ArchiveConfig {

    private final String name;
    private final Set<Object> required;

    public DefaultArchiveConfig(String name) {
        this.name = name;
        required = new LinkedHashSet<>();
    }

    @Override
    public void require(Object ... units) {
        required.addAll(Arrays.asList(units));
    }

    @Override
    public Set<String> required() {
        final LinkedHashSet<String> result = new LinkedHashSet<>();
        if (required.isEmpty()) {
            // Nothing set?  apply convention.
            switch (name) {
                case "main":
                    result.add("api");
                    result.add("spi");
                    break;
                case "mainSource":
                    result.add("apiSource");
                    result.add("spiSource");
                    break;
                case "stub":
                    result.add("main");
                    break;
                case "stubSource":
                    result.add("mainSource");
                    break;
                case "impl":
                    result.add("main");
                    result.add("stub*");
                    break;
                case "implSource":
                    result.add("mainSource");
                    result.add("stubSource*");
                    break;
            }
        } else {
            for (Object require : required) {
                result.addAll(GradleCoerce.unwrapStrings(require));
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "DefaultArchiveConfig{" +
            "name='" + name + '\'' +
            ", required=" + required +
            '}';
    }
}
