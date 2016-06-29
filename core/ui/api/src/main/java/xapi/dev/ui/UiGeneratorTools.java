package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.ui.ContainerMetadata.MetadataRoot;
import xapi.fu.In2Out1;
import xapi.fu.Out2;
import xapi.source.X_Source;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/28/16.
 */
public abstract class UiGeneratorTools {

    protected final StringTo<Integer> numGenerated;
    protected final StringTo<UiComponentGenerator> componentGenerators;
    protected final StringTo<UiFeatureGenerator> featureGenerators;

    public UiGeneratorTools() {
        numGenerated = X_Collect.newStringMap(Integer.class);
        componentGenerators = X_Collect.newStringMap(UiComponentGenerator.class);
        featureGenerators = X_Collect.newStringMap(UiFeatureGenerator.class);
        componentGenerators.addAll(getComponentGenerators());
        featureGenerators.addAll(getFeatureGenerators());
    }

    protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
        return Arrays.asList(
              Out2.out2("app", new UiComponentGenerator()),
              Out2.out2("import", new UiComponentGenerator()),
              Out2.out2("button", new UiComponentGenerator())
        );
    }

    protected Iterable<Out2<String, UiFeatureGenerator>> getFeatureGenerators() {
        return Arrays.asList(
              Out2.out2("ref", new UiFeatureGenerator()),
              Out2.out2("title", new UiFeatureGenerator()),
              Out2.out2("body", new UiFeatureGenerator()),
              Out2.out2("data", new UiFeatureGenerator()),
              Out2.out2("file", new UiFeatureGenerator()),
              Out2.out2("text", new UiFeatureGenerator()),
              Out2.out2("onClick", new UiFeatureGenerator())
        );
    }

    public abstract UiGeneratorService getGenerator();

    public UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata) {
        return componentGenerators.getOrCreate(container.getName(), n->getGenerator().getComponentGenerator(container, metadata));
    }

    public UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        return featureGenerators.getOrCreate(container.getNameString(), n->getGenerator().getFeatureGenerator(container, componentGenerator));
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
