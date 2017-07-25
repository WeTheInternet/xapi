package xapi.scope.api;

import xapi.fu.In1Out1;
import xapi.fu.MapLike;
import xapi.fu.Out2;
import xapi.fu.X_Fu;
import xapi.fu.iterate.SizedIterable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public interface Scope {

  default <T, C extends T> T getLocal(Class<C> cls) {
    return this.<T, C>localData().get(cls);
  }

  @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
  default SizedIterable<Class<?>> getLocalKeys() {
    final SizedIterable keys = localData().keys();
    return keys;
  }

  @SuppressWarnings("unchecked")
  default void loadMap(MapLike<Class<?>, Object> into) {
    Scope start = this;
    while (start != null) {
      MapLike<?, ?> data = start.localData();
      data.forEachItem().forAll(i->{
        if (!into.has((Class<?>)i.out1())) {
          into.putPair((Out2<Class<?>, Object>) i);
        }
      });
      start = getParent();
    }
  }

  default boolean hasLocal(Class cls) {
    return localData().has(cls);
  }

  default <T, C extends T> T removeLocal(Class<C> cls) {
    return this.<T, C>localData().remove(cls);
  }

  default <T, C extends T> T setLocal(Class<C> cls, T value) {
    return this.<T, C>localData().put(cls, value);
  }

  default boolean isAttached() {
    return localData().has(attachedKey()) || forScope() == GlobalScope.class;
  }

  default void release() {
    removeLocal(ScopeKeys.ATTACHED_KEY);
    final Scope parent = getParent();
    parent.removeLocal(forScope());
    removeLocal(parent.forScope());
    setLocal(parentKey(), nullScope());
  }

  default Scope getParent() {
    // We use the class ParentKey as a key,
    // and just tell java we expect some kind of scope back
    // (a type which will not be of type ParentKey)
    return getLocal(parentKey());
  }

  default Scope setParent(Scope parent) {
    assert !findParent(parent.getClass(), true).isPresent();
    // We use the class ParentKey as a key,
    // and just tell java we expect some kind of scope back
    // (a type which will not be of type ParentKey)
    final MapLike<Class<Scope>, Scope> data = localData();
    setLocal(attachedKey(), parent.isAttached());
    setLocal(parent.forScope(), parent);
    parent.setLocal(forScope(), this);
    return data.put(parentKey(), parent);
  }

  <T, C extends T> MapLike<Class<C>, T> localData();

  default Class<? extends Scope> forScope() {
    return getClass();
  }

  default boolean has(Class<?> cls) {
    if (hasLocal(cls)) {
      return true;
    }
    final Scope parent = getParent();
    if (parent == null) {
      return false;
    } else {
      return parent.has(cls);
    }
  }

  default <T, C extends T> T get(Class<C> cls) {
    final T value = getLocal(cls);
    if (value != null) {
      return value;
    }
    final Scope parent = getParent();
    if (parent == null) {
      return null;
    } else {
      return parent.get(cls);
    }
  }


  default <T, C extends T> T getOrCreate(Class<C> key, In1Out1<Class<C>, T> factory) {
    if (has(key)) {
      return get(key);
    } else {
      final T value = factory.io(key);
      setLocal(key, value);
      return value;
    }
  }
  default <T, C extends T, S extends Scope> T getOrCreateIn(Class<S> type, Class<C> key, In1Out1<Class<C>, T> factory) {
    if (has(key)) {
      return get(key);
    } else {
      final T value = factory.io(key);
      Optional<S> s = findParentOrSelf(type, false);
      s.map(X_Fu::<S, Scope>downcast)
          .orElse(this)
          .setLocal(key, value);
      return value;
    }
  }

  default <S extends Scope> Optional<S> findParent(Class<S> type, boolean exactMatch) {
    Scope current = this;
    Predicate<Class> matcher = exactMatch ? type::equals : type::isAssignableFrom;
    while (current != null) {
      if (matcher.test(current.getClass())) {
        return Optional.of((S)current);
      }
      current = current.getParent();
    }
    return Optional.empty();
  }

  default <S extends Scope> Optional<S> findParentOrSelf(Class<S> type, boolean exactMatch) {
    Scope current = this;
    Predicate<Class> matcher = exactMatch ? type::equals : type::isAssignableFrom;
    if (matcher.test(getClass())) {
      return Optional.of((S)this);
    }
    while (current != null) {
      if (matcher.test(current.getClass())) {
        return Optional.of((S)current);
      }
      current = current.getParent();
    }
    return Optional.empty();
  }

  default Class<Scope> parentKey() {
    return ScopeKeys.PARENT_KEY;
  }

  default Class<Object> attachedKey() {
    return ScopeKeys.ATTACHED_KEY;
  }

  default Scope nullScope() {
    return ScopeKeys.NULL;
  }
}

@SuppressWarnings("unchecked")
final class ScopeKeys {
  static final Class<Scope> PARENT_KEY = Class.class.cast(ParentKey.class);
  static final Class<Object> ATTACHED_KEY = Class.class.cast(AttachedKey.class);
  static final Scope NULL = MapLike::empty;
  private interface ParentKey extends Scope {}
  private interface AttachedKey extends Scope {}
}
