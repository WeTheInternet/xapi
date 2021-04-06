package net.wti.gradle.schema.api;

import org.gradle.api.Named;

/**
 * SchemaMetadata:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 16/03/2021 @ 3:07 a.m..
 */
public interface SchemaMetadata extends Named {

    @Override
    String getName();
}
