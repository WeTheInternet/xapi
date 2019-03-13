package net.wti.gradle.internal.api;

import net.wti.gradle.internal.require.api.ArchiveGraph;
import org.gradle.api.internal.component.UsageContext;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/10/18 @ 2:33 AM.
 */
public interface XapiUsageContext extends UsageContext {
    String getConfigurationName();

    ArchiveGraph getModule();
}
