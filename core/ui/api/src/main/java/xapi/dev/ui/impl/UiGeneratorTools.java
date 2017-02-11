package xapi.dev.ui.impl;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.collect.impl.SimpleLinkedList;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.api.ApiGeneratorTools;
import xapi.dev.ui.api.*;
import xapi.dev.ui.api.ContainerMetadata.MetadataRoot;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.api.UiNamespace.DefaultUiNamespace;
import xapi.dev.ui.tags.UiTagGenerator;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out2;
import xapi.source.X_Source;
import xapi.util.X_String;

import java.util.Arrays;
import java.util.Set;

import static xapi.fu.Out2.out2Immutable;

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
              out2Immutable("app", new UiComponentGenerator()),
              out2Immutable("import", new UiComponentGenerator()),
              out2Immutable("button", new UiComponentGenerator()),
              out2Immutable("define-tags", new UiTagGenerator()),
              out2Immutable("define-tag", new UiTagGenerator())
        );
    }

    protected Iterable<Out2<String, UiFeatureGenerator>> getFeatureGenerators() {
        return Arrays.asList(
              out2Immutable("ref", new UiFeatureGenerator()),
              out2Immutable("title", new UiFeatureGenerator()),
              out2Immutable("body", new UiFeatureGenerator()),
              out2Immutable("data", new UiFeatureGenerator()),
              out2Immutable("file", new UiFeatureGenerator()),
              out2Immutable("text", new UiFeatureGenerator()),
              out2Immutable("onClick", new UiFeatureGenerator())
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
        ContainerMetadata metadata, ComponentBuffer buffer
    ) {
        return getGenerator().createVisitor(metadata, buffer);
    }

    public In1Out1<Expression, Expression> varResolver(Ctx ctx) {
        return In2Out1.with1(this::resolveVar, ctx);
    }

    protected void initializeComponent(GeneratedUiComponent result) {
        final UiGeneratorService gen = getGenerator();
        if (gen != this && gen instanceof UiGeneratorTools) {
            ((UiGeneratorTools)gen).initializeComponent(result);
        }
    }

    public GeneratedUiComponent newComponent(String pkg, String className) {
        final GeneratedUiComponent component = new GeneratedUiComponent(pkg, className);
        initializeComponent(component);
        return component;
    }

    public UiNamespace namespace() {
        return DefaultUiNamespace.DEFAULT;
    }

    public UiNamespace getNamespace(GeneratedUiComponent component, boolean base) {
        UiNamespace ns = namespace();
        if (base) {
            return ns;
        }
        ns = component.getImpls()
            .reduceInstances(GeneratedUiImplementation::reduceNamespace, ns);
        return ns;
    }

    public String tagToJavaName(UiContainerExpr n) {
        String[] names = n.getName().split("-");
        return X_String.join("", X_String::toTitleCase, names);
    }

    public ComponentBuffer getComponentInfo(String name) {
        return getGenerator().getBuffer(name);
    }
}
