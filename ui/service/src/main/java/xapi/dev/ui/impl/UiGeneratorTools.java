package xapi.dev.ui.impl;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.collect.simple.SimpleLinkedList;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.lang.gen.ApiGeneratorTools;
import xapi.dev.ui.api.*;
import xapi.dev.ui.api.MetadataRoot;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.api.UiNamespace.DefaultUiNamespace;
import xapi.dev.ui.tags.UiTagGenerator;
import xapi.fu.*;
import xapi.fu.data.SetLike;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.CachingIterator.ReplayableIterable;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.SizedIterable;
import xapi.source.X_Source;
import xapi.string.X_String;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static xapi.collect.X_Collect.newSet;
import static xapi.fu.Out2.out2Immutable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/28/16.
 */
public abstract class UiGeneratorTools <Ctx extends ApiGeneratorContext<Ctx>> implements ApiGeneratorTools <Ctx> {

    protected final StringTo<Integer> numGenerated;
    protected final Lazy<StringTo<UiComponentGenerator>> componentGenerators;
    protected final StringTo<UiFeatureGenerator> featureGenerators;
    protected final SimpleLinkedList<UiVisitScope> scopes;
    private final ChainBuilder<Do> roundEndListener;
    private final ChainBuilder<Do> roundStartListener;
    protected final SetLike<GeneratedUiComponent> seen;
    protected final SetLike<GeneratedApi> apis;

    public UiGeneratorTools() {
        seen = newSet(GeneratedUiComponent.class).asSetLike();
        apis = newSet(GeneratedApi.class).asSetLike();

        numGenerated = X_Collect.newStringMap(Integer.class);
        componentGenerators = Lazy.deferred1(()->{
            final StringTo<UiComponentGenerator> generators = X_Collect.newStringMap(UiComponentGenerator.class);
            // we use a lazy here so we can defer this initialization (gives caller a chance to mutate us)
            generators.addAll(getComponentGenerators());
            return generators;
        });
        featureGenerators = X_Collect.newStringMap(UiFeatureGenerator.class);
        featureGenerators.addAll(getFeatureGenerators());
        scopes = new SimpleLinkedList<>();
        roundEndListener = Chain.startChain();
        roundStartListener = Chain.startChain();
    }

    protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
        return Arrays.asList(
              out2Immutable("app", new UiComponentGenerator()),
              out2Immutable("import", new UiComponentGenerator()),
              out2Immutable("button", new UiComponentGenerator()),
              out2Immutable("define-tags", createTagGenerator()),
              out2Immutable("define-tag", createTagGenerator())
        );
    }

    public UiComponentGenerator createTagGenerator() {
        return new UiTagGenerator();
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
        return componentGenerators.out1().get(name);
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

    public String calculateGeneratedName(String pkgName, String className) {
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

    protected boolean shouldInitialize(GeneratedUiComponent result) {
        return !seen.contains(result);
    }

    protected void initializeComponent(GeneratedUiComponent result, ContainerMetadata metadata) {
        if (seen.addIfMissing(result)) {
            result.setRecommendedImports(metadata.getRecommendedImports());
            final UiGeneratorService gen = getGenerator();
            if (gen != this && gen instanceof UiGeneratorTools) {
                UiGeneratorTools tools = (UiGeneratorTools) gen;
                // hideous recursion sickness is avoided for now, using set semantics...
                // generator service and impl generators will virally call each other's
                // initializeComponent methods, exactly once.
                // Use shouldInitialize(result) to guard calls into here
                tools.initializeComponent(result, metadata);
            }
        }
    }

    public GeneratedUiComponent newComponent(String pkg, String className, ContainerMetadata metadata) {
        final GeneratedUiComponent component = new GeneratedUiComponent(pkg, className, metadata::getUi);
        initializeComponent(component, metadata);
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

    public GeneratedUiDefinition getDefinition(Ctx ctx, String name) {
        return getGenerator().getComponentDefinition(ctx, name);
    }

    public void onRoundStart(Do task) {
        synchronized (roundStartListener) {
            roundStartListener.add(task);
        }
    }

    public void onRoundComplete(Do task) {
        synchronized (roundEndListener) {
            roundEndListener.add(task);
        }
    }

    public Do.Closeable startRound(String id, GeneratedUiComponent component) {
        clearTasks(roundStartListener);
        return this::finishRound;
    }

    private void clearTasks(ChainBuilder<Do> tasks) {
        while (!tasks.isEmpty()) {
            final SizedIterable<Do> jobs;
            // We only use this on final variables,
            // and this synchronized is why this method is private, instead of a utility method
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (tasks) {
                jobs = tasks.cached();
                tasks.clear();
            }
            jobs.forAll(Do::done);
        }
    }

    public void finishRound() {
        clearTasks(roundEndListener);
    }

    public SizedIterable<String> getImplPrefixes() {
        // TODO: not this...  for now, you can override to inject these.
        // we'll have a more formal solution relying on xapi.settings later...
        return ArrayIterable.iterate("gwt", "javafx");
    }

    public String getComponentType(Expression nodeToUse, Type memberType) {
        if (Boolean.TRUE.equals(nodeToUse.getExtra(UiConstants.EXTRA_FOR_LOOP_VAR))) {
            // When we are a for loop var, then we are either an array or an collection.
            // For now, we'll just handle the collection case.
            if (memberType instanceof ClassOrInterfaceType) {
                final List<Type> typeArgs = ((ClassOrInterfaceType) memberType).getTypeArgs();
                if (typeArgs.size() != 1) {
                    throw new IllegalArgumentException("For loop var " + debugNode(nodeToUse) + " " +
                        "must have a type with exactly one type argument; you sent " + debugNode(memberType));
                }
                return typeArgs.get(0).toSource();
            } else {
                assert false : "Not tested / implemented";
                return memberType.toSource();
            }
        } else {
            return memberType.toSource();
        }
    }

    // TODO: add a collector for things which need to emit values to settings.xapi
}
