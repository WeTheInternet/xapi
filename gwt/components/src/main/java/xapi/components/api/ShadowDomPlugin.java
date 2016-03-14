package xapi.components.api;

import elemental.dom.Element;

/**
 * Used to apply a process to an element's shdaowRoot, after it is created.
 *
 * Although the API allows you to return any element,
 * it is only tested when you return the shadow root that was supplied to you.
 *
 * The fluent return pattern is simply to make it easier to generate code.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/13/16.
 */
public interface ShadowDomPlugin {

  Element transform(Element shadowRoot);

}
