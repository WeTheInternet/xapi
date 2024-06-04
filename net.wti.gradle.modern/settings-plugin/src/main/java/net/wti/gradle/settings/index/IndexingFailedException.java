package net.wti.gradle.settings.index;

import org.gradle.api.GradleException;

import javax.annotation.Nullable;

/**
 * IndexingFailedException:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 07/05/2021 @ 2:04 a.m..
 */
public class IndexingFailedException extends GradleException {

    public IndexingFailedException(final String message) {
        super(message);
    }

    public IndexingFailedException(final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}

