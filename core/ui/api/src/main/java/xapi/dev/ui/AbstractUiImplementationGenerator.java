package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.source.SourceBuilder;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/28/16.
 */
public abstract class AbstractUiImplementationGenerator extends UiGeneratorTools implements UiImplementationGenerator {
    protected UiGeneratorService generator;

    public AbstractUiImplementationGenerator() {
    }

    @Override
    public ContainerMetadata generateComponent(ContainerMetadata metadata, ComponentBuffer buffer) {
        final String pkgName = metadata.getControllerPackage();
        final String className = metadata.getControllerSimpleName();
        final UiContainerExpr expr = metadata.getContainer();
        metadata.setControllerType(pkgName, className);
        String generatedName = calculateGeneratedName(pkgName, className, expr);
        SourceBuilder b = new SourceBuilder("public class " + generatedName)
            .setPackage(pkgName)
            .setSuperClass(buffer.getBinder().getQualifiedName());
        metadata.setSourceBuilder(b);
        UiGeneratorVisitor visitor = createVisitor(metadata);

        visitor.visit(expr, this);
        return metadata;
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

}
