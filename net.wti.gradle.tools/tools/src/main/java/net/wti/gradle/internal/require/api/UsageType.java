package net.wti.gradle.internal.require.api;

import org.gradle.api.Named;
import org.gradle.api.artifacts.Configuration;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2/5/19 @ 11:58 PM.
 */
public interface UsageType extends Named {

    Configuration findConfig(ArchiveGraph module, boolean only);

}
