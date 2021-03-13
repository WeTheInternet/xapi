package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.ArchiveRequest;
import net.wti.gradle.internal.require.api.ConsumerConfiguration;
import net.wti.gradle.internal.require.api.ProducerConfiguration;

public class DefaultArchiveRequest implements ArchiveRequest {
    private ProducerConfiguration from;
    private ConsumerConfiguration to;
    private ArchiveRequestType type;

    public DefaultArchiveRequest(ArchiveGraph from, ArchiveGraph to, ArchiveRequestType type) {
        this(()->from, ()->to, type);
    }

    public DefaultArchiveRequest(ProducerConfiguration from, ConsumerConfiguration to, ArchiveRequestType type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }

    @Override
    public ProducerConfiguration from() {
        return from;
    }

    @Override
    public ConsumerConfiguration to() {
        return to;
    }

    @Override
    public ArchiveRequestType type() {
        return type;
    }
}
