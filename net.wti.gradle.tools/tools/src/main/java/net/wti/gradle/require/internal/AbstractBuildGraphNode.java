package net.wti.gradle.require.internal;

import net.wti.gradle.require.internal.BuildGraph.ProjectGraph;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Namer;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.internal.reflect.Instantiator;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/1/19 @ 2:07 AM.
 */
public abstract class AbstractBuildGraphNode <T> {
    private final Set<String> registeredItems, realizedItems;
    private final NamedDomainObjectContainer<T> items;

    protected class GraphNodeContainer extends AbstractNamedDomainObjectContainer<T> {

        protected GraphNodeContainer(
            Class<T> type,
            Instantiator instantiator,
            Namer<? super T> namer,
            CollectionCallbackActionDecorator callbackDecorator
        ) {
            super(type, instantiator, namer, callbackDecorator);
        }

        protected GraphNodeContainer(
            Class<T> type,
            Instantiator instantiator,
            CollectionCallbackActionDecorator callbackActionDecorator
        ) {
            super(type, instantiator, callbackActionDecorator);
        }

        @Override
        protected T doCreate(String name) {
            realizedItems.add(name);
            return createItem(name);
        }
    }

    protected abstract T createItem(String name);

    public AbstractBuildGraphNode(Class<T> type, Instantiator inst) {
        registeredItems = new LinkedHashSet<>();
        realizedItems = new LinkedHashSet<>();
        items = new GraphNodeContainer(type, inst, CollectionCallbackActionDecorator.NOOP);
    }

    protected NamedDomainObjectContainer<T> getItems() {
        return items;
    }

    protected NamedDomainObjectProvider<T> getOrRegister(String p) {
        if (registeredItems.add(p)) {
            return items.register(p);
        } else {
            return items.named(p);
        }
    }

    protected Set<T> realizedItems() {
        Set<T> result = new LinkedHashSet<>();
        for (String realizedProject : realizedItems) {
            result.add(items.getByName(realizedProject));
        }
        return result;
    }

    protected Set<String> registeredItems() {
        return Collections.unmodifiableSet(registeredItems);
    }
}
