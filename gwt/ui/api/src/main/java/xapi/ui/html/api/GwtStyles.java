package xapi.ui.html.api;

import xapi.fu.MappedIterable;
import xapi.ui.api.style.HasStyleResources;

import static xapi.fu.iterate.ArrayIterable.iterate;

import com.google.gwt.resources.client.ClientBundle;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/8/17.
 */
public interface GwtStyles extends HasStyleResources {

    Class<? extends ClientBundle>[] allBundles();

    @Override
    default MappedIterable<Class<?>> allResources() {
        return iterate(allBundles());
    }
}
