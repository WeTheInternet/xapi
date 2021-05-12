package xapi.dev.ui.tags;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.api.GeneratedJavaFile;
import xapi.dev.api.GeneratedUiMember;
import xapi.dev.api.GeneratedUiModel;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.LocalVariable;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.dev.ui.tags.assembler.ModelBindingAssembler;
import xapi.dev.ui.tags.assembler.TagAssembler;
import xapi.source.util.X_Modifier;
import xapi.source.read.JavaModel.IsTypeDefinition;

import static xapi.source.X_Source.javaQuote;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class UiTagAssemblerGenerator extends UiFeatureGenerator {

    private final String pkg;
    private final String name;
    private final UiTagGenerator owner;

    public UiTagAssemblerGenerator(String pkg, String name, UiTagGenerator owner) {
        this.pkg = pkg;
        this.name = name;
        this.owner = owner;
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools tools,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata me,
        UiAttrExpr attr
    ) {
        final GeneratedUiComponent component = me.getGeneratedComponent();
        final GeneratedUiLayer api = component.getApi();
        final GeneratedUiModel model = api.getModel();
        final ApiGeneratorContext ctx = me.getContext();
        tools.maybeAddImports(ctx, model, attr);

        final Expression resolved = tools.resolveVar(
            source.getContext(),
            attr.getExpression()
        );
        if (resolved instanceof BooleanLiteralExpr) {
            if (((BooleanLiteralExpr) resolved).getValue()) {
                // The user wants a generated assembler.
                final GeneratedJavaFile layer = component.getOrCreateExtraLayer(
                    "assembler",
                    "xapi.dev.gen.assembler",
                    component.getTypeName()
                );

                layer.setSuffix("Assembler");
                layer.setType(IsTypeDefinition.newClass(pkg, layer.getWrappedName()));


                boolean hasModel = component.hasPublicModel();
                final ClassBuffer cb = layer.getSource().getClassBuffer()
                    // TODO: make a different superclass for non-model tags.
                    .setSuperClass(ModelBindingAssembler.class)
                    .addInterface(TagAssembler.class)
                ;

                final FieldBuffer assembly = cb.createField(
                    AssembledUi.class,
                    "assembly",
                    X_Modifier.PRIVATE_FINAL
                );
                assembly.addGetter(X_Modifier.PROTECTED)
                    .addAnnotation(Override.class);
                assembly
                    .createConstructor(X_Modifier.PUBLIC);

                final MethodBuffer definition = cb.createMethod(
                    X_Modifier.PROTECTED,
                    GeneratedUiDefinition.class,
                    "definition"
                )
                    .addAnnotation(Override.class);

                component.beforeSave(service -> {
                    final LocalVariable local = definition.newVariable(GeneratedUiDefinition.class, "def");
                    local.initConstructorLns(
                        "\n  " + javaQuote(component.getPackageName())
                        + "\n  ," + javaQuote(component.getTypeName())
                        + "\n  ," + javaQuote(component.getTagName())
                        + "\n  ," + javaQuote(component.getApi().getWrappedName())
                        + "\n  ," + javaQuote(component.getBase().getWrappedName())
                        + "\n  ," + (hasModel ?
                            javaQuote(component.getPublicModel().getWrappedName()) : "null")
                        + "\n  ," + (component.hasFactory() ?
                            javaQuote(component.getFactory().getWrappedName()) : "null")
                    );
                    if (hasModel) {
                        local.println("def.onEarly(() -> {").indent();
                        for (GeneratedUiMember field : model.getFields().values()) {
                            local.print("addField");
                            boolean nullable = field.getAnnotations().anyMatch(expr->expr.getNameString().equalsIgnoreCase("Nullable"));
                            if (nullable) {
                                local.print("Nullable");
                            }
                            String member = field.getMemberType().toSource();
                            // Try to turn short names back into qualified names
                            member = model.getSource().getImports().qualify(member);
                            local.patternln("($1, $2);",
                                javaQuote(member),
                                javaQuote(field.getMemberName())
                            );
                        }
                        local.outdent().println("});");
                    }
                    definition.returnValue(local.getName());
                });

            }
        } else {
            // TODO: support dynamic methods / [arrays thereof]
            throw new UnsupportedOperationException("assembler= only supports boolean arguments; you sent " + tools.debugNode(attr.getExpression()));
        }

        return UiVisitScope.FEATURE_NO_CHILDREN;
    }
}
