package net.wti.gradle.internal.api;

import org.gradle.api.internal.component.SoftwareComponentInternal;
import org.gradle.api.internal.component.UsageContext;

import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/10/18 @ 2:29 AM.
 */
public interface XapiVariant extends SoftwareComponentInternal {

    @Override
    Set<XapiUsageContext> getUsages();
}
