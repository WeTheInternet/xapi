package xapi.api;

import xapi.fu.In1Out1;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public interface Scope {

  interface GlobalScope <UserType> extends Scope {
    <RequestType> SessionScope<UserType, RequestType> getSessionScope(UserType user);
  }
  interface SessionScope <UserType, RequestType> extends Scope {
    UserType getUser();
    RequestScope getRequestScope(RequestType request);
  }
  interface RequestScope  extends Scope {
  }
  interface LocalScope extends Scope {}

  boolean isReleased();

  <T, C extends Class<? extends T>> T get(C cls);

  <T, C extends Class<? extends T>> T set(C cls, T value);

  void release();

  Scope getParent();

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

  <K, T> T getOrCreate(K key, In1Out1<K, String> keyProvider, In1Out1<K, T> factory);

  default <T> T getOrCreate(String key, In1Out1<String, T> factory) {
    return getOrCreate(key, String::toString, factory);
  }

}
