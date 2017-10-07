package xapi.dev.ui.tags;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.DynamicDeclarationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.visitor.DumpVisitor;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.In2;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/25/17.
 */
public class UiTagCallbackGenerator extends UiFeatureGenerator {

    private final String pkg;
    private final String name;
    private final UiTagGenerator owner;

    public UiTagCallbackGenerator(String pkg, String name, UiTagGenerator owner) {
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
        final In2<GeneratedUiImplementation, MethodBuffer> writer;
        final GeneratedUiComponent component = source.getGeneratedComponent();
        final Expression toPrint = service.resolveVar(
            source.getContext(),
            attr.getExpression()
        );
        if (!(toPrint instanceof DynamicDeclarationExpr)) {
            // TODO also handle lambdas
            throw new UnsupportedOperationException("ui callbacks presently only support dynamic declarations" +
                " (`public void myMethod(...)`)");
        }
        final String methodName;
        switch (attr.getNameString()) {
            case "onCreated":
                methodName = "afterCreatedCallback";
                break;
            case "onAttached":
                methodName = "attachedCallback";
                break;
            case "onDetached":
                methodName = "detachedCallback";
                break;
            case "onAttributeChanged":
                methodName = "attributeChangedCallback";
                break;
            default:
                throw new IllegalArgumentException("Cannot create callback for " + service.debugNode(attr));
        }
        writer = (impl, out)->{
            // TODO detect if the declaration is platform-specific or not,
            // so we can put the method into either the base, or the impl type.
            DynamicDeclarationExpr decl = (DynamicDeclarationExpr) toPrint;
            if ("gwt".equals(impl.getAttrKey())) {
                final ClassBuffer cb = impl.getSource().getClassBuffer();
                cb.printlns(decl.getBody().toSource());
                if (decl.getBody() instanceof MethodDeclaration) {
                    MethodDeclaration mthd = (MethodDeclaration) decl.getBody();
                    // TODO: actually lookup method definition and check expected argument types in some sane manner
                    out.println("component." + methodName + "(getUi, " + cb.getSimpleName() + "::" + mthd.getName()+");");
                } else {
                    throw new UnsupportedOperationException("on" + X_String.toTitleCase(methodName) + " only supports method declarations");
                }
            } else {
                throw new UnsupportedOperationException("on" + X_String.toTitleCase(methodName) + " not yet supported for " + impl.getAttrKey() + " components");
            }
        };

        component.getImpls()
            .forAll(GeneratedUiImplementation::registerCallbackWriter, writer);
        return UiVisitScope.FEATURE_NO_CHILDREN;
    }
}
