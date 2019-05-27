package net.wti.gradle.schema.api;

import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Named;
import org.gradle.internal.HasInternalProtocol;

/**
 * A configuration object to describe the available platforms within the gradle build.
 *
 * A platform describes a particular "native" implementation layer,
 * like javafx vs. gwt vs. j2cl vs. vert.x vs. appengine.
 *
 * All code within a given platform should be *mutually exclusive* with other platforms
 * (except when a platform extends another platform).
 *
 * Hopefully most of your code can live in the "main" or core platform,
 * which is where all of your apis, abstractions and general logic tools live.
 * Your Platform specific code should be responsible for implementing all the spis needed by your apis and tools.
 *
 * If you instead want to add another conceptual "archive type" to a / all platforms,
 * see {@link ArchiveConfig}, which you can add to your root {@link XapiSchema},
 * or to individual PlatformConfigs.
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 2:32 PM.
 */
@HasInternalProtocol
public interface PlatformConfig extends Named {

    PlatformConfig getParent();

    default PlatformConfig getRoot() {
        final PlatformConfig parent = getParent();
        return parent == null ? this : parent;
    }

    ArchiveConfigContainer getArchives();

    ArchiveConfig getMainArchive();

    default ArchiveConfig getArchive(Object named) {
        final String name = GradleCoerce.unwrapString(named);
        return getArchives().maybeCreate(name);
    }

    default ArchiveConfig findArchive(Object named) {
        final String name = GradleCoerce.unwrapString(named);
        return getArchives().findByName(name);
    }

    /**
     * Set requireSource = true to cause all non-source dependencies
     * to have their source-equivalent artifacts requested.
     *
     * When those dependencies are inter-project dependencies,
     * this will also trigger the creation of source archives.
     *
     * The default is false, but this property is transitively inherited,
     * so you should feel free to be explicit if you really don't want source archives created.
     *
     * @param requires
     */
    void setRequireSource(Object requires);

    boolean isTest();

    boolean isPublished();

    void setPublished(Object test);

    void setTest(Object test);

    void replace(CharSequence named);

    default void replace(PlatformConfig named) {
        replace(named.getName());
    }

    void addChild(PlatformConfig platform);

    boolean isRoot();

    boolean isRequireSource();

}
