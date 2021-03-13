package net.wti.gradle.internal.require.api;

import net.wti.gradle.require.api.PlatformModule;
import org.gradle.api.Named;
import org.gradle.api.artifacts.Configuration;

/**
 * An "only usable as a producer" wrapper around a gradle {@link Configuration}.
 *
 * This lets us put our common producer-based logic in one place,
 * and encourages a safer / subject-oriented API.
 *
 * If these ABIs breaks on upgrades,
 * it is likely because of an underlying change in foreign behavior;
 * that is, it's good to force you fix how you use this interface,
 * instead of slowly, silently failing because we didn't want to bother you.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2/22/19 @ 11:33 PM.
 */
public interface ProducerConfiguration extends Named {

    default PlatformModule getProducerCoords() {
        return getProducerModule().asCoords();
    }

    ArchiveGraph getProducerModule();

    @Override
    default String getName() {
        return getProducerModule().getPath();
    }
}
