package xapi.ui.api.component;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.fu.Immutable;
import xapi.fu.Out1;
import xapi.fu.itr.SizedIterable;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;

/**
 * Abstract base class of all generic component builders.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/13/17.
 */
public interface ComponentBuilder <
    El,
    O extends ComponentOptions<El, C>,
    C extends IsComponent<El>
    > {

    static <C extends IsComponent, O extends ComponentOptions> void registerFactory(
        Class<C> apiType,
        ScopedComponentFactory<O, C> factory) {
        final IntTo<ScopedComponentFactory<?, ?>> list = ComponentBuilderFactories.factories.get(apiType);
        assert !list.contains(factory) : "Component registered twice (" + apiType + "): " + factory;
        list.add(factory);
    }

    static <O extends ComponentOptions, C extends IsComponent> SizedIterable<ScopedComponentFactory<O, C>> getFactory(Class<C> componentType) {
        final IntTo list = ComponentBuilderFactories.factories.get(componentType);
        return list;
    }

    Class<C> componentType();

    O asOptions();

    default C build() {
        return build(X_Scope::currentScope);
    }

    default C build(Scope scope) {
        return build(Immutable.immutable1(scope));
    }

    default C build (Out1<Scope> scope) {
        final SizedIterable<ScopedComponentFactory<O, C>> list = getFactory(componentType());
        if (list.size() == 1) {
            return list.first().createComponent(asOptions());
        }
        double best = Double.MIN_VALUE;
        final Scope s = scope.out1();
        if (list.isEmpty()) {
            throw new IllegalStateException("No registered factory for " + componentType());
        }
        ScopedComponentFactory<O, C> toUse = list.first();
        for (ScopedComponentFactory<O, C> provider : list) {
            double score = provider.getScore(s);
            if (score > best) {
                toUse = provider;
            }
        }
        if (toUse == null) {
            throw new IllegalStateException("No compatible registered factory in " + list);
        }

        return toUse.createComponent(asOptions());
    }

}
class ComponentBuilderFactories {
    static final ClassTo.Many<ScopedComponentFactory<?, ?>> factories = X_Collect.newClassMultiMap(ScopedComponentFactory.class);
}
