package xapi.dev.components;

import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.GeneratedUiFactory;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.api.UiNamespace;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.fu.Lazy;
import xapi.inject.X_Inject;

import static xapi.source.X_Modifier.PUBLIC;
import static xapi.source.X_Modifier.STATIC;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/8/17.
 */
public class GeneratedWebComponent extends GeneratedUiImplementation {

    private final Lazy<UiNamespaceGwt> namespace;
    private FieldBuffer extractor;
    private MethodBuffer assemble;
    private FieldBuffer creator;

    public GeneratedWebComponent(GeneratedUiComponent ui) {
        super(ui, ui.getPackageName());
        namespace = Lazy.deferred1(()-> X_Inject.instance(UiNamespaceGwt.class));
    }

    @Override
    public String getAttrKey() {
        return "gwt";
    }

    @Override
    public UiNamespace reduceNamespace(UiNamespace from) {
        return namespace.out1();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void resolveNativeAttr(
        AssembledUi assembly, UiAttrExpr attr, AssembledElement el, MethodBuffer out
    ) {
        final UiGeneratorTools tools = assembly.getTools();
        final ApiGeneratorContext ctx = assembly.getContext();
        final String name = tools.resolveString(ctx, attr.getName());
        if (name.startsWith("on")) {
            // for web components, "on*" event handlers are handled "for free".
            // TODO: something generic to serialize lambdas we want to dump into source...
            final String func = tools.resolveString(ctx, attr.getExpression(), false, true);

            boolean capture = attr.getAnnotation(a->
                a.getNameString().toLowerCase().equals("capture") &&
                (
                    a.getMembers().isEmpty() ||
                    "true".equals(tools.resolveString(ctx, a.getMembers().first().getValue()) )
                )
            ).isPresent();

            out.println(".onCreated( e -> ").indent()
               .patternln("e.addEventListener(\"$1\", $2, $3)", name.substring(2), func, capture)
               .outdent().println(")");
            return;
        }
        super.resolveNativeAttr(assembly, attr, el, out);
    }

    @Override
    public void finalizeBuilder(GeneratedUiFactory builder) {
        final MethodBuffer builderMethod = getSource().getClassBuffer()
            .createMethod(PUBLIC | STATIC, builder.getQualifiedName(), UiNamespace.METHOD_BUILDER);
        builderMethod.patternln("return new $1<>($2, $3);"
                // $1
                , builderMethod.getReturnType()
                // $2
                , creator.getName()
                // $3
                , extractor.getName()
            );
    }

    public void setMetadata(MethodBuffer assemble, FieldBuffer creator, FieldBuffer extractor) {
        setAssemble(assemble);
        setCreator(creator);
        setExtractor(extractor);
    }

    public void setExtractor(FieldBuffer extractor) {
        this.extractor = extractor;
    }

    public FieldBuffer getExtractor() {
        return extractor;
    }

    public void setAssemble(MethodBuffer assemble) {
        this.assemble = assemble;
    }

    public MethodBuffer getAssemble() {
        return assemble;
    }

    public void setCreator(FieldBuffer creator) {
        this.creator = creator;
    }

    public FieldBuffer getCreator() {
        return creator;
    }
}
