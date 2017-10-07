package xapi.demo.gwt.client.resources;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
public interface DemoResources extends ClientBundle {

    @Source("Demo.gss")
    DemoCss demo();

    @Source("xapi/demo/content/home.xapi")
    TextResource getHome();

    @Source("xapi/demo/content/Declarative.xapi")
    TextResource getDeclarative();

    @Source("xapi/demo/content/-lang.xapi")
    TextResource getLang();
}
