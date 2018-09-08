package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.In2Out1;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.Out2;
import xapi.source.X_Source;
import xapi.util.X_String;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains basic information about a generated component (type name,
 * model name + fields, etc).
 *
 * The contents of these definitions are added to settings.xapi,
 * using the following form:
 *
 * <code>
     <settings

     components = [
         <tag
             name="input-text"
             package="xapi.ui.edit"
             type="InputText"
             model= @Named("ModelInputText") {
                 value: String.class,
                 title: @Nullable String.class,
                 placeholder: @Nullable String.class
             }
         /tag>
     ]

     /settings>
 *
 * </code>
 *
 * Created by James X. Nelson (james @wetheinter.net) on 5/23/17.
 */
public class GeneratedUiDefinition {

    @NotNull
    private String tagName;
    @NotNull
    private String typeName;
    @NotNull
    private String packageName;
    private String modelName;
    @NotNull
    private final StringTo<GeneratedUiField> modelFields;

    private String apiName;
    private String baseName;
    private String builderName;

    public GeneratedUiDefinition() {
        modelFields = X_Collect.newStringMapInsertionOrdered(GeneratedUiField.class);
    }

    public GeneratedUiDefinition(
        String packageName,
        String typeName,
        String tagName,
        String apiName,
        String baseName,
        String modelName,
        String builderName
    ) {
        this();
        this.packageName = packageName;
        this.typeName = typeName;
        this.tagName = tagName;
        this.apiName = apiName;
        this.baseName = baseName;
        this.modelName = modelName;
        this.builderName = builderName;
        // TODO: accept / record api type parameter information as well, so we don't make assumptions
        // about expected types (currently, a lot of code assumes CompName<El>, meaning always 1 param of type Element).
    }

    public UiContainerExpr toSettings() {
        final NameExpr name = new NameExpr("tag");
        final List<UiAttrExpr> attributes = new ArrayList<>();
        final List<Expression> items = new ArrayList<>();
        final UiBodyExpr body = new UiBodyExpr(-1, -1, -1, -1, items);
        UiContainerExpr tag = new UiContainerExpr(-1,-1,-1,-1, name, attributes, body, false);
        tag.addAttribute(false, UiAttrExpr.of("name", tagName));
        tag.addAttribute(false, UiAttrExpr.of("package", packageName));
        tag.addAttribute(false, UiAttrExpr.of("type", typeName));
        tag.addAttribute(false, UiAttrExpr.of("apiName", apiName));
        tag.addAttribute(false, UiAttrExpr.of("baseName", baseName));
        tag.addAttribute(false, UiAttrExpr.of("builderName", builderName));
        if (modelFields.isNotEmpty()) {
            final List<JsonPairExpr> fields = new ArrayList<>();
            for (Out2<String, GeneratedUiField> field : modelFields.forEachItem()) {
                String fieldName = field.out1();
                final GeneratedUiField fieldValue = field.out2();
                final Type fieldType = fieldValue.getMemberType();
                if (fieldType.getAnnotations() == null || fieldType.getAnnotations().isEmpty()) {
                    List<AnnotationExpr> annos = new ArrayList<>();
                    for (AnnotationExpr anno : fieldValue.getAnnotations()) {
                        annos.add(anno);
                    }
                    fieldType.setAnnotations(annos);
                }
                fields.add(JsonPairExpr.of(fieldName, new TypeExpr(fieldType)));
            }

            final Expression model = new JsonContainerExpr(false, fields);
            final UiAttrExpr modelAttr = UiAttrExpr.of("model", model);
            tag.addAttribute(false, modelAttr);
            if (!modelName.equals("Model" + typeName)) {
                modelAttr.setAnnotations(Collections.singletonList(
                    AnnotationExpr.newSingleMemberAnnotation("Named", StringLiteralExpr.stringLiteral(modelName))
                ));
            }
        }
        return tag;
    }

