package xapi.ui.api.component;

import xapi.annotation.model.ComponentType;
import xapi.collect.api.IntTo;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.data.Allable;
import xapi.model.X_Model;
import xapi.model.api.Model;

/**
 * A model of lists of child components, plus lists of callbacks for mutations to children list.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 4:32 AM.
 */
public interface ComponentList <S extends IsComponent> extends Model, Allable<S> {

    @SuppressWarnings("unchecked")
    static <S extends IsComponent> ComponentList<S> create() {
        return X_Model.create(ComponentList.class);
    }

    Class<? extends S> getChildType();
    void setChildType(Class<? extends S> type);
    default Class<? extends S> childType() {
        Class<? extends S> cls = getChildType();
        if (cls == null) {
            // A dirty lie, w.r.t. this method signature,
            // but the correct bounds to use for checked collections.
            return Class.class.cast(IsComponent.class);
        }
        return cls;
    }

    @ComponentType("getChildType()")
    IntTo<S> getChildren();
    void setChildren(IntTo<S> children);
    default IntTo<S> children() {
        return getOrCreateList(childType(), this::getChildren, this::setChildren);
    }
    default boolean hasChildren() {
        final IntTo<S> children = getChildren();
        return children != null && children.isNotEmpty();
    }

    default void add(S child) {
        children().add(child);
        final IntTo<In1<S>> callbacks = getOnAdd();
        if (IntTo.isNotEmpty(callbacks)) {
            callbacks.forAll(In1.invoker(), child);
        }
    }

    default void remove(S child) {
        boolean removed = children().remove(child) != null;
        if (removed) {
            final IntTo<In1<S>> callbacks = getOnRemove();
            if (IntTo.isNotEmpty(callbacks)) {
                callbacks.forAll(In1.invoker(), child);
            }
        }
    }

    IntTo<In1<S>> getOnAdd();
    void setOnAdd(IntTo<In1<S>> onAdd);
    default IntTo<In1<S>> onAdd() {
        return getOrCreateList(In1.class, this::getOnAdd, this::setOnAdd);
    }

    IntTo<In1<S>> getOnRemove();
    void setOnRemove(IntTo<In1<S>> onRemove);
    default IntTo<In1<S>> onRemove() {
        return getOrCreateList(In1.class, this::getOnRemove, this::setOnRemove);
    }

    @Override
    default Do all(In1<S> onAdd, In1<S> onRemove) {
        if (onAdd == In1.IGNORED) {
            if (onRemove == In1.IGNORED) {
                return Do.NOTHING;
            }
            final IntTo<In1<S>> toRemove = onRemove();
            toRemove.add(onRemove);
            return Do.of(toRemove::remove, onRemove);
        }
        if (onRemove == In1.IGNORED) {
            final IntTo<In1<S>> toAdd = onAdd();
            toAdd.add(onAdd);
            if (hasChildren()) {
                children().forAll(onAdd);
            }
            return Do.of(toAdd::remove, onAdd);
        }
        final IntTo<In1<S>> toAdd = onAdd();
        final IntTo<In1<S>> toRemove = onRemove();
        toAdd.add(onAdd);
        toRemove.add(onRemove);
        if (hasChildren()) {
            children().forAll(onAdd);
        }
        return ()->{
            toAdd.remove(onAdd);
            toRemove.remove(onRemove);
        };
    }
}
