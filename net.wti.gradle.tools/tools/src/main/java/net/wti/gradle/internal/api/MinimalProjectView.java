package net.wti.gradle.internal.api;

import org.gradle.api.plugins.ExtensionAware;

import java.io.File;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 29/07/19 @ 5:33 AM.
 */
public interface MinimalProjectView extends ExtensionAware {

    File getProjectDir();

    Object findProperty(String key);
}
