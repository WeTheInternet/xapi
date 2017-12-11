package xapi.elemental.api;

import elemental2.dom.Node;
import org.junit.Test;
import xapi.ui.api.ElementBuilder;

import static org.junit.Assert.assertEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/10/17.
 */
public class ElementalBuilderTest {

    @Test
    public void testToHtml() {
        final ElementBuilder<Node> b = new ElementalBuilder<>()
            .withChild("div", d->d.setName("one"))
            .withChild2("span", ElementBuilder::setId, "two");
        assertEquals("<div name='one'></div><span id='two'></span>", b.toSource());
    }

}
