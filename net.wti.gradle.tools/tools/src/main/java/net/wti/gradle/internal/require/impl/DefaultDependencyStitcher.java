package net.wti.gradle.internal.require.impl;

import net.wti.gradle.internal.require.api.DependencyStitcher;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 28/05/19 @ 3:30 AM.
 */
public class DefaultDependencyStitcher implements DependencyStitcher {

    private final DefaultArchiveGraph mod;

    public DefaultDependencyStitcher(DefaultArchiveGraph mod) {
        this.mod = mod;
    }
}
