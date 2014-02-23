package xapi.collect.api;

/**
 * StringTo is a special mapping interface,
 * since it has the best possible native support in dictionary-oriented
 * languages, like javascript, we do not extend ObjectTo, which
 * forces generic override problems in the GWT compiler,
 * rather, we tie in to the HasValues interface instead.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <V>
 */
public interface StringTo <V>
extends HasValues<String,V>
{

  V get(String key);
  V put(String key, V value);
  V remove(String key);

  String[] keyArray();

  int size();
  
  public static interface Many <V>
  extends StringTo<IntTo<V>>
  {
  }

}
