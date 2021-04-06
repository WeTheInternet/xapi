package net.wti.gradle.schema.api;

import java.io.File;

/**
 * SchemaDirs:
 * <p>
 * <p>
 * A place to collect useful "default paths", based off a single base File: indexDir.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 04/04/2021 @ 2:56 a.m..
 */
public interface SchemaDirs {

    File getDirIndex();

    default File getDirByPpm() {
        return new File(getDirIndex(), "path");
    }

    default File getDirByGnv() {
        return new File(getDirIndex(), "coord");
    }
}
