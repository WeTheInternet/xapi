package xapi.scope.api;

import xapi.fu.In1Out1;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public interface Scope {

  interface GlobalScope <UserType, RequestType> extends Scope {

    SessionScope<UserType, RequestType> getSessionScope(UserType user);

    @Override
    default Class<? extends Scope> forScope() {
      return GlobalScope.class;
    }
  }
  interface SessionScope <UserType, RequestType> extends Scope {
    UserType getUser();
    RequestScope<RequestType> getRequestScope(RequestType request);

    SessionScope<UserType, RequestType> setUser(UserType user);

    void touch();

    @Override
    default Class<? extends Scope> forScope() {
      return SessionScope.class;
    }
  }
  interface RequestScope<RequestType>  extends Scope {
    RequestType getRequest();

    void setRequest(RequestType req);

    @Override
    default Class<? extends Scope> forScope() {
      return RequestScope.class;
    }
  }
  interface LocalScope extends Scope {
    @Override
    default Class<? extends Scope> forScope() {
      return LocalScope.class;
    }
  }

  boolean isReleased();

  boolean hasLocal(Class<?> cls);

  <T, C extends T> T getLocal(Class<C> cls);

  <T, C extends T> T set(Class<C> cls, T value);

  void release();

  Scope getParent();

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
      set(key, value);
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

}
