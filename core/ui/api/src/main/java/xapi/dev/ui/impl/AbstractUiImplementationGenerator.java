package xapi.dev.ui.impl;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.api.*;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.impl.InterestingNodeFinder.InterestingNodeResults;
import xapi.dev.ui.api.UiComponentGenerator.UiGenerateMode;
import xapi.util.X_String;
import xapi.util.api.RemovalHandler;

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
    public String calculateGeneratedName(
          String pkgName, String className, UiContainerExpr expr
    ) {
        return getImplPrefix() + super.calculateGeneratedName(pkgName, className, expr);
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
    protected void initializeComponent(GeneratedUiComponent result) {
        if (!X_String.isEmpty(getImplPrefix())) {
            getImpl(result).setPrefix(getImplPrefix());
        }
        super.initializeComponent(result);
    }

    @Override
    public UiGenerateMode getMode(ComponentBuffer component, ContainerMetadata metadata) {
        if (metadata.getUi().getName().startsWith("define-tag")) {
            return UiGenerateMode.TAG_DEFINITION;
        }
        if (metadata.getUi().getName().startsWith("model")) {
            return UiGenerateMode.MODEL_BUILDING;
        }
        return UiGenerateMode.UI_BUILDING;
    }
}
