package xapi.dev.ui.layout;

/**
 * A special class of ui generator designed for assembling instructions
 * on how to generate runtime layout code for a give set of components.
 *
 * The initial version of this will be moreso for proof-of-concept prototyping,
 * with an end-goal to be able to do things like runtime binding of properties,
 * layout / resize event firing and shadow dom encapsulation.
 *
 * Eventually, define-tag components will generate a layout generator
 * for their given type, so we'll want everything to be as cookie-cutter
 * as possible (with an API for supported features).
 *
 * Our initial use case will be for handling slotted components and
 * binding fields in parent models to child components.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 6/11/17.
 */
public interface LayoutGenerator {

    String getTagName();

}
