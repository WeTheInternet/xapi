package xapi.scope.api;

import xapi.annotation.process.Multiplexed;
import xapi.annotation.process.Uniplexed;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.except.NotConfiguredCorrectly;
import xapi.except.NotYetImplemented;
import xapi.fu.*;
import xapi.fu.has.HasLock;
import xapi.fu.iterate.SizedIterable;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public interface Scope extends HasLock {

  default <T, C extends T> T getLocal(Class<C> cls) {
    return this.<T, C>localData().get(cls);
  }

  @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
  default SizedIterable<Class<?>> getLocalKeys() {
    return mutex(()->{
      // TODO handle multiplexed inner values
      final SizedIterable keys = localData().keys();
      return keys;
    });
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
    return mutex(()->localData().has(cls));
  }

  default <T, C extends T> T removeLocal(Class<C> cls) {
    boolean isMultiplexed = isMultiplexed(cls);
    if (isMultiplexed) {
      final MultiplexedScope map = getOrCreate(MultiplexedScope.class, k->new MultiplexedScope());
      return (T) map.get().remove(cls);
    }
    return mutex(()->this.<T, C>localData().remove(cls));
  }

  static boolean isMultiplexed(Class<?> cls) {
    return cls.getAnnotation(Uniplexed.class) == null &&
           cls.getAnnotation(Multiplexed.class) != null;
  }

  default <T, C extends T> T setLocal(Class<C> cls, T value) {
    assert !isReleased() : "Do not add values to a released scope; instead, implement Immortal and call reincarnateIfNeeded()";
    boolean isMultiplexed = isMultiplexed(cls);
    if (isMultiplexed) {
      final MultiplexedScope map = getOrCreate(MultiplexedScope.class, k->new MultiplexedScope());
      return (T) map.get().put(cls, value);
    }
    return mutex(()->this.<T, C>localData().put(cls, value));
  }

  default boolean isReleased() {
    return !hasLocal(ScopeKeys.ATTACHED_KEY);
  }

  default boolean isAttached() {
    return mutex(()->localData().has(attachedKey()) || forScope() == GlobalScope.class);
  }

  default void release() {
    removeLocal(ScopeKeys.ATTACHED_KEY);
    if (has(MultiplexedScope.class)) {
      get(MultiplexedScope.class).remove();
    }
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
    assert !findParent(parent.forScope(), false).isPresent() :
        "Already have a parent for " + parent.forScope() +" (from " + parent + ")";
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


  default <T, C extends T> T getOrSupply(Class<C> key, Out1<T> factory) {
    return getOrCreate(key, factory.ignoreIn1());
  }

  default <T, C extends T> T getOrCreate(Class<C> key, In1Out1<Class<C>, T> factory) {
    return mutex(()-> {
      if (has(key)) {
        return get(key);
      } else {
        final T value = factory.io(key);
        setLocal(key, value);
        return value;
      }
    });
  }
  default <T, C extends T, S extends Scope> T getOrCreateIn(Class<S> type, Class<C> key, In1Out1<Class<C>, T> factory) {
    return mutex(()-> {
      if (has(key)) {
        return get(key);
      } else {
        final T value = factory.io(key);
        Maybe<S> s = findParentOrSelf(type, false);
        s.mapDeferred(In1Out1.<S, Scope>downcast())
            .ifAbsentReturn(this)
            .setLocal(key, value);
        return value;
      }
    });
  }

  default <S extends Scope> Maybe<S> findParent(Class<S> type, boolean exactMatch) {
    Scope current = this;
    Predicate<Class> matcher = exactMatch ? type::equals : type::isAssignableFrom;
    while (current != null) {
      if (matcher.test(current.getClass())) {
        return Maybe.immutable((S)current);
      }
      current = current.getParent();
    }
    return Maybe.not();
  }

  default <S extends Scope> S findParentOrCreateChild(Class<S> type, boolean exactMatch, Out1<S> factory) {
    return findParentOrCreateChildIn(type, exactMatch, forScope(), factory.ignoreIn1());
  }

  default <S extends Scope> S findParentOrCreateChildIn(Class<S> type, boolean exactMatch, Class<? extends Scope> insertionPoint, Out1<S> factory) {
    return findParentOrCreateChildIn(type, exactMatch, insertionPoint, factory.ignoreIn1());
  }

  default <S extends Scope> S findParentOrCreateChild(Class<S> type, boolean exactMatch, In1Out1<Class<S>, S> factory) {
    return findParentOrCreateChildIn(type, exactMatch, forScope(), factory);
  }
  default <S extends Scope> S findParentOrCreateChildIn(Class<S> type, boolean exactMatch, Class<? extends Scope> insertionPoint, In1Out1<Class<S>, S> factory) {
    final Maybe<S> parent = findParent(type, exactMatch);
    Scope self = this;
    return parent.ifAbsentSupply(()->{
      final S newInstance = factory.io(type);

      Scope target;
      if (insertionPoint == self.forScope()) {
        newInstance.setParent(this);
        target = this;
      } else {
        //
        target = findParent(insertionPoint, exactMatch)
            .getOrThrow(()->new NotConfiguredCorrectly("No insertion point found for " + insertionPoint));
      }

      target.mutex(()->{
        target.setLocal(type, newInstance);
        if (type != newInstance.forScope()) {
          // we are forgiving about this, in case there is classloader weirdness from your factory,
          // we want both sources / classloaders to have a key to retrieve the instance.
          target.setLocal(newInstance.forScope(), newInstance);
          // we'll also want to ensure that, when this scope object is released,
          // we update the other key used for this scope
          newInstance.onDetached(target::removeLocal, type);
        }
        target.onDetached(newInstance::release);
      });
      return newInstance;
    });
  }

  default void onDetached(Do cleanup) {
    if (this == nullScope()) {
      // This is user error, trying to add a callback to the null scope
      throw new IllegalStateException("Cannot add onDetach scopes to null / detached scope");
    }
    throw new NotYetImplemented(getClass(), "Must implement onDetached()");
  }

  default <T> void onDetached(In1<T> callback, T value) {
    onDetached(callback.provide(value));
  }

  default <S extends Scope> Maybe<S> findParentOrSelf(Class<S> type, boolean exactMatch) {
    Scope current = this;
    Predicate<Class> matcher = exactMatch ? type::equals : type::isAssignableFrom;
    if (matcher.test(getClass())) {
      return Maybe.immutable((S)this);
    }
    while (current != null) {
      if (matcher.test(current.getClass())) {
        return Maybe.immutable((S)current);
      }
      current = current.getParent();
    }
    return Maybe.not();
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

  default Do captureScope() {
    Do capture = getParent() == null ? Do.NOTHING : getParent().captureScope();
    if (has(MultiplexedScope.class)) {
      MultiplexedScope tl = get(MultiplexedScope.class);
      if (tl.isInitialized()) {
        final ClassTo<Object> data = tl.get();
        // executed later to return to this scope.
        return capture.doAfter(()->{
          if (tl.isInitialized()) {
            // copy data into existing map
            final ClassTo<Object> existing = tl.get();
            for (Out2<Class<?>, Object> o : data.forEachEntry()) {
              final Object was = existing.get(o.out1());
              if (was == null) {
                existing.put(o.asEntry());
              } else {
                final Object v = o.out2();
                if (v == null || v == was) {
                  continue;
                }
                // There's a merge conflict here.
                existing.put(o.out1(), resolveConflict(o.out1(), o.out2(), v));
              }
            }
          } else {
            // initialize current thread w/ previous data
            tl.set(data);
          }
        });

      }
    }
    return capture;
  }

  /**
   * Used to resolve multiplexing conflicts when running the function returned from {@link #captureScope()}.
   *
   * @param type The class key for the scope entry
   * @param previous The previous value of existing scope
   * @param restored The value being restored by invoking
   * @return
   */
  default Object resolveConflict(Class<?> type, Object previous, Object restored) {
    if (X_Log.loggable(LogLevel.DEBUG)) {
      X_Log.debug(Scope.class, "Conflict detected in ", getClass(), " : ", this, ";\n",
        "Value ", type, " was overwritten from ", previous, " to ", restored);
      // TODO: incorporate Undo somehow.
    }
    return restored;
  }

  default boolean hasRetainers() {
    return mutex(()->hasLocal(ScopeKeys.RETAINER_KEY));
  }

  default Do retain() {
    mutex(()->{
      final Object retainers = getLocal(ScopeKeys.RETAINER_KEY);
      setLocal(ScopeKeys.RETAINER_KEY, retainers == null ? 1 : ((Integer)retainers)+1);
    });
    return ()->
      mutex(()->{
        final Object current = getLocal(ScopeKeys.RETAINER_KEY);
        if (current.equals(1)) {
          removeLocal(ScopeKeys.RETAINER_KEY);
        } else {
          setLocal(ScopeKeys.RETAINER_KEY, ((Integer)current)-1);
        }
      });
  }
}

@SuppressWarnings("unchecked")
// Intentionally stuffed here because nobody else should be referencing them...
final class ScopeKeys {
  static final Class<Scope> PARENT_KEY = Class.class.cast(ParentKey.class);
  static final Class<Object> ATTACHED_KEY = Class.class.cast(AttachedKey.class);
  static final Class<Object> RETAINER_KEY = Class.class.cast(RetainersKey.class);
  static final Scope NULL = MapLike::empty;
  private interface ParentKey extends Scope {}
  private interface AttachedKey extends Scope {}
  private interface RetainersKey extends Scope {}
}
// This class is intentionally stuffed here, because nobody else should be touching this.
final class MultiplexedScope extends ThreadLocal<ClassTo<Object>> {

  private final Map<Thread, Void> initialized = new WeakHashMap<>();

  @Override
  protected ClassTo<Object> initialValue() {
    initialized.put(Thread.currentThread(), null);
    return X_Collect.newClassMap(Object.class, X_Collect.MUTABLE_CONCURRENT);
  }

  public boolean isInitialized(){
    return initialized.containsKey(Thread.currentThread());
  }
}
