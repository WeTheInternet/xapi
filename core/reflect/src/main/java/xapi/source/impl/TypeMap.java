package xapi.source.impl;

import java.util.HashMap;

import xapi.source.api.IsType;

public class TypeMap {

  private final HashMap<String,IsType> types = new HashMap<String, IsType>();

  public IsType getType(String pkg, String simplename) {
    String key = pkg+"."+simplename;
    IsType type = types.get(key);
    if (type == null) {
      synchronized (types) {
        if (types.containsKey(key))
          return types.get(key);
        type = new ImmutableType(pkg, simplename);
        types.put(key, type);
      }
    }
    return type;
  }

  public IsType getType(IsType parentType, String simplename) {
    String key = parentType.getQualifiedName()+"."+simplename;
    IsType type = types.get(key);
    if (type == null) {
      synchronized (types) {
        if (types.containsKey(key))
          return types.get(key);
        type = new ImmutableType(parentType, simplename);
        types.put(key, type);
      }
    }
    return type;
  }
}
