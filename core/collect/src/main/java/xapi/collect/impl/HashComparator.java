package xapi.collect.impl;

import java.util.Comparator;

public final class HashComparator <T> implements Comparator<T> {
  public int compare(T o1, T o2) {
    if (o1 == o2)
      return 0;
    if (o1 == null) {
      return 1;
    } else if (o2 == null) {
      return -1;
    }
    int delta = o1.hashCode() - o2.hashCode();
    if (delta == 0) {
      if (o1.equals(o2)) {
        return 0;
      } else {
        throw new RuntimeException("Hash collision for inequal objects: "+o1+" && "+o2);
      }
    }
    return delta > 0 ? 1 : -1;
  };
}