    public static <Ctx extends ApiGeneratorContext<Ctx>> GeneratedUiDefinition
    fromSettings(UiGeneratorTools<Ctx> tools, Ctx ctx, UiContainerExpr tag) {
        final GeneratedUiDefinition definition = new GeneratedUiDefinition();
        final UiAttrExpr nameAttr = tag.getAttributeNotNull("name");
        final UiAttrExpr packageAttr = tag.getAttributeNotNull("package");
        final UiAttrExpr type = tag.getAttributeNotNull("type");
        final UiAttrExpr apiAttr = tag.getAttributeNotNull("apiName");
        final UiAttrExpr baseAttr = tag.getAttributeNotNull("baseName");
        final UiAttrExpr builderAttr = tag.getAttributeNotNull("builderName");
        final Maybe<UiAttrExpr> modelAttr = tag.getAttribute("model");

        final String name = tools.resolveString(ctx, nameAttr.getExpression());
        final String packageName = tools.resolveString(ctx, packageAttr.getExpression());
        final String typeName = tools.resolveString(ctx, type.getExpression());
        final String apiName = tools.resolveString(ctx, apiAttr.getExpression());
        final String baseName = tools.resolveString(ctx, baseAttr.getExpression());
        final String builderName = tools.resolveString(ctx, builderAttr.getExpression());

        definition.setTagName(name);
        definition.setPackageName(packageName);
        definition.setTypeName(typeName);
        definition.setApiName(apiName);
        definition.setBaseName(baseName);
        definition.setBuilderName(builderName);
        if (modelAttr.isPresent()) {
            final UiAttrExpr model = modelAttr.get();
            final Expression modelExpr = model.getExpression();
            if (!(modelExpr instanceof JsonContainerExpr)) {
                throw new IllegalArgumentException(
                    "A model= expression must be a {json: container}; you sent " + tag
                );
            }

            // If there is an @Named annotation, use that for the model name
            final Maybe<AnnotationExpr> named = model.getAnnotation(
                AnnotationExpr::getNameString,
                "Named"::equalsIgnoreCase
            );
            final String modelName = named
                .mapNullSafe(AnnotationExpr::getMembers)
                .mapNullSafe(MappedIterable::first)
                .mapNullSafe(MemberValuePair::getValue)
                .mapNullSafe(In2Out1.with1(tools::resolveString, ctx))
                .ifAbsentSupply(() -> "Model" + typeName);

            definition.setModelName(modelName);
            final List<JsonPairExpr> items = ((JsonContainerExpr) modelExpr).getPairs();

            for (JsonPairExpr item : items) {
                String fieldName = item.getKeyString();
                final TypeExpr fieldType = tools.methods().$type(tools, ctx, item.getValueExpr());
                final List<AnnotationExpr> annotations = item.getAnnotations();
                GeneratedUiField field = new GeneratedUiField(fieldType.getType(), fieldName);
                field.addAnnotations(annotations);
                definition.getModelFields().put(fieldName, field);
            }
        }
        return definition;
    }

