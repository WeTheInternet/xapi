package xapi.jre.ui.impl.feature;

import net.wti.lang.parser.ASTHelper;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import javafx.geometry.Pos;
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
public class JavaFxAlignFeatureGenerator extends UiFeatureGenerator {

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

        final String align = container.getUi().getAttribute("align")
              .mapDeferred(ASTHelper::extractAttrValue)
              .ifAbsentReturn("center").toLowerCase();

        // default align in javafx is ugly; only use it if null is explicitly sent.
        if (!"null".equals(align)) {
            String pos = mb.addImport(Pos.class);
            mb.print(panel + ".setAlignment(" + pos + ".");
            switch (align) {
                case "center":
                    mb.println(Pos.CENTER + ");");
                    break;
                case "centerleft":
                    mb.println(Pos.CENTER_LEFT + ");");
                    break;
                case "centerright":
                    mb.println(Pos.CENTER_RIGHT + ");");
                    break;
                case "top":
                    mb.println(Pos.TOP_CENTER + ");");
                    break;
                case "topleft":
                    mb.println(Pos.TOP_LEFT + ");");
                    break;
                case "topright":
                    mb.println(Pos.TOP_RIGHT + ");");
                    break;
                case "bottom":
                    mb.println(Pos.BOTTOM_CENTER + ");");
                    break;
                case "bottomleft":
                    mb.println(Pos.BOTTOM_LEFT + ");");
                    break;
                case "bottomright":
                    mb.println(Pos.BOTTOM_RIGHT + ");");
                    break;
                case "left":
                    mb.println(Pos.BASELINE_LEFT + ");");
                    break;
                case "right":
                    mb.println(Pos.BASELINE_RIGHT + ");");
                    break;
                case "middle":
                    mb.println(Pos.BASELINE_CENTER + ");");
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable align attribute value: " + align);
            }
        }


        return super.startVisit(service, generator, source, container, attr);
    }
}
