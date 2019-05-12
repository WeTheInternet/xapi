package xapi.fu.filter;

import xapi.fu.Filter.Filter1;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/05/19 @ 3:21 AM.
 */
public class IfNotNull<T> implements Filter1<T> {
  @Override
  public boolean filter1(T args) {
    return args != null;
  }
}
