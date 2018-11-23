package xapi.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.internal.provider.DefaultPropertyState;
import xapi.gradle.api.Freezable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/5/18 @ 2:38 AM.
 */
public class FreezingLockProperty <T> extends DefaultPropertyState<T> {

    private final Project project;

    public FreezingLockProperty(Project project, Class<T> cls) {
        super(cls);
        this.project = project;
    }

    @Override
    public void finalizeValue() {
        T current = getOrNull();
        if (current instanceof Freezable) {
            ((Freezable) current).freeze(project, current);
        }
        super.finalizeValue();
    }

}
