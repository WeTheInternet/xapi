package xapi.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.internal.reflect.Instantiator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 2:56 AM.
 */
public class XapiExtensionDist extends XapiExtension {

    private final Property<String> distType;

    public XapiExtensionDist(Project project, Instantiator instantiator) {
        super(project, instantiator);
        distType = project.getObjects().property(String.class);
        distType.set("jar");
    }

    public Property<String> getDistType() {
        return distType;
    }

    public void setDistType(Provider<String> distType) {
        this.distType.set(distType);
    }

    public void setDistType(String distType) {
        this.distType.set(distType);
    }
}
