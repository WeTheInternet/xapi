package net.wti.gradle.schema.view;

import net.wti.gradle.schema.api.SchemaGraph;
import net.wti.gradle.schema.api.SchemaProject;

/**
 * IndexViewMutable:
 * <p><p>
 *      A mutable view of a fully-read schema.xapi index.
 * <p><p>
 *     This object allows adding, disabling, or otherwise mutating a {@link SchemaGraph}
 * <p><p>
 *     You are allowed to call any mutating methods up until {@link #resolve} is called (check {@link #isResolved} returns true!)
 * <p><p>
 * Created by James X. Nelson (James@WeTheInter.net) on 08/05/2021 @ 4:02 a.m..
 */
public interface IndexViewMutable extends ViewChain<IndexViewImmutable> {

    SchemaGraph getGraph();

    SchemaProject getProject(String path);
}