package xapi.dev.ui.tags;

import com.github.javaparser.ast.expr.DynamicDeclarationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import xapi.collect.api.IntTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.model.X_Model;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;
import xapi.model.api.ModelKey;

import java.lang.reflect.Modifier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class UiTagModelGenerator extends UiFeatureGenerator {

    private final String pkg;
    private final String name;
    private final UiTagGenerator owner;

    public UiTagModelGenerator(String pkg, String name, UiTagGenerator owner) {
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
        final GeneratedUiApi api = component.getApi();
        final GeneratedUiBase base = component.getBase();
        final GeneratedUiModel model = api.getModel();
        final ApiGeneratorContext ctx = me.getContext();

        owner.maybeAddImports(tools, ctx, model, attr);
        final String modelName = api.getModelName();
        final In1Out1<Type, String> apiFactory = Type::toSource;
        final In1Out1<Type, String> baseFactory = Type::toSource;
        api.addExtension("pkgName", api.getModelName(),
            component.getBase().getModelName(), apiFactory, baseFactory);
        final ClassBuffer out = api.getSource().getClassBuffer();
        // TODO: check for custom type hierarchy, and see if somebody defines getModel().
        // right now that is implicitly done by extending AbstractModelComponent


       out.createMethod("default String getModelType()")
            .returnValue("\"" + api.getModel().getTypeName() + "\"");

       out.createMethod("default " + modelName + " createModel()")
            .returnValue(out.addImportStatic(X_Model.class, "create") + "(" + modelName + ".class)");

        base.ensureFieldDefined(model.getWrappedName(), "model", false);

        boolean immutable = attr.getAnnotation(anno->anno.getNameString().equalsIgnoreCase("immutable")).isPresent();
        final Expression expr = attr.getExpression();
        if (expr instanceof JsonContainerExpr) {
            JsonContainerExpr json = (JsonContainerExpr) expr;
            final GeneratedUiModel apiModel = api.getModel();

            final ClassBuffer modOut = api.getModel().getSource().getClassBuffer();
            json.getPairs().forEach(pair->{
                String rawFieldName = tools.resolveString(ctx, pair.getKeyExpr());
                if (rawFieldName.matches("[0-9]+")) {
                    // TODO smarter type->name inference for field names...
                    rawFieldName = "val" + rawFieldName;
                }

                final Expression typeExpr = tools.resolveVar(ctx, pair.getValueExpr());
                if (typeExpr instanceof DynamicDeclarationExpr) {
                    // Must be a default method.
                    DynamicDeclarationExpr method = (DynamicDeclarationExpr) typeExpr;
                    owner.printMember(tools, api.getModel(), me, method);
                } else {
                    Type type = tools.methods().$type(tools, ctx, typeExpr).getType();
                    // TODO smart import lookups...
                    boolean isImmutable = immutable;
                    if (!isImmutable) {
                        isImmutable = pair.getAnnotation(anno -> anno.getNameString().equalsIgnoreCase(
                            "immutable")).isPresent();
                    }
                    switch (type.toSource()) {
                        case "IntTo":
                            type = new ClassOrInterfaceType(IntTo.class.getName());
                            break;
                        case "ModelKey":
                            type = new ClassOrInterfaceType(ModelKey.class.getName());
                            break;
                        default:
                            final String typeSrc = type.toSource();
                            switch (typeSrc.split("<")[0]) {
                                case "IntTo":
                                    type = new ClassOrInterfaceType(IntTo.class.getName() + typeSrc.substring(5)) ;
                                    break;
                                case "ModelKey":
                                    type = new ClassOrInterfaceType(ModelKey.class.getName() + typeSrc.substring(8));
                                    break;
                            }
                    }
                    apiModel.addField(tools, type, rawFieldName, isImmutable);
                    modOut.getImports().reserveSimpleName(rawFieldName);
                }
            });

            // Create some basic utilities in the model interface
            String constantName = api.getConstantName();
            String modelField = api.getModelFieldName();
            String modelType = api.getModelName();

            modOut.createField(String.class, "MODEL_" + constantName)
                .setInitializer("\"" + modelField + "\"");

            final String out1 = modOut.addImport(Out1.class);
            final String keyBuilder = modOut.addImport(KeyBuilder.class);
            final String modBuilder = modOut.addImport(ModelBuilder.class);
            final String forType = modOut.addImportStatic(KeyBuilder.class, "forType");
            final String build = modOut.addImportStatic(ModelBuilder.class, "build");
            final String create = modOut.addImportStatic(X_Model.class, "create");

            modOut.createField(out1 + "<" + keyBuilder+">", constantName + "_KEY_BUILDER")
                .setInitializer(forType + "(MODEL_" + constantName + ")");

            modOut.createMethod(Modifier.STATIC, keyBuilder, "newKey")
                  .returnValue(constantName + "_KEY_BUILDER" + ".out1()");

            modOut.createField(out1 + "<" + modBuilder + "<" + modelType + ">>", constantName + "_MODEL_BUILDER")
                .getInitializer()
                .println("()->")
                .indentln(build + "(" + constantName + "_KEY_BUILDER.out1(),")
                .indentln("()->" + create + "(" + modelType + ".class));");

        } else {
            throw new IllegalArgumentException("<define-tag model={mustBe: Jso}  />; you sent " + tools.debugNode(attr));
        }

        return UiVisitScope.FEATURE_NO_CHILDREN;
    }
}
