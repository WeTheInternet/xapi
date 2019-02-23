package net.wti.gradle.internal.require.api;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;

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
        public Configuration findConsumerConfig(ArchiveGraph module, boolean only) {
            return only ? module.configIntransitive() : module.configTransitive();
//            return only ? module.configCompileOnly() : module.configCompile(); // hm, we should accept lenient here as well...
        }

        @Override
        public Configuration findProducerConfig(ArchiveGraph module, boolean only) {
            return module.configExportedApi();
        }

        @Override
        public Usage findUsage(
            ProjectGraph project, Configuration consumer, Configuration producer
        ) {
            return project.usageApiClasses();
        }
    },
    /**
     * The transitive runtime scope of your module
     */
    Runtime {
        @Override
        public Configuration findConsumerConfig(ArchiveGraph module, boolean only) {
            return only ? module.configRuntimeOnly() : module.configRuntime();
        }

        @Override
        public Configuration findProducerConfig(ArchiveGraph module, boolean only) {
            return module.configExportedRuntime();
        }

        @Override
        public Usage findUsage(ProjectGraph project, Configuration consumer, Configuration producer) {
            return project.usageRuntime();
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
    public Configuration findConsumerConfig(ArchiveGraph module, boolean only) {
        throw new UnsupportedOperationException(this + " not yet supported");
    }

    @Override
    public Configuration findProducerConfig(ArchiveGraph module, boolean only) {
        throw new UnsupportedOperationException(this + " not yet supported");
    }
}
