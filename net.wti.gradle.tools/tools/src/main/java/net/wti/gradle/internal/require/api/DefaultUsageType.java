package net.wti.gradle.internal.require.api;

import org.gradle.api.artifacts.Configuration;

/**
 * An enumerated set of standard {@link UsageType};
 * Api and Runtime represent the familiar gradle Usages of `api` and `runtime`,
 * and are tailored to as-seamlessly-as-possible integrate with usage of java and java-library plugins.
 *
 * The rest are currently speculative,
 * and would represent additional sourcesets,
 * to be assembled, inserted and used in various places.
 *
 * See each enum element for a short synopsis.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2/5/19 @ 11:53 PM.
 */
public enum DefaultUsageType implements UsageType {
    /**
     * The transitive compile scope of your module
     */
    Api {
        @Override
        public Configuration findConfig(ArchiveGraph module, boolean only) {
            return only ? module.configIntransitive() : module.configTransitive();
        }
    },
    /**
     * The transitive runtime scope of your module
     */
    Runtime {
        @Override
        public Configuration findConfig(ArchiveGraph module, boolean only) {
            return only ? module.configRuntimeOnly() : module.configRuntime();
        }
    },
    /**
     * Extra runtime code you only want during development
     */
    Dev,
    /**
     * Extra build-time code / resources / configuration
     */
    Build,
    /**
     * Extra artifacts you want available only in test code
     */
    Test
    ;

    @Override
    public String getName() {
        return name().toLowerCase();
    }

    @Override
    public Configuration findConfig(ArchiveGraph module, boolean only) {
        throw new UnsupportedOperationException(this + " not yet supported");

    }
}
