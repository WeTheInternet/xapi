package xapi.dev.ui.api;

import com.github.javaparser.ast.plugin.NodeTransformer;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.debug.NameGen;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.itr.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/26/18.
 */
public class MetadataRoot {

    private final StringTo<Integer> nameCounts;
    private final ClassTo<Lazy<?>> factories;
    private final StringTo<StringTo<NodeTransformer>> fieldRenames;
    private final NameGen names;
    private SizedIterable<String> recommendedImports;
    private ApiGeneratorContext<?> ctx;
    private GeneratedUiComponent generatedComponent;

    public MetadataRoot() {
        nameCounts = X_Collect.newStringMap(Integer.class);
        factories = X_Collect.newClassMap(Lazy.class);
        fieldRenames = X_Collect.newStringDeepMap(NodeTransformer.class);
        names = initNames();
    }

    public NameGen getNames() {
        return names;
    }

    protected NameGen initNames() {
        return NameGen.getGlobal();
    }

    public String newVarName(String prefix) {
        final Integer cnt;
        synchronized (nameCounts) {
            cnt = nameCounts.compute(prefix, (k, was) -> was == null ? 0 : was + 1);
        }
        if (cnt == 0) {
            return prefix;
        }
        return prefix + "_" + cnt;
    }

    public void reserveName(String refName) {
        Integer was = nameCounts.put(refName, 0);
        assert was == null : "Tried to reserve a name, `" + refName + "` more than once\n" +
            "Existing items: " + nameCounts;
    }

    public void registerFieldMapping(String ref, String fieldName, NodeTransformer accessor) {
        fieldRenames.get(ref).put(fieldName, accessor);
    }

    public NodeTransformer findReplacement(String ref, String var) {
        if (fieldRenames.containsKey(ref)) {
            return fieldRenames.get(ref).get(var);
        }
        return null;
    }

    public ApiGeneratorContext<?> getCtx() {
        return ctx;
    }

    public void setCtx(ApiGeneratorContext<?> ctx) {
        this.ctx = ctx;
    }

    public void setGeneratedComponent(GeneratedUiComponent generatedComponent) {
        this.generatedComponent = generatedComponent;
    }

    public GeneratedUiComponent getGeneratedComponent() {
        return generatedComponent;
    }

    public boolean hasResolvedFactory(Class<?> key) {
        final Maybe<Lazy<?>> success = factories.firstWhereKeyValue(key::isAssignableFrom, Lazy::isFull1);
        return success.isPresent();
    }

    public boolean hasRegisteredFactory(Class<?> key) {
        final Maybe<Lazy<?>> success = factories.firstWhereKey(key::isAssignableFrom);
        return success.isPresent();
    }

    public <T, Generic extends T> T getOrCreateFactory(
        Class<Generic> key,
        In1Out1<Class<? super Generic>, T> factory
    ) {
        final Lazy<?> existing = factories.getAssignable(key);
        if (existing != null) {
            return (T) existing.out1();
        }
        final Lazy<?> result = Lazy.deferred1(factory.supply(key));
        factories.put(key, result);
        return (T) result.out1();
    }

    public void addRecommendedImports(SizedIterable<String> recommendedImports) {
        if (this.recommendedImports == null) {
            this.recommendedImports = recommendedImports;
        } else {
            this.recommendedImports = this.recommendedImports.plus(recommendedImports);
        }
    }

    public SizedIterable<String> getRecommendedImports() {
        return recommendedImports;
    }
}
