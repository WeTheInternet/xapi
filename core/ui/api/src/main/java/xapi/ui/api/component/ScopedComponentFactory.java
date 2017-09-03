package xapi.ui.api.component;

import xapi.platform.CommonPlatforms;
import xapi.scope.api.Scope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/13/17.
 */
public interface ScopedComponentFactory <Opts extends ComponentOptions, Component extends IsComponent> {

    default double getScore(Scope test) {
        final CommonPlatforms platform = test.get(CommonPlatforms.class);
        if (platform == null) {
            return 0.5;
        }
        if (platform == getPlatform()) {
            return 1;
        }
        return 0;
    }

    default CommonPlatforms getPlatform() {
        return CommonPlatforms.Jre;
    }

    Component createComponent(Opts opts);
}
