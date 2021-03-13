package net.wti.gradle.internal.require.api;

import net.wti.gradle.require.api.PlatformModule;
import org.gradle.api.Named;
import org.gradle.api.artifacts.Configuration;

/**
 * A lambda-able "only usable as a consumer" wrapper around a gradle {@link Configuration}.
 *
 * This lets us put our common consumer-based logic in one place,
 * and encourages a safer / subject-oriented API.
 *
 * If these ABIs breaks on upgrades,
 * it is likely because of an underlying change in foreign behavior;
 * that is, it's good to force you fix how you use this interface,
 * instead of slowly, silently failing because we didn't want to bother you.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2/22/19 @ 11:33 PM.
 */
public interface ConsumerConfiguration extends Named {

    default PlatformModule getConsumerCoords() {
        return getConsumerModule().asCoords();
    }

    ArchiveGraph getConsumerModule();

    @Override
    default String getName() {
        return getConsumerModule().getPath();
    }
}
