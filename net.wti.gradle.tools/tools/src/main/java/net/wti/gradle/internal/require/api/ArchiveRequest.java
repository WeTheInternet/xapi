package net.wti.gradle.internal.require.api;

import org.gradle.api.Named;

/**
 * Highly experimental "basically psuedocode" for an object that models a dependency between archives.
 *
 * We want to make this something that is aware of source / transformed artifacts and platform selection.
 *
 * Leaving it simple for now, so we can wait for use cases to drive design decisions.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/2/19 @ 4:24 AM.
 */
public interface ArchiveRequest extends Named {
    /**
     * Describes a give request type; current version should be viewed as "roadmap" rather than "intended design".
     */
    enum ArchiveRequestType {
        /**
         * Process sources/classpath into new source files
         */
        TRANSPILE("=>"),
        /**
         * Process sources/classpath into binary output files
         */
        COMPILE("->"),
        /**
         * A development runtime (will include sources / private tools).
         * Will prefer class directories on unix machines, and jars/zips on windows.
         * Will include sources and tools like runtime injection support.
         *
         */
        RUNTIME_DEV("==>"),
        /**
         * A strict production runtime (minified archives / no extras).
         * Will use jars/zips unless explicitly configured otherwise.
         * Will avoid tools like runtime injection support unless explicitly added.
         * May also attempt AoT compilation if prod jvm args are supplied.
         */
        RUNTIME_PROD("-->"),
        /**
         * A whole-world "shadow archive" for a given platform.
         */
        DISTRIBUTION("*")
        ;
        private final String symbol;

        ArchiveRequestType(String symbol) {
            this.symbol = symbol;
        }
    }

    /**
     * An ArchiveRequest models a dependency as being from a producer, to a consumer.
     *
     * Data (files) from() the producer are provided to() the consumer.
     *
     * @return the producer who is expected to produce dependencies.
     */
    ProducerConfiguration from();


    /**
     * An ArchiveRequest models a dependency as being from a producer, to a consumer.
     *
     * Data (files) from() the producer are provided to() the consumer.
     *
     * @return the consumer who is expected to reference dependency files.
     */
    ConsumerConfiguration to();

    /**
     * A request may earmark particular dependencies for a particular purpose.
     *
     * This functionality is not yet hooked up to anything.
     *
     * @return an ArchiveRequestType enum member of our particular dependency relationship
     */
    ArchiveRequestType type();

    @Override
    default String getName() {
        return from().getName() + type().symbol + to().getName();
    }

    default boolean isSelectable() {
        return from().getProducerModule().isSelectable() || to().getConsumerModule().isSelectable();
    }
}
