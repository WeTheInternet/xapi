package net.wti.gradle.internal.require.api;

import net.wti.gradle.schema.api.Transitivity;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
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
        public Configuration findConsumerConfig(ArchiveGraph module, Transitivity trans) {
            if (trans == null) {
                return module.configTransitive();
            }
            switch (trans) {
                case api:
                case stub:
                case impl:
                    return module.configTransitive();
                case compile_only:
                case internal:
                    return module.configIntransitive();
            }
            return module.configTransitive();
        }

        @Override
        public Configuration findProducerConfig(ArchiveGraph module, Transitivity transitivity) {
            return module.configExportedApi();
        }

        @Override
        public Usage findUsage(
            ProjectGraph project, Configuration consumer, Configuration producer
        ) {
            return project.usageApiClasses();
        }

        @Override
        public String deriveConfiguration(
            String base,
            Dependency dep,
            boolean isLocal,
            Transitivity transitivity,
            boolean lenient
        ) {
            return isLocal ?
                "main".equals(base) ? "exportCompile" : base + "ExportCompile"
                :
                "compile"
//                "default"
                // TODO: get disambiguation rules working for consuming apiElements
//                "apiElements"
                ;
        }
        // TODO: figure out why the -compile variant doesn't have sourceSet output exposed (yet)
//
//        @Override
//        public String computeRequiredCapabilities(Dependency dep, String fromPlat, String fromMod) {
//            return super.computeRequiredCapabilities(dep, fromPlat, fromMod.isEmpty() || "main".equals(fromMod) ? "compile" : fromMod + "-compile");
//        }
    },
    /**
     * The transitive runtime scope of your module
     */
    Runtime {
        @Override
        public Configuration findConsumerConfig(ArchiveGraph module, Transitivity trans) {
            if (trans == null) {
                return module.configRuntime();
            }
            switch (trans) {
                case compile_only:
                case runtime_only:
                case execution:
                    module.configRuntimeOnly();
                case runtime:
                case internal:
                case api:
                    return module.configRuntime();
                case stub:
                case impl:
            }
            return module.configRuntime();
        }

        @Override
        public Configuration findProducerConfig(ArchiveGraph module, Transitivity transitivity) {
            return module.configExportedRuntime();
        }

        @Override
        public Usage findUsage(ProjectGraph project, Configuration consumer, Configuration producer) {
            return project.usageRuntime();
        }

        @Override
        public String deriveConfiguration(
            String base,
            Dependency dep,
            boolean isLocal, Transitivity transitivity,
            boolean lenient
        ) {
            return isLocal ?
                "main".equals(base) ? "exportRuntime" : base + "ExportRuntime"
                :
                "runtime"
//                "default"
//                "runtimeElements"
                ;
        }
    },
    Source {
        @Override
        public Configuration findConsumerConfig(ArchiveGraph module, Transitivity trans) {
            return module.configSource();
        }

        @Override
        public Configuration findProducerConfig(ArchiveGraph module, Transitivity transitivity) {
            return module.configExportedSource();
        }

        @Override
        public Usage findUsage(ProjectGraph project, Configuration consumer, Configuration producer) {
            return project.usageSourceJar(); // TODO: pick between Source and SourceJar based on consuming configuration...
        }

        @Override
        public String deriveConfiguration(
            String base,
            Dependency dep,
            boolean isLocal, Transitivity transitivity,
            boolean lenient
        ) {
            return isLocal && !"false".equals(System.getProperty("xapi.composite")) ? "main".equals(base) ? "exportSource" : base + "ExportSource" : null;
        }

        @Override
        public String computeRequiredCapabilities(Dependency dep, String fromPlat, String fromMod, String classifier) {
            return super.computeRequiredCapabilities(dep, fromPlat, fromMod, null) + "-sources";
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
    public Configuration findConsumerConfig(ArchiveGraph module, Transitivity transitivity) {
        throw new UnsupportedOperationException(this + " not yet supported");
    }

    @Override
    public Configuration findProducerConfig(ArchiveGraph module, Transitivity transitivity) {
        throw new UnsupportedOperationException(this + " not yet supported");
    }

    @Override
    public String deriveConfiguration(
        String base, Dependency dep, boolean isLocal, Transitivity transitivity, boolean lenient
    ) {
        throw new UnsupportedOperationException(this + " not yet supported");
    }

    @Override
    public String computeRequiredCapabilities(
        Dependency dep,
        String fromPlat,
        String fromMod,
        String classifier
    ) {
        // We are overriding this b/c groovy can complain when trying to find inherited default methods
        return UsageType.super.computeRequiredCapabilities(dep, fromPlat, fromMod, classifier);
    }
}
