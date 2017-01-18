package xapi.ui.api;

import xapi.fu.In1Out1;
import xapi.ui.api.style.HasStyleResources;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/6/16.
 */
public interface StyleAssembler
    <Element,
    StyleElement,
    Bundle extends HasStyleResources,
    Service extends StyleService
        <StyleElement, ? super Bundle>
    > {

    In1Out1<Element, StyleElement> styleInjector(Service service);

}
