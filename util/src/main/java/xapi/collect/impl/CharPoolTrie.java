package xapi.collect.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.api.CharPool;
import xapi.except.NotYetImplemented;
import xapi.source.api.Chars;

@SingletonDefault(implFor=CharPool.class)
public class CharPoolTrie extends MultithreadedStringTrie<char[]> implements CharPool{

  @Override
  public char[] getArray(char[] src) {
    if (src.length == 0)
      return CharPool.EMPTY_STRING;
    return get(src, 0, src.length);
  }

  @Override
  public char[] getArray(char[] src, int start, int len) {
    if (start == 0 && len == 0)
      return CharPool.EMPTY_STRING;
    return get(src,start,len);
  }

  @Override
  public char[] getArray(CharSequence src) {
    int len = src.length();
    if (len == 0)
      return CharPool.EMPTY_STRING;
    return getArray
      (src,0,len);
  }

  @Override
  public char[] getArray(CharSequence src, int start, int len) {
    if (src instanceof Chars)
      return super.get((Chars)src, start, len);
    if (src instanceof String) {
      if (start == 0 && len == src.length())
        return super.get((String)src);
      char[] key = new char[len-start];
      ((String)src).getChars(start, start+len, key, 0);
      return super.get(key);
    }
    throw new NotYetImplemented("CharPoolTrie only accepts String and Chars " +
    		"for CharSequence.  You sent "+src+"; a "+src.getClass());
  }

  @Override
  protected char[] onEmpty(Edge e, Chars key, int pos, int end) {
    assert pos < end;//you can't call onEmpty if you're on the last node!
    char[] singleton = new char[end-pos];
    char[] keys = key.getChars();
    System.arraycopy(keys, key.getStart()+pos, singleton, 0, singleton.length);
    doPut(e, keys, key.getStart()+pos, key.getStart()+end, singleton);
    return singleton;
  }

}
