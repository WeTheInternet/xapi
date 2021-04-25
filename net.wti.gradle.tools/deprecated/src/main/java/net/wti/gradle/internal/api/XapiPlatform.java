package net.wti.gradle.internal.api;

import net.wti.gradle.publish.api.PublishedModule;
import net.wti.gradle.publish.api.PublishedModuleContainer;
import org.gradle.api.Action;
import org.gradle.api.Named;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/10/18 @ 2:53 AM.
 */
public class XapiPlatform implements Named {

    private final String name;
    private final PublishedModuleContainer usages;

    public XapiPlatform(
        ProjectView view,
        String name
    ) {
        this.name = name;
        this.usages = new PublishedModuleContainer(view, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public PublishedModule getModule(String name) {
        return usages.maybeCreate(name);
    }

    public boolean isEmpty() {
        return usages.isEmpty();
    }

    public void whenModuleAdded(Action<? super PublishedModule> callback) {
        usages.whenObjectAdded(callback);
    }

    public PublishedModuleContainer getModules() {
        return usages;
    }
}
