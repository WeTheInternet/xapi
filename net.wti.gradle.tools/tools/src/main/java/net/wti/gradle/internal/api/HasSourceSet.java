package net.wti.gradle.internal.api;

import org.gradle.api.tasks.SourceSet;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2/4/19 @ 1:23 AM.
 */
public interface HasSourceSet {
    SourceSet getSources();
}
