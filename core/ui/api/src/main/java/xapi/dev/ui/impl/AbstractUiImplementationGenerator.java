package xapi.dev.ui.impl;

import com.github.javaparser.ast.TypeArguments;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.impl.InterestingNodeFinder.InterestingNodeResults;
import xapi.dev.ui.api.UiComponentGenerator.UiGenerateMode;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.source.X_Source;
import xapi.ui.api.component.*;
import xapi.util.X_String;
import xapi.util.api.RemovalHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/28/16.
 */
public abstract class AbstractUiImplementationGenerator <Ctx extends ApiGeneratorContext<Ctx>> extends UiGeneratorTools<Ctx> implements UiImplementationGenerator {
    protected UiGeneratorService generator;

    public AbstractUiImplementationGenerator() {
    }

    @Override
    public GeneratedUiImplementation generateComponent(
        ContainerMetadata metadata,
        ComponentBuffer buffer,
        UiGenerateMode mode
    ) {
        final String pkgName = metadata.getControllerPackage();
        final String className = metadata.getControllerSimpleName();
        final UiContainerExpr expr = metadata.getUi();
        metadata.setControllerType(pkgName, className);
        final GeneratedUiImplementation impl = getImpl(buffer.getGeneratedComponent());
        metadata.setImplementation(impl);
        UiGeneratorVisitor visitor = createVisitor(metadata, buffer);
        visitor.setMode(mode);
        visitor.visit(expr, this);
        return impl;
    }

    @Override
    public UiGeneratorVisitor createVisitor(ContainerMetadata metadata, ComponentBuffer buffer) {
        final UiGeneratorVisitor visitor = super.createVisitor(metadata, buffer);
        visitor.wrapScope(
        handler->
            scope -> {
                scopes.add(scope);
                final RemovalHandler undo = handler.io(scope);
                return ()->{
                    undo.remove();
                    UiVisitScope popped = scopes.pop();
                    assert popped == scope : "Scope stack inconsistent; " +
                          "expected " + popped + " to be the same reference as " + scope;
                };
            }
        );
        return visitor;
    }

    protected abstract String getImplPrefix();

    @Override
    public String getImplName(String pkgName, String className) {
        return X_Source.qualifiedName(pkgName, getImplPrefix() + className);
    }

    @Override
    public String calculateGeneratedName(
          String pkgName, String className
    ) {
        return getImplPrefix() + super.calculateGeneratedName(pkgName, className);
    }

    public UiGeneratorService getGenerator() {
        return generator;
    }

    @Override
    public UiGeneratorTools getTools() {
        return this;
    }

    public void setGenerator(UiGeneratorService generator) {
        this.generator = generator;
    }

