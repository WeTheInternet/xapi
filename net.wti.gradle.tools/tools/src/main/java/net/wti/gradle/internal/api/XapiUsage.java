package net.wti.gradle.internal.api;

import org.gradle.api.attributes.Usage;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/10/18 @ 2:29 AM.
 */
public interface XapiUsage extends Usage {
    String SOURCE_PREFIX = "xapi-source";
    String SOURCE = SOURCE_PREFIX;
    String SOURCE_JAR = SOURCE_PREFIX + "-jar";
    String INTERNAL = "xapi-internal";
}
