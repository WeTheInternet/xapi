package net.wti.gradle.system.impl;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.system.api.TaskSpy;
import org.gradle.api.Task;

/**
 * ImmutableTaskSpy:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 10/04/2021 @ 1:03 a.m..
 */
public final class ImmutableTaskSpy implements TaskSpy {

    private final MinimalProjectView view;

    public ImmutableTaskSpy(final MinimalProjectView view) {
        this.view = view;
    }

    @Override
    public MinimalProjectView getView() {
        return view;
    }
}
