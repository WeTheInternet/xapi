package xapi.components.api;

import xapi.ui.api.StyleAssembler;
import xapi.ui.api.StyleService;
import xapi.ui.api.style.HasStyleResources;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/8/17.
 */
public interface UiConfig
    <
        Element,
        StyleElement,
        R extends HasStyleResources,
        Service extends StyleService<
            StyleElement,
            ? super R
        >
    > {

    void addStyleAssembler(StyleAssembler<Element, StyleElement, R, Service> assembler);

    R getResources();

    StyleAssembler<Element,StyleElement,R,Service> getDefaultAssembler();
}
