package xapi.demo.gwt.client;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import org.junit.Before;
import org.junit.Test;
import xapi.dev.resource.impl.StringDataResource;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.source.DomBuffer;
import xapi.fu.Out2;
import xapi.fu.itr.SizedIterable;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.source.X_Source;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
public class SlideRendererTest {

    SizedIterable<Out2<StringDataResource, UiContainerExpr>> items;

    @Before
    public void before() {
        if (items == null) {
            ClasspathScanner scanner = X_Inject.instance(ClasspathScanner.class);
            final ClasspathResourceMap results = scanner
                .matchResource(".*.xapi")
                .scanPackage("xapi.demo.content")
                .scan(getClass().getClassLoader());
            items = results.getAllResources()
                   .mapUnsafe(res->{
                        final UiContainerExpr container = JavaParser.parseUiContainer(res.getResourceName(), res.readAll());
                        sanityCheck(res.getResourceName(), container);
                        return Out2.out2Immutable(res, container);
                   })
                   .cached();
            assertTrue(items.hasAtLeast(3));
        }
    }

    @Test
    public void testAllSlidesCanRender() throws IOException, ParseException {

        // Make sure all of our slides can be parsed
        items.forAll(item->{
            SlideRenderer renderer = new SlideRenderer();
            final DomBuffer into = new DomBuffer()
                .setNewLine(true)
                .setTrimWhitespace(false);
            renderer.renderSlide(into, item.out2());
            X_Log.info(X_Source.pathToLogLink(item.out1().getResourceName()),
                "\n", item.out2(),
                "\nRenders:", into,
                "\n\n"
            );
        });
    }

    private void sanityCheck(String resourceName, UiContainerExpr container) {
        // ensure slides are sane (no unrecognized tags or attributes)
        assertEquals("root of slide " + X_Source.pathToLogLink(resourceName) + " must be xapi-slide", "xapi-slide", container.getName());

    }

}
