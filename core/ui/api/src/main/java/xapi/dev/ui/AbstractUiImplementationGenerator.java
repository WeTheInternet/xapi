package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.GeneratedUiComponent.GeneratedUiImplementation;
import xapi.dev.ui.InterestingNodeFinder.InterestingNodeResults;
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
    public GeneratedUiImplementation generateComponent(ContainerMetadata metadata, ComponentBuffer buffer) {
        final String pkgName = metadata.getControllerPackage();
        final String className = metadata.getControllerSimpleName();
        final UiContainerExpr expr = metadata.getUi();
        metadata.setControllerType(pkgName, className);
        final GeneratedUiImplementation impl = getImpl(buffer.getGeneratedComponent());
        metadata.setImplementation(impl);
        UiGeneratorVisitor visitor = createVisitor(metadata, buffer);

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
}