    @Override
    public UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata) {
        final UiComponentGenerator gen = super.getComponentGenerator(container, metadata);
        if (gen == null) {
            return getGenerator().getComponentGenerator(container, metadata);
        }
        return gen;
    }

    @Override
    public UiFeatureGenerator getFeatureGenerator(
          UiAttrExpr container, UiComponentGenerator componentGenerator
    ) {
        final UiFeatureGenerator gen = super.getFeatureGenerator(container, componentGenerator);
        if (gen == null) {
            return getGenerator().getFeatureGenerator(container, componentGenerator);
        }
        return gen;
    }

    @Override
    public void spyOnInterestingNodes(
        ComponentBuffer component, InterestingNodeResults interestingNodes
    ) {

    }

    @Override
    public void spyOnNewComponent(ComponentBuffer component) {

    }

    @Override
    protected void initializeComponent(GeneratedUiComponent result, ContainerMetadata metadata) {
        if(shouldInitialize(result)) {
            if (!X_String.isEmpty(getImplPrefix())) {
                getImpl(result).setPrefix(getImplPrefix());
            }
            super.initializeComponent(result, metadata);
            standardInitialization(result, metadata);
        }
    }

    protected void standardInitialization(GeneratedUiComponent result, ContainerMetadata metadata) {

        final UiNamespace baseNs = getTools().namespace();
        final GeneratedUiApi api = result.getApi();
        final GeneratedUiBase base = result.getBase();

        final Maybe<String> model = metadata.getUi().getAttribute("model")
            .mapNullSafe(attr->{
                final GeneratedUiModel mod = api.getModel();
                for (AnnotationExpr anno : attr.getAnnotations()) {
                    if ("Named".equalsIgnoreCase(anno.getNameString())) {

                        String name = getTools().resolveString(new ApiGeneratorContext(), anno.getMembers().first().getValue());
                        mod.overrideName(name);
                        return name;
                    }
                }
                return mod.getWrappedName();
            })
            .lazy();

        final ClassBuffer baseOut = base.getSource().getClassBuffer();

        final String eleName = base.getElementType(baseNs);
        List<Type> types = new ArrayList<>();

        final ClassOrInterfaceType rawEle = new ClassOrInterfaceType(eleName);
        final ClassOrInterfaceType apiType = new ClassOrInterfaceType(api.getWrappedName());
        types.add(rawEle);
        apiType.setTypeArguments(TypeArguments.withArguments(types));
        // provide local definitions for api.  TODO: also provide base type now?
        result.addGlobalDefinition(UiNamespace.TYPE_API, new ReferenceType(apiType));

        final String abstractCompPkg;
        final String abstractCompName;
        if (model.isPresent()) {
            abstractCompPkg = AbstractModelComponent.class.getPackage().getName();
            abstractCompName = AbstractModelComponent.class.getSimpleName();
        } else {
            abstractCompPkg = AbstractComponent.class.getPackage().getName();
            abstractCompName = AbstractComponent.class.getSimpleName();

        }
        final GeneratedUiSupertype superType = new GeneratedUiSupertype(result, abstractCompPkg, abstractCompName,
            new GeneratedTypeParameter(UiNamespace.TYPE_ELEMENT, new TypeParameter(eleName)),
            new GeneratedTypeParameter(UiNamespace.TYPE_API, new TypeParameter(UiNamespace.TYPE_API, apiType))
        );
        if (model.isPresent()) {

            final String baseModel = base.getModelType(baseNs);
            final ClassOrInterfaceType modelType = new ClassOrInterfaceType(baseModel);

            final GeneratedTypeParameter modelParam = new GeneratedTypeParameter(UiNamespace.TYPE_MODEL,
                new TypeParameter(baseModel, modelType));
            superType.addTypeParameter(1, modelParam);
            baseOut.makeAbstract();
            base.addLocalDefinition(UiNamespace.TYPE_MODEL, new ReferenceType(modelType));
        }
        result.updateSupertype(superType);

        superType.requireConstructor(new Parameter(UiNamespace.TYPE_ELEMENT, UiNamespace.VAR_ELEMENT));

        final String o1 = baseOut.addImport(Out1.class);
        ClassOrInterfaceType out1 = new ClassOrInterfaceType(o1);
        out1.setTypeArguments(TypeArguments.withArguments(rawEle));
        superType.requireConstructor(new Parameter(out1, UiNamespace.VAR_ELEMENT));

        types = new ArrayList<>();
        types.add(rawEle);
        types.add(apiType);


        final String opts;
        if (api.hasModel()) {
            opts = baseOut.addImport(ModelComponentOptions.class);
        } else {
            opts = baseOut.addImport(ComponentOptions.class);

        }

        String ctor = baseOut.addImport(ComponentConstructor.class);

        final ClassOrInterfaceType ctorType = new ClassOrInterfaceType(ctor);
        ctorType.setTypeArguments(TypeArguments.withArguments(types));

        if (model.isPresent()) {
            types = new ArrayList<>(types);
            final ClassOrInterfaceType modelType = new ClassOrInterfaceType(
                api.getModelNameQualified()
            );
            types.add(1, modelType);
        }
        final ClassOrInterfaceType optsType = new ClassOrInterfaceType(opts);
        optsType.setTypeArguments(TypeArguments.withArguments(types));

        superType.requireConstructor(
            new Parameter(optsType, "opts"),
            new Parameter(ctorType, "ctor")
        );

        final String apiEle = api.getElementType(baseNs);
        final GeneratedTypeParameter apiEleParam = result.addGeneric(
            UiNamespace.TYPE_ELEMENT,
            new TypeParameter(apiEle)
        );
        apiEleParam.setExposed(true);
        final Class<? extends IsComponent> componentType;
        if (api.hasModel()) {
            componentType = IsModelComponent.class;
        } else {
            componentType = IsComponent.class;
        }

        String isCompPkg = componentType.getPackage().getName();
        String isCompName = componentType.getSimpleName();
        api.getSource().addImport(componentType);
        final GeneratedUiSupertype superInterface = new GeneratedUiSupertype(result, isCompPkg, isCompName,
            new GeneratedTypeParameter(UiNamespace.TYPE_ELEMENT, apiEleParam.getType())
        );
        if (api.hasModel()) {
            final String apiModel = api.getModelType(baseNs);
            final ClassOrInterfaceType modelType = new ClassOrInterfaceType(apiModel);
            final GeneratedTypeParameter apiModelParam = result.addGeneric(
                UiNamespace.TYPE_MODEL,
                new TypeParameter(apiModel, modelType)
            );
            superInterface.addTypeParameter(1, apiModelParam);
            api.addLocalDefinition(UiNamespace.TYPE_MODEL, new ReferenceType(modelType));
        }

        result.addSuperInterface(superInterface);
    }

    @Override
    public UiGenerateMode getMode(ComponentBuffer component, ContainerMetadata metadata) {
        String name = metadata.getUi().getName();
        if (name.startsWith("define-tag")) {
            return UiGenerateMode.TAG_DEFINITION;
        }
        if (name.startsWith("model")) {
            return UiGenerateMode.MODEL_BUILDING;
        }
        if (name.startsWith("web-app")) {
            return UiGenerateMode.WEB_APP_BUILDING;
        }
        return UiGenerateMode.UI_BUILDING;
    }

    @Override
    public UiComponentGenerator createTagGenerator() {
        final UiGeneratorTools tools = generator.tools();
        return tools == this ? super.createTagGenerator() : tools.createTagGenerator();
    }
}
