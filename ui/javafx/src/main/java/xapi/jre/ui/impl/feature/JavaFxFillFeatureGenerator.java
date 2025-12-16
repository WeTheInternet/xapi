package xapi.jre.ui.impl.feature;

import net.wti.lang.parser.ASTHelper;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import javafx.scene.layout.Region;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/1/16.
 */
public class JavaFxFillFeatureGenerator extends UiFeatureGenerator {

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {

        String panel = container.peekPanelName();
        final MethodBuffer mb = container.getMethod(panel);
        final String fill = container.getUi().getAttribute("fill")
              .mapDeferred(ASTHelper::extractAttrValue)
              .ifAbsentReturn("null").toLowerCase();

        if (!"null".equals(fill)) {
            String region = mb.addImport(Region.class);
            boolean bindWidth = false;
            boolean bindHeight = false;
            switch (fill) {
                case "both":
                    bindHeight = true;
                    // fallthrough
                case "width":
                    bindWidth = true;
                    break;
                case "height":
                    bindHeight = true;
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable fill attribute value: " + fill);
            }
            String parent = container.newVarName("parent");
            mb.println(region + " " + parent + " = (" + region + ")" + panel + ".getParent();");
            if (bindWidth) {
               mb.println(panel + ".prefWidthProperty().bind(" + parent + ".prefWidthProperty());");
            }
            if (bindHeight) {
               mb.println(panel + ".prefHeightProperty().bind(" + parent + ".prefHeightProperty());");

            }
        }

        return super.startVisit(service, generator, source, container, attr);
    }
}
