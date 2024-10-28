package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.XapiUsage;
import net.wti.gradle.schema.api.Transitivity;
import org.gradle.api.Named;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Usage;

import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2/5/19 @ 11:58 PM.
 */
public interface UsageType extends Named {

    Configuration findConsumerConfig(ArchiveGraph module, Transitivity transitivity);

    Configuration findProducerConfig(ArchiveGraph module, Transitivity transitivity);

    default Usage findUsage(ProjectGraph project, Configuration consumer, Configuration producer) {
        Usage result;
        return (result = getPreferredUsage(project, consumer, producer)) != null ? result :
               (result = consumer.getAttributes().getAttribute(USAGE_ATTRIBUTE)) != null ? result :
               (result = producer.getAttributes().getAttribute(USAGE_ATTRIBUTE)) != null ? result :
               getDefaultUsage(project);
    }

    /**
     * The default usage is {@link XapiUsage#INTERNAL}.
     *
     * This usage includes the compiled main output,
     * the complete compile classpath,
     * all internal / "runtime" transitive dependencies,
     * and may (configurably) include transitive source dependencies.
     *
     * @param project The project graph node,
     *                so you can influence this behavior through ProjectGraph subclassing.
     * @return The default {@link Usage} for a given UsageType,
     *                used when neither producer nor consumer have a Usage attribute.
     *
     * If you want to override the default-selection semantics of {@link #findUsage(ProjectGraph, Configuration, Configuration)}
     * instead override {@link #getPreferredUsage(ProjectGraph, Configuration, Configuration)}.
     */
    default Usage getDefaultUsage(ProjectGraph project) {
        return project.usageInternal();
    }

    /**
     * The preferred usage is null.
     *
     * This method exists to give you a place to optionally provide a non-null {@link Usage},
     * without having to override {@link #findUsage(ProjectGraph, Configuration, Configuration)}.
     *
     * This preserves our standard fallback of "first check the consumer, then check the producer",
     * and allows a man-in-the-middle class to override {@link #findUsage(ProjectGraph, Configuration, Configuration)}
     * and chose when in their logic to call super.findUsage().
     *
     * @param project The project graph node,
     *                so you can influence this behavior through ProjectGraph subclassing.
     * @param consumer The consumer configuration; normally checked before producer.
     * @param producer The producer configuration; normally checked after consumer.
     * @return A nullable {@link Usage} that will take first precedence in {@link #findUsage(ProjectGraph, Configuration, Configuration)}.
     *
     */
    default Usage getPreferredUsage(
        ProjectGraph project,
        Configuration consumer,
        Configuration producer
    ) {
        return null;
    }

    default ModuleDependency newDependency(ProjectGraph project, ArchiveGraph consumer, ArchiveGraph producer, Transitivity transitivity) {
        final DependencyHandler deps = consumer.getView().getDependencies();
        final ModuleDependency dep = (ModuleDependency) deps.create(producer.getView().getService().getProject());
        if (!"true".equals(String.valueOf(producer.getView().findProperty("xapiModern")))) {
            // modern project references should not mess with target configurations
            // TODO: add validation that target configuration exists.
            dep.setTargetConfiguration(findProducerConfig(producer, transitivity).getName());
        }
        return dep;
    }

    String deriveConfiguration(
        String base,
        Dependency dep,
        boolean isLocal,
        Transitivity transitivity,
        boolean lenient
    );

    default String computeRequiredCapabilities(
        Dependency dep,
        String fromPlat,
        String fromMod,
        String classifier
    ) {
        // consider using the classifier here? we're going to rework all this mess shortly anyway...
        return dep.getGroup() +
            ":" +
            XapiNamer.moduleName(dep.getName(), fromPlat, fromMod)
        ;
    }
}
