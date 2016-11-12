package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.collect.impl.SimpleLinkedList;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.api.ApiGeneratorTools;
import xapi.dev.ui.ContainerMetadata.MetadataRoot;
import xapi.fu.In2Out1;
import xapi.fu.Out2;
import xapi.source.X_Source;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/28/16.
 */
public abstract class UiGeneratorTools <Ctx extends ApiGeneratorContext<Ctx>> implements ApiGeneratorTools <Ctx> {

    protected final StringTo<Integer> numGenerated;
    protected final StringTo<UiComponentGenerator> componentGenerators;
    protected final StringTo<UiFeatureGenerator> featureGenerators;
    protected final SimpleLinkedList<UiVisitScope> scopes;

    public UiGeneratorTools() {
        numGenerated = X_Collect.newStringMap(Integer.class);
        componentGenerators = X_Collect.newStringMap(UiComponentGenerator.class);
        featureGenerators = X_Collect.newStringMap(UiFeatureGenerator.class);
        componentGenerators.addAll(getComponentGenerators());
        featureGenerators.addAll(getFeatureGenerators());
        scopes = new SimpleLinkedList<>();
    }

    protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
        return Arrays.asList(
              Out2.out2Immutable("app", new UiComponentGenerator()),
              Out2.out2Immutable("import", new UiComponentGenerator()),
              Out2.out2Immutable("button", new UiComponentGenerator())
        );
    }

    protected Iterable<Out2<String, UiFeatureGenerator>> getFeatureGenerators() {
        return Arrays.asList(
              Out2.out2Immutable("ref", new UiFeatureGenerator()),
              Out2.out2Immutable("title", new UiFeatureGenerator()),
              Out2.out2Immutable("body", new UiFeatureGenerator()),
              Out2.out2Immutable("data", new UiFeatureGenerator()),
              Out2.out2Immutable("file", new UiFeatureGenerator()),
              Out2.out2Immutable("text", new UiFeatureGenerator()),
              Out2.out2Immutable("onClick", new UiFeatureGenerator())
        );
    }

    public abstract UiGeneratorService getGenerator();

    public UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata) {
        String name = container.getName();
        final UiComponentGenerator scoped = scopes.findNotNullMappedReverse(
              scope -> scope.getComponentOverrides().get(name)
        );
        if (scoped != null) {
            return scoped;
        }
        return componentGenerators.get(name);
    }

    public UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        String name = container.getNameString();
        final UiFeatureGenerator scoped = scopes.findNotNullMappedReverse(
              scope -> scope.getFeatureOverrides().get(name)
        );
        if (scoped != null) {
            return scoped;
        }

        return featureGenerators.get(name);
    }

    public ContainerMetadata createMetadata(MetadataRoot root, UiContainerExpr n) {
        return getGenerator().createMetadata(root, n);
    }

    public String calculateGeneratedName(String pkgName, String className, UiContainerExpr expr) {
        String fqcn = X_Source.qualifiedName(pkgName, className);
        return "Component" + numGenerated.compute(fqcn, (k, i) -> i == null ? 0 : i++) + "_"+ className ;
    }

    public In2Out1<UiContainerExpr,ContainerMetadata,UiComponentGenerator> containerFilter(Set<UiContainerExpr> dataParents) {
        return (dom, meta)->{
            if (dataParents.contains(dom)) {
                final ContainerMetadata child = meta.createChild(dom, this);
                final UiComponentGenerator generator = new UiComponentGenerator();
                generator.setMetadata(child);
                return generator;
            } else {
                return null;
            }
        };
    }

    public UiGeneratorVisitor createVisitor(
          ContainerMetadata metadata
    ) {
        return getGenerator().createVisitor(metadata);
    }

}
