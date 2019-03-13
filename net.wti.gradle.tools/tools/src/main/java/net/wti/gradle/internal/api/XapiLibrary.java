package net.wti.gradle.internal.api;

import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.publish.api.PublishedModule;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.component.ComponentWithVariants;
import org.gradle.api.internal.CompositeDomainObjectSet;
import org.gradle.api.internal.component.SoftwareComponentInternal;
import org.gradle.api.internal.component.UsageContext;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import java.util.Set;

/**
 * A XapiLibrary represents an entire project's groups of archives.
 *
 * The library itself is a {@link XapiVariant} representing the main sourceset.
 * It can also contain platform-specific variants, {@link XapiPlatform}s, which are themselves XapiVariants.
 *
 * Each XapiVariant has a default "main" archive, plus optional api, spi and stub archives,
 * source jars, and (eventually) automatic test sourcesets added and wired up sanely.
 *
 * For simple cases, you have no platform-specific implementation code at all,
 * and you just add code to src/main/java, src/api/java like normal.
 *
 * For more complex cases, where you have implementations which export api and spi jars,
 * you would add code to, say:
 * src/main/java
 * src/api/java
 * src/spi/java
 * src/gwt/main/java
 * src/gwt/api/java
 * src/gwt/test/java
 *
 * All transitive dependencies will be automatically wired up for you;
 * all mains will automatically inherit api, while all implementation mains (platforms)
 * will, by default (if no explicit dependencies added) inherit main, api AND spi,
 * as it's presumed that your platform-native code is implementing the spi and glue for the api to work.
 *
 * TODO: either rename this to XapiPublishLibrary, or otherwise use this object to encapsulate
 * the output of realizing a given {@link ProjectGraph}
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/10/18 @ 2:15 AM.
 */
public class XapiLibrary implements SoftwareComponentInternal, ComponentWithVariants {

    public static final String EXT_NAME = "xapiLibrary";
    private final XapiPlatformContainer platforms;
    private final CompositeDomainObjectSet<XapiUsageContext> all;
    /**
     * main is the implicit "gradle java plugin's main" sourceset.
     *
     * In the future, we may want to promote a single platform's MAIN archive type to be the "real main".
     *
     * It is debatable / undecided whether publish non-standard mains should "take over" to artifactId.
     * At the very least, this would be safe if no src/main/java, src/api/java / etc ("main" XapiArchiveSet) are used.
     * We may also simply opt for such artifacts to receive a -main or -core prefix,
     * while the platform that takes over gets prefix-less artifactIds.
     */
    private final Property<XapiPlatform> main;
    private final ObjectFactory objects;
    private final CompositeDomainObjectSet<PublishedModule> allModules;

    @SuppressWarnings("unchecked")
    public XapiLibrary(
        ProjectView view
    ) {
        this.objects = view.getObjects();
        allModules = CompositeDomainObjectSet.create(PublishedModule.class, view.getDecorator());
        platforms =  new XapiPlatformContainer(view, allModules);
        main = objects.property(XapiPlatform.class);
        all = CompositeDomainObjectSet.create(XapiUsageContext.class, view.getDecorator());
        platforms.whenObjectAdded(p-> p.whenModuleAdded(m->all.addCollection(m.getUsages())));
        main.convention(view.getProviders().provider(()->
            getPlatform("main")
        ));
    }

    /**
     * Variants are sets of mutually exclusive components.
     *
     * Each xapi platform represents the "implementation-native" portion of a group of dependencies,
     * and is represented to gradle as a set of software components.
     *
     * The XapiLibrary class itself represents the "main"
     *
     * In most cases, this will be used when creating multiple implementations of an spi.
     * This allows you to have gwt/android/jre/etc sets-of-artifacts,
     * which depend on the correct set of transitive dependencies.
     *
     * These child variant names will be "$prefix-$platform": xapi-gwt, xapi-android, xapi-jfx, etc.
     * Where -Pxapi.prefix controls the root variant name ($prefix), which is the suffix for all child variants.
     * These will match the per-platform artifact naming convention, so it would be perfectly reasonable to have
     * the following sets of published jars:
     *
     * xapi-thing.jar -> XapiLibrary's default dependency, uses main sourceset.
     * xapi-thing-source.jar -> The main sourceset's source jar
     * xapi-thing-api.jar -> api jar, transitive compile-scoped inherited by the main sourceset.
     * xapi-thing-spi.jar -> spi jar, transitive runtime-scoped inherited by the main sourceset.
     * xapi-gwt-thing.jar -> Uses gwt sourceset, provides / depends on everything needed to "use in gwt"
     * xapi-gwt-thing-api.jar -> Gwt-only transitive compile-scoped inherited by the gwt source sourceset.
     *
     *
     * In most cases, `gwt-thing` would inherit and implement `thing-spi`,
     * whereas `gwt-otherthing` would inherit `gwt-thing-api` if it only needs the public API.
     *
     */
    @Override
    public Set<PublishedModule> getVariants() {
        // Return the various XapiPlatform that exist for this module / library
        return platforms.getAllVariants();
    }

    public XapiPlatform getPlatform(String named) {
        return platforms.maybeCreate(named);
    }

    @Override
    public String getName() {
        // replace w/ xapi.prefix
        return "xapi";
    }

    public NamedDomainObjectSet<XapiPlatform> getPlatforms() {
        return platforms;
    }

    public Property<XapiPlatform> getMain() {
        return main;
    }

    public void setMain(XapiPlatform platform) {
        main.set(platform);
    }

    public boolean isEmpty() {
        return platforms.isEmpty() ||
            (platforms.size() == 1 && platforms.iterator().next().isEmpty());
    }

    @Override
    public Set<? extends UsageContext> getUsages() {
        // It's not obvious whether we should return the main archive usages or something else (like all possible usages).
        // For now, we'll return only the main module, as it is necessary to adapt standard java-library projects.

        main.finalizeValue();
        return main.get().getModule("main").getUsages();
    }
}
