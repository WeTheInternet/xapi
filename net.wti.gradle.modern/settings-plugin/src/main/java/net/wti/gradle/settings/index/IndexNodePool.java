package net.wti.gradle.settings.index;

import net.wti.gradle.api.BuildCoordinates;
import net.wti.gradle.settings.api.ModuleIdentity;
import net.wti.gradle.settings.api.PlatformModule;
import net.wti.gradle.settings.impl.ModuleIdentityImmutable;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.In3Out1;
import xapi.fu.data.MapLike;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;

/**
 * IndexNodePool:
 * <p>
 * <p>Each IndexNode MUST only get created once per project~platform:module
 * <p>
 * <p>This class is, for now, just a thin wrapper around a map.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 14/06/2024 @ 8:31 p.m.
 */
public class IndexNodePool {
    private final MapLike<ModuleIdentity, IndexNode> allNodes;
    private final MapLike<ModuleIdentity, IndexNode> deletedNodes;
    private final MapLike<String, PlatformModule> platMods;
    private final MapLike<String, ModuleIdentity> allIdentities;
    private static final In1Out1<ModuleIdentity, IndexNode> nodeFactory = IndexNode::new;
    private static final In3Out1<BuildCoordinates, String, PlatformModule, ModuleIdentity> modFactory = ModuleIdentityImmutable::new;
    private static final In2Out1<BuildCoordinates, String, In1Out1<PlatformModule, ModuleIdentity>> fromPlatMod =
            (coords, path) -> modFactory.supply1(coords).supply1(path);
    private final In1Out1<ModuleIdentity, IndexNode> getNodeMapper = this::getNode;

    public IndexNodePool() {
        allNodes = X_Jdk.mapHashConcurrent();
        deletedNodes = X_Jdk.mapHashConcurrent();
        platMods = X_Jdk.mapHashConcurrent();
        allIdentities = X_Jdk.mapHashConcurrent();
    }

    public ModuleIdentity getIdentity(BuildCoordinates coords, String path, PlatformModule platMod) {
        String key = ModuleIdentity.toKey(coords, path, platMod);
        return allIdentities.computeIfAbsent(key, ()->new ModuleIdentityImmutable(coords, path, platMod));
    }
    public In1Out1<PlatformModule, IndexNode> nodeFactory(BuildCoordinates coords, String path) {
        return fromPlatMod.io(coords, path).mapOut(getNodeMapper);
    }

    public IndexNode getNode(ModuleIdentity forIdentity) {
        return allNodes.computeIfAbsent(forIdentity, nodeFactory);
    }

    public IndexNode getDeletedNode(ModuleIdentity forIdentity) {
        return deletedNodes.get(forIdentity);
    }

    public PlatformModule getPlatformModule(String platform, String module) {
        return platMods.computeIfAbsent(platform + ":" + module, ()->new PlatformModule(platform, module));
    }

    public SizedIterable<IndexNode> getAllNodes() {
        return allNodes.mappedValues();
    }

    public boolean isDeleted(final ModuleIdentity identity) {
        return deletedNodes.has(identity);
    }
    public void delete(final ModuleIdentity identity) {
        // we probably just want to mark this as disabled, but we'll try reducing giant-map-size first.
        final IndexNode was = allNodes.remove(identity);
        if (was != null) {
            deletedNodes.put(identity, was);
            was.getLivenessReasons().clear();
            was.setDeleted(true);
            // when we delete a node, we should also remove ourselves from any outgoing dependencies.
            for (IndexNode dependency : was.getAllDependencies()) {
                // tell our dependency we no longer depend on them
                if (dependency.removeOutgoing(was)) {
                    // if this outgoing dependency removal was the last thing keeping the dependency alive...
                    if (dependency.isIncludeOnly() && !dependency.hasOutgoing()) {
                        // ...then delete the dependency as well
                        delete(dependency.getIdentity());
                        // this recursion allows us to iterate each node once, and still remove all empty outgoing edges
                    }
                }
            }

        }
    }
}