    public static UiAttrExpr serialize(MappedIterable<GeneratedUiDefinition> items) {
        return UiAttrExpr.of("components", JsonContainerExpr.jsonArray(
            items.map(GeneratedUiDefinition::toSettings)
        ));
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getModelName() {
        return modelName;
    }

    public String getQualifiedModelName() {
        return modelName == null ? null : modelName.indexOf('.') == -1 ?
            X_Source.qualifiedName(getPackageName(), modelName) : modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public StringTo<GeneratedUiField> getModelFields() {
        return modelFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final GeneratedUiDefinition that = (GeneratedUiDefinition) o;

        if (!tagName.equals(that.tagName))
            return false;
        if (!typeName.equals(that.typeName))
            return false;
        if (!packageName.equals(that.packageName))
            return false;
        if (modelName != null ? !modelName.equals(that.modelName) : that.modelName != null)
            return false;
        if (!modelFields.equals(that.modelFields))
            return false;
        if (apiName != null ? !apiName.equals(that.apiName) : that.apiName != null)
            return false;
        if (baseName != null ? !baseName.equals(that.baseName) : that.baseName != null)
            return false;
        return builderName != null ? builderName.equals(that.builderName) : that.builderName == null;
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + typeName.hashCode();
        result = 31 * result + packageName.hashCode();
        result = 31 * result + (modelName != null ? modelName.hashCode() : 0);
        result = 31 * result + modelFields.hashCode();
        result = 31 * result + (apiName != null ? apiName.hashCode() : 0);
        result = 31 * result + (baseName != null ? baseName.hashCode() : 0);
        result = 31 * result + (builderName != null ? builderName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GeneratedUiDefinition{" +
            "tagName='" + tagName + '\'' +
            ", typeName='" + typeName + '\'' +
            ", packageName='" + packageName + '\'' +
            ", modelName='" + modelName + '\'' +
            ", modelFields=" + modelFields +
            ", apiName='" + apiName + '\'' +
            ", baseName='" + baseName + '\'' +
            ", builderName='" + builderName + '\'' +
            '}';
    }

    public String getQualifiedName() {
        return X_Source.qualifiedName(getPackageName(), getTypeName());
    }

//    public MethodCallExpr getTagFactory(
//        UiGeneratorTools tools,
//        ApiGeneratorContext ctx,
//        GeneratedUiComponent other,
//        UiNamespace namespace,
//        UiContainerExpr ui
//    ) {
//        // Our tag factory should be a method generated onto the base class;
//        // if the source tag supplied a model or a style, we must pass those references in as parameters.
////        final GeneratedUiBase myBase = me.getBase();
////        String myBaseName = myBase.getWrappedName();
//
//        GeneratedUiBase otherBase = other.getBase();
//        final String myBaseName = otherBase.getSource().addImport(getBaseName());
//        final Maybe<UiAttrExpr> model = ui.getAttribute("model");
//        final Maybe<UiAttrExpr> style = ui.getAttribute("style");
//        final String name = "create" + myBaseName;
//        MethodCallExpr call = new MethodCallExpr(null, name);
//        final Type returnType = tools.methods().$type(
//            tools,
//            ctx,
//            StringLiteralExpr.stringLiteral(myBaseName)
//        ).getType();
//        GeneratedUiMethod method = new GeneratedUiMethod(returnType, name);
//        method.setSource(ui);
//        method.setContext(ctx);
//        final String elBuilder = otherBase.getElementBuilderType(namespace);
//        final MethodBuffer creator = other.getBase().getSource().getClassBuffer()
//            .createMethod("public abstract " + elBuilder + " " + name);
//        final List<Expression> args = new ArrayList<>();
//        RequiredMethodType type = CREATE;
//        if (model.isPresent()) {
//            type = CREATE_FROM_MODEL;
//            args.add(tools.resolveVar(ctx, model.get().getExpression()));
//            method.addParam(getModelName(), "model");
//            creator.addParameter(getModelName(), "model");
//        }
//        if (style.isPresent()) {
//            type = type == CREATE_FROM_MODEL ? CREATE_FROM_MODEL_AND_STYLE : CREATE_FROM_STYLE;
//            final Expression resolvedStyle = tools.resolveVar(ctx, style.get().getExpression());
//            args.add(resolvedStyle);
//            final String styleType = namespace.getBaseStyleResourceType(CanAddImports.NO_OP);
//            method.addParam(styleType, "style");
//            creator.addParameter(styleType, "style");
//        }
//        method.setType(type);
//
//        call.setArgs(args);
//
//        other.getImpls().forAll(GeneratedUiImplementation::requireMethod, type, method, call);
//        return call;
//    }

    public String getBaseName() {
        return baseName == null ? "Base" + getApiName() : baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getApiName() {
        return apiName == null ? getTypeName() + "Component" : apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getModelKeyConstant() {
        return "MODEL_" + X_String.toConstantName(getTypeName());
    }

    public void setBuilderName(String builderName) {
        this.builderName = builderName;
    }

    public String getBuilderName() {
        return builderName;
    }
}
