package net.wti.gradle.internal.require.impl;

import net.wti.gradle.internal.api.HasWork;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.impl.DefaultWorker;
import net.wti.gradle.internal.require.api.GraphNode;
import net.wti.gradle.system.api.RealizableNamedObjectContainer;
import net.wti.gradle.system.impl.DefaultRealizableNamedObjectContainer;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Namer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.internal.reflect.Instantiator;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/1/19 @ 2:07 AM.
 */
public abstract class AbstractBuildGraphNode <T extends HasWork> extends DefaultWorker implements GraphNode {
    private final Set<String> registeredItems, realizedItems;
    private final RealizableNamedObjectContainer<T> items;

    protected class GraphNodeContainer extends DefaultRealizableNamedObjectContainer<T> {

        protected GraphNodeContainer(
            Class<T> type,
            Instantiator instantiator,
            Namer<? super T> namer,
            CollectionCallbackActionDecorator callbackDecorator
        ) {
            super(type, instantiator, namer, callbackDecorator);
            configureEach(known->
                known.drainTasks(ReadyState.CREATED)
            );
        }

        protected GraphNodeContainer(
            Class<T> type,
            Instantiator instantiator,
            CollectionCallbackActionDecorator callbackActionDecorator
        ) {
            super(type, instantiator, callbackActionDecorator);
            configureEach(known->
                known.drainTasks(ReadyState.CREATED)
            );
        }

        @Override
        protected final T doCreate(String name) {
            realizedItems.add(name);
            final T item = createItem(name);
            item.drainTasks(ReadyState.BEFORE_CREATED);
            return item;
        }

        @Override
        protected NamedDomainObjectProvider<T> createDomainObjectProvider(
            String name, @Nullable Action<? super T> configurationAction
        ) {
            final NamedDomainObjectProvider<T> provider = super.createDomainObjectProvider(name, configurationAction);
            provider.configure(realized->{
                realized.drainTasks(ReadyState.AFTER_CREATED);
            });
            return provider;
        }
    }

    protected abstract T createItem(String name);

    public AbstractBuildGraphNode(Class<T> type, ProjectView project) {
        registeredItems = new LinkedHashSet<>();
        realizedItems = new LinkedHashSet<>();
        items = new GraphNodeContainer(type, project.getInstantiator(), project.getDecorator());
    }

    protected RealizableNamedObjectContainer<T> getItems() {
        return items;
    }

    public NamedDomainObjectProvider<T> getOrRegister(String p) {
        if (registeredItems.add(p)) {
            return items.register(p);
        } else {
            return items.named(p);
        }
    }

    public void whenRealized(Action<? super T> action) {
        items.configureEach(action);
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
