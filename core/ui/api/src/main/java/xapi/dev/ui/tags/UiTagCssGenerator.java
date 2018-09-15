package xapi.dev.ui.tags;

import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class UiTagCssGenerator extends UiFeatureGenerator {

    private final String pkg;
    private final String name;
    private final UiTagGenerator owner;

    public UiTagCssGenerator(String pkg, String name, UiTagGenerator owner) {
        this.pkg = pkg;
        this.name = name;
        this.owner = owner;
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {
        source.getGeneratedComponent().beforeSave(gen->{
            for (GeneratedUiImplementation impl : source.getGeneratedComponent().getImpls()) {
                impl.addCss(container, attr);
            }
        });
        return UiVisitScope.FEATURE_NO_CHILDREN;
    }
}
