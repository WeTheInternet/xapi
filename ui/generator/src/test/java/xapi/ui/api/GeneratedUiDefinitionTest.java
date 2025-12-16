package xapi.ui.api;

import net.wti.lang.parser.ast.expr.AnnotationExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.type.ClassOrInterfaceType;
import org.junit.Test;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.ui.UiGeneratorServiceDefault;
import xapi.dev.ui.api.GeneratedUiDefinition;
import xapi.dev.ui.api.GeneratedUiField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/24/17.
 */
public class GeneratedUiDefinitionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void surviveRoundTrip() {

        GeneratedUiDefinition definition = new GeneratedUiDefinition(
            "xapi.ui.test",
            "TestType",
            "test-type",
            "RootTestType",
            "BaseTestType",
            "ModelType",
            "BuildTestType"
        );
        final GeneratedUiField field = new GeneratedUiField(new ClassOrInterfaceType("String"), "field");
        definition.getModelFields().put("field", field);
        final UiContainerExpr settings = definition.toSettings();
        final String serialized = settings.toSource();
        ApiGeneratorContext ctx = new ApiGeneratorContext<>();
        UiGeneratorServiceDefault tools = new UiGeneratorServiceDefault();
        final GeneratedUiDefinition restored = GeneratedUiDefinition.fromSettings(tools, ctx, settings);

        assertEquals("Did not survive round trip", definition, restored);
        restored.getModelFields().get("field").addAnnotation(AnnotationExpr.newMarkerAnnotation("Changed"));
        assertNotEquals(".equals() not working correctly", definition, restored);
    }
}
