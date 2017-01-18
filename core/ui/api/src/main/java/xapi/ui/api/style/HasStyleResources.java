package xapi.ui.api.style;

import xapi.fu.MappedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/8/17.
 */
public interface HasStyleResources {

    MappedIterable<Class<?>> allResources();

}
