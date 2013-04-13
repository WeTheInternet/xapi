package xapi.collect.api;


public interface ClassTo <V>
extends ObjectTo<Class<?>, V>
{

  static interface Many <V>
    extends ClassTo<IntTo<V>>, ObjectTo.Many<Class<?>, V>
  {}

}