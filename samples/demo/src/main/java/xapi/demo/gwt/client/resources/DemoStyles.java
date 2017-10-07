package xapi.demo.gwt.client.resources;

import xapi.ui.html.api.GwtStyles;

import com.google.gwt.resources.client.ClientBundle;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
class DemoStyles implements GwtStyles {

    final DemoResources res;

    public DemoStyles(DemoResources res) {
        this.res = res;
    }

    @Override
    public Class<? extends ClientBundle>[] allBundles() {
        return new Class[]{DemoResources.class};
    }
}
