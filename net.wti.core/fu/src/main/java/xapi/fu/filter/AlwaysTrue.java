package xapi.fu.filter;

import xapi.fu.Filter.Filter1;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/05/19 @ 3:20 AM.
 */
public class AlwaysTrue<T> implements Filter1<T> {
  @Override
  public boolean filter1(T args) {
    return true;
  }
}
