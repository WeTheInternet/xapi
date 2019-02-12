package net.wti.gradle.internal.require.impl;

import net.wti.gradle.internal.api.HasWork;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.GraphNode;
import org.gradle.api.Action;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2/9/19 @ 4:00 AM.
 */
public abstract class AbstractChildGraphNode <T extends HasWork, P extends GraphNode> extends AbstractBuildGraphNode<T> {

    private final P parent;

    public AbstractChildGraphNode(Class<T> type, P parent, ProjectView project) {
        super(type, project);
        this.parent = parent;
    }

    public P getParent() {
        return parent;
    }

    @Override
    public boolean whenReady(int priority, Action<Integer> newItem) {
        final boolean first = super.whenReady(priority, newItem);
        if (first) {
            parent.whenReady(priority, this::drainTasks);
        }
        return first;
    }
}
