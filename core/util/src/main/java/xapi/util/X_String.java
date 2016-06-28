package xapi.util;

import xapi.collect.api.CharPool;
import xapi.inject.X_Inject;

import static xapi.util.service.StringService.binarySuffix;
import static xapi.util.service.StringService.metricSuffix;

import java.util.Iterator;

public class X_String {

  private X_String() {}

  public static byte[] getBytes(String source) {
    return source.getBytes();
  }

  public static CharPool getCharPool() {
    return X_Inject.singleton(CharPool.class);
  }

  public static String toMetricSuffix(double val) {
    String suffix;
    int index = 3; //we go from nano to giga; up three and down three.
    if (val > 1) {
      //count up
      while(val >= 1000){
        index++;
        val = val / 1000.0;
        if (index==6)break;
      }
      suffix = metricSuffix[index];
      return ((int)(10*val)/10.0)+suffix;
    }else {
      //count down
      while(val < 1.0){
        if (index==0) {
          if (val < 0.1) {
            //nano scale, we need to stop and truncate to nearest non-zero values.
            suffix = Double.toString(val);
            for (int pos = 2; pos < suffix.length(); pos++) {
              if (suffix.charAt(pos)!='0') {
                suffix = suffix.substring(0, Math.min(pos+1,suffix.length()-1));
                return suffix+metricSuffix[index];
              }
            }
            return "0";
          }
          break;
        }
        index--;
        val = val * 1000.0;
      }
      suffix = metricSuffix[index];
      return ((int)(10.001*val)/10.0)+suffix;
    }
  }
  public static String toBinarySuffix(double val) {
    String suffix;
    boolean neg = val < 0;
    if (neg)val = val*-1;
    int index = 0;
    while(val >= 1024){
      index++;
      val = val / 1024.0;
    }
    suffix = binarySuffix[Math.min(index,binarySuffix.length-1)];
    return (neg?"-":"")+((int)(10*val))/10.0+suffix;
  }

  @SafeVarargs
  public static <T> String joinObjects(
      @SuppressWarnings("unchecked") T ... values
      ) {
    return joinObjects(", ", values);
  }

  @SafeVarargs
  public static <T> String joinObjects(
    String separator,
    @SuppressWarnings("unchecked") T ... values
  ) {
    int i = values.length;
    String[] copy = new String[i];
    for(;i-->0;)
      copy[i] = String.valueOf(values[i]);
    return join(separator, copy);
  }

  public static String joinClasses(String separator, Class<?> ... values) {
    String[] copy = classesToQualified(values);
    return join(separator, copy);
  }

  public static String classToQualified(Class<?> cls) {
    return classesToQualified(cls)[0];
  }
  public static String[] classesToQualified(Class<?> ... values) {
    int i = values.length;
    String[] copy = new String[i];
    for(;i-->0;){
      Class<?> cls = values[i];
      if (cls != null)
        copy[i] = cls.getCanonicalName();
    }
    return copy;
  }

  public static String classToSourceFiles(Class<?> cls) {
    return classesToSourceFiles(cls)[0];
  }

  public static String[] classesToSourceFiles(Class<?> ... values) {
    int i = values.length;
    String[] copy = new String[i];
    for(;i-->0;){
      Class<?> cls = values[i];
      if (cls != null)
        copy[i] = cls.getCanonicalName().replace('.', X_Runtime.fileSeparatorChar()) + ".java";
    }
    return copy;
  }

  public static String classToBinary(Class<?> cls) {
    return classesToBinary(cls)[0];
  }
  public static String[] classesToBinary(Class<?> ... values) {
    int i = values.length;
    String[] copy = new String[i];
    for(;i-->0;){
      Class<?> cls = values[i];
      if (cls != null)
        copy[i] = cls.getName();
    }
    return copy;
  }

  public static String joinStrings(String ... values) {
    return join(", ", values);
  }
  public static String join(String separator, String ... values)
  /*js:

  return values.join(separator);

  :js*/
  {//java:
    if (values.length == 0) return "";// need at least one element
    // all string operations use a new array, so minimize all calls possible
    char[] sep = separator.toCharArray();

    // determine final size and normalize nulls
    int totalSize = (values.length - 1) * sep.length;// separator size
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null)
        values[i] = "";
      else
        totalSize += values[i].length();
    }

    // exact size; no bounds checks or resizes
    char[] joined = new char[totalSize];
    int pos = 0;
    // note, we are iterating all the elements except the last one
    for (int i = 0, end = values.length - 1; i < end; i++) {
      System.arraycopy(values[i].toCharArray(), 0, joined, pos, values[i].length());
      pos += values[i].length();
      System.arraycopy(sep, 0, joined, pos, sep.length);
      pos += sep.length;
    }
    // now, add the last element;
    // this is why we short-circuited values.length == 0 off the hop
    System.arraycopy(values[values.length - 1].toCharArray(), 0, joined, pos,
      values[values.length - 1].length());

    return new String(joined);
  }//:java

  public static String join(String separator, Iterable values) {
    StringBuilder b = new StringBuilder();
    final Iterator itr = values.iterator();
    if (itr.hasNext()) {
      b.append(String.valueOf(itr.next()));
    }
    while (itr.hasNext()) {
      b.append(separator).append(String.valueOf(itr.next()));
    }
    return b.toString();
  }

  public static boolean isEmpty(String enclosing) {
    return enclosing == null || enclosing.length() == 0;
  }

  public static boolean isEmptyTrimmed(String enclosing) {
    return enclosing == null || enclosing.trim().length() == 0;
  }

  public static boolean isNotEmpty(String enclosing) {
    return enclosing != null && enclosing.length() > 0;
  }
  public static boolean isEmpty(String[] enclosing) {
    if (enclosing == null || enclosing.length == 0) {
      return true;
    }
    for (String s : enclosing) {
      if (isNotEmpty(s)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isNotEmpty(String[] enclosing) {
    return !isEmpty(enclosing);
  }

  public static boolean isNotEmptyTrimmed(String enclosing) {
    return enclosing != null && enclosing.trim().length() > 0;
  }

  public static String firstChunk(String string, char c) {
    assert string != null : "No nulls to X_String.firstChunk, please.";
    int i = string.charAt(c);
    if (i == -1)
      return string;
    return string.substring(0, i);
  }

  public static String firstNotEmpty(String groupId1, String groupId2) {
    return isEmptyTrimmed(groupId1) ? groupId2 : groupId1;
  }

  public static String[] splitNewLine(String str) {
    return str.split("\n");
  }

  public static String notNull(String str) {
    return str == null ? "" : str;
  }

  public static String chopOrReturnEmpty(String source, String match) {
    int ind = source.lastIndexOf(match);
    if (ind == -1) {
      return "";
    }
    return source.substring(0, ind);
  }

  public static String toTitleCase(String name) {
    return isEmpty(name) ? name : Character.toUpperCase(name.charAt(0))+name.substring(1);
  }

  public static String normalizeNewlines(String text) {
    return text.replaceAll("\\r\\n?", "\n");
  }

}
