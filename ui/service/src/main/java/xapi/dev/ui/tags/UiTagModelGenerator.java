package xapi.dev.ui.tags;

import net.wti.lang.parser.ast.expr.DynamicDeclarationExpr;
import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.expr.JsonContainerExpr;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.type.ClassOrInterfaceType;
import net.wti.lang.parser.ast.type.Type;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import xapi.collect.api.IntTo;
import xapi.dev.lang.gen.*;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.Out1;
import xapi.fu.Out2;
import xapi.model.X_Model;
import xapi.model.api.KeyBuilder;
import xapi.model.api.ModelBuilder;
import xapi.model.api.ModelKey;
import xapi.string.X_String;

import java.lang.reflect.Modifier;

import static xapi.dev.ui.api.UiNamespace.*;
import static xapi.source.util.X_Modifier.DEFAULT;
import static xapi.source.util.X_Modifier.STATIC;

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
        final GeneratedTypeOwner component = me.getGeneratedComponent();

        generateModel(tools, me.getContext(), component, component.getPublicModel(), attr, false);

        return UiVisitScope.FEATURE_NO_CHILDREN;
    }

    public static void generateModel(
        UiGeneratorTools tools,
        ApiGeneratorContext ctx,
        GeneratedTypeOwner component,
        final GeneratedUiModel model,
        UiAttrExpr attr,
        boolean apiMode
    ) {
        // Need to refactor these a little, so they make sense for use by xapi.dev.ui.api.GeneratedApi.addModels()
        // for GeneratedApi, we'll likely want model related convenience methods in api+base,
        // but the actual GeneratedUiModel will need to be a newly created type for each invocation of this method
        // (ui components only have one public model, and it is bound to the api|base classes.
        final GeneratedTypeWithModel api = component.getApi();

        if (!apiMode) {
            component.getBase().ensureFieldDefined(model.getWrappedName(), "model", false);
        }

        tools.maybeAddImports(ctx, model, attr);
        final String modelName = model.getWrappedName();
        final ClassBuffer apiOut = api.getSource().getClassBuffer();
        // TODO: check for custom type hierarchy, and see if somebody defines getModel().
        // right now that is implicitly done by extending AbstractModelComponent

        // TODO: create an ApiWriter interface that we make callers pass in here;
        // anywhere we check apiMode goes into a method of that interface.
        int mod = apiMode ? STATIC : DEFAULT;

        final String modelType = X_String.firstCharToLowercase(model.getTypeName());
        final String apiType = X_String.firstCharToLowercase(api.getTypeName());
        MethodBuffer mthd = apiOut.createMethod(
            mod, String.class,
            "get" + (apiMode ? modelName + "Type" : "ModelType")
        ).returnValue("\"" + modelType + "\"");

        if (apiMode) {
            // TODO: move this into a ApiImplGenerator (wow, what a java-esque name!)
            // @JsProperty(namespace = "xapi.model", name = "newType")
            mthd.withAnnotation(JsProperty.class,
                Out2.out2Immutable("namespace", "\"" + apiType + ".model\""),
                Out2.out2Immutable("name", "\"" + model.getTypeName() + "\"")
            );
        }

        mthd = apiOut.createMethod(
            mod, modelName,
            "create" + (apiMode ? modelName : "Model"))
            .returnValue(apiOut.addImportStatic(X_Model.class, "create") + "(" + modelName + ".class)");

        if (apiMode) {
            // @JsMethod(namespace = "xapi.model", name = "newType")
            mthd.withAnnotation(JsMethod.class,
                Out2.out2Immutable("namespace", "\"" + apiType + ".model\""),
                Out2.out2Immutable("name", "\"new" + model.getTypeName() + "\"")
            );

        }

        boolean defaultImmutable = attr.hasAnnotationBool(true, "immutable");
        boolean defaultPublic = attr.hasAnnotationBool(true, "public");
        boolean defaultExposed = attr.hasAnnotationBool(true, "exposed");

        final Expression expr = attr.getExpression();
        if (expr instanceof JsonContainerExpr) {
            JsonContainerExpr json = (JsonContainerExpr) expr;

            final ClassBuffer modOut = model.getSource().getClassBuffer();
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
                    UiTagGenerator.printMember(tools, model, ctx, component, method);
                } else {
                    Type type = tools.methods().$type(tools, ctx, typeExpr).getType();
                    // TODO smart import lookups...
                    boolean isImmutable = defaultImmutable;
                    boolean isPublic = defaultPublic;
                    boolean isExposed = defaultExposed;
                    if (!isImmutable) {
                        isImmutable = pair.hasAnnotationBool(true, "immutable");
                    }
                    if (pair.hasAnnotationBool(false, "exposed")) {
                        isExposed = true;
                    }
                    if (pair.hasAnnotationBool(true, "public")) {
                        isPublic = true;
                    }
                    if (pair.hasAnnotationBool(true, "private")) {
                        isPublic = false;
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
                            String simple = typeSrc.split("<")[0];
                            switch (simple) {
                                case "IntTo":
                                    type = new ClassOrInterfaceType(IntTo.class.getName() + typeSrc.substring(5)) ;
                                    break;
                                case "ModelKey":
                                    type = new ClassOrInterfaceType(ModelKey.class.getName() + typeSrc.substring(8));
                                    break;
                                default:
                                    String qualified = modOut.getImports().qualify(simple);
                                    if (!qualified.equals(simple)) {
                                        type = new ClassOrInterfaceType(
                                            qualified +
                                                (typeSrc.contains("<") ? typeSrc.substring(simple.length()) : "")
                                        );
                                    }
                                    break;
                            }
                    }
                    final GeneratedUiMember field = model.addField(tools, type, rawFieldName, isImmutable);
                    // copy imports
                    field.copyImports(pair);
                    // check if we should make this field public or not...
                    if (!apiMode && isPublic) {
                        // api mode never exposes model fields because it does not have a getModel(); api is only a factory
                        String t = field.importType(apiOut);
                        // try to re-qualify the type t...
                        t = modOut.getImports().qualify(t);
                        t = apiOut.addImport(t);
                        apiOut
                            .createMethod(DEFAULT, t, field.getterName())
                            .returnValue("getModel()." + field.getterName() + "()");
                    }
                    if (isExposed) {
                        // add two-way binding from model fields to raw element
                        // (will only apply in web mode for now; may look into fxml support some day...

                    }
//                    modOut.getImports().reserveSimpleName(rawFieldName);
                }
            });

            // Create some basic utilities in the model interface
            String constantName = model.getConstantName();

            modOut.createField(String.class, "MODEL_" + constantName)
                .setInitializer("\"" + X_String.firstCharToLowercase(model.getTypeName()) + "\"");

            final String out1 = modOut.addImport(Out1.class);
            final String keyBuilder = modOut.addImport(KeyBuilder.class);
            final String modBuilder = modOut.addImport(ModelBuilder.class);
            final String forType = modOut.addImportStatic(KeyBuilder.class, "forType");
            final String build = modOut.addImportStatic(ModelBuilder.class, "build");
            final String create = modOut.addImportStatic(X_Model.class, "create");

            modOut.createField(out1 + "<" + keyBuilder+">", constantName + "_KEY_BUILDER")
                .setInitializer(forType + "(MODEL_" + constantName + ")");

            modOut.createMethod(Modifier.STATIC, keyBuilder, METHOD_NEW_MODEL_KEY)
                .returnValue(constantName + "_KEY_BUILDER" + ".out1()");

            modOut.createMethod(Modifier.STATIC, modBuilder + "<" + modelName + ">", METHOD_NEW_MODEL_BUILDER)
                .returnValue(constantName + "_MODEL_BUILDER" + ".out1()");

            modOut.createMethod(Modifier.STATIC, modelName, METHOD_NEW_MODEL)
                .returnValue(METHOD_NEW_MODEL_BUILDER + "().buildModel()");

            modOut.createField(out1 + "<" + modBuilder + "<" + modelName + ">>", constantName + "_MODEL_BUILDER")
                .getInitializer()
                .patternln("()->$1(newKey(), ()->$2($3.class));", build, create, modelName);

            if (apiMode) {
                component.addExtraLayer(model.getQualifiedName(), model);
            }

        } else {
            throw new IllegalArgumentException("<define-tag model={mustBe: Jso}  />; you sent " + tools.debugNode(attr));
        }
    }
}
