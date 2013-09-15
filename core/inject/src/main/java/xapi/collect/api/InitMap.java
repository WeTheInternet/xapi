package xapi.collect.api;

import xapi.collect.impl.AbstractInitMap;
import xapi.util.api.Pair;
import xapi.util.api.Triple;

/**
 * Our init map purposely forces string-key semantics,
 * to force our subclasses to take care of key serialization for us.
 *
 * {@link AbstractInitMap} will accept a key converter, which will work,
 * and allow reuse of code and singleton sharing,
 * but will be slower than if you extends an existing StringDictionary
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <Key>
 * @param <Value>
 */
public interface InitMap <Key, Value> extends StringDictionary<Value>{

  /**
   * @param key - The Key used when value was null.
   * @return - A newly created instance of whatever you need to init.
   *
   * Note that you can use Pair<,> or Triple<,,> to overload your key type.
   */
  Value initialize(Key key);

  /**
   * We force subclasses to deal with serializing keys to unique names,
   * so javascript can implement this directly on an object,
   * and you can achieve a degree of portability,
   * by making map structure deterministic.
   *
   * @param key - A key of whatever type you want.  Use {@link Pair} or {@link Triple} to overload.
   * @return - A unique key for dictionary-like sematics
   * try to have a string id pre-constructed;
   * if your object fields are immutable, you can safely construct the id,
   * and never have to null check.
   */
  String toKey(Key key);

  // Add some typed methods, for easier map-like access
  Value get(Key key);
  boolean containsKey(Key key);
  Value put(Key key, Value value);

}
