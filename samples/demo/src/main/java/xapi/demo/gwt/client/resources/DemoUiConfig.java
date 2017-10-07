package xapi.demo.gwt.client.resources;

import xapi.components.impl.AbstractUiConfig;
import xapi.elemental.api.ElementalService;
import xapi.ui.html.api.GwtStyles;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
public class DemoUiConfig extends AbstractUiConfig<DemoStyles> {

    @Override
    protected CssResource getCss(DemoStyles r) {
        return styleBundle.res.demo();
    }

    @Override
    protected Class<? extends CssResource> cssType() {
        return DemoCss.class;
    }

    @Override
    protected Class<? extends DemoStyles> resourceType() {
        return DemoStyles.class;
    }

    @Override
    protected AbstractUiConfig<DemoStyles> duplicate() {
        return new DemoUiConfig(service, styleBundle.res);
    }

    public DemoUiConfig(ElementalService service, DemoResources res) {
        super(service, new DemoStyles(res));
    }
}


