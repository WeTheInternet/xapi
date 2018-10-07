package xapi.util;

import xapi.fu.In1Out1;
import xapi.fu.Out1;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.Iterator;

import static xapi.fu.itr.ArrayIterable.iterate;
import static xapi.fu.itr.MappedIterable.mapped;

public class X_String {

  public static final String EMPTY_STRING = "";
  private static final String[] binarySuffix = new String[]{"","K","M","G","T","P","E","Z"};
  private static final String[] metricSuffix = new String[]{" nano"," micro"," milli"," "," kilo"," mega"," giga"};

    /**
   * Used for the encodeURIComponent function
   */
  private static final BitSet notEncoded;
  private static final String HEX_DIGITS = "0123456789ABCDEF";

  static {
    notEncoded = new BitSet(256);

    // a-z
    for (int i = 'a'; i <= 'z'; ++i) {
      notEncoded.set(i);
    }
    // A-Z
    for (int i = 'A'; i <= 'Z'; ++i) {
      notEncoded.set(i);
    }
    // 0-9
    for (int i = '0'; i <= '9'; ++i) {
      notEncoded.set(i);
    }

    // '()*
    for (int i = 39; i <= 42; ++i) {
      notEncoded.set(i);
    }
    notEncoded.set(33); // !
    notEncoded.set(45); // -
    notEncoded.set(46); // .
    notEncoded.set(95); // _
    notEncoded.set(126); // ~
  }

  /**
   * Escapes all characters except the following: alphabetic, decimal digits, - _ . ! ~ * ' ( )
   *
   * @param input A component of a URI
   * @return the escaped URI component
   */
  public static String encodeURIComponent(String input) {
    if (input == null) {
      return input;
    }

    StringBuilder filtered = new StringBuilder(input.length()*2);
    char c;
    for (int i = 0; i < input.length(); ++i) {
      c = input.charAt(i);
      if (notEncoded.get(c)) {
        filtered.append(c);
      } else {
        final byte[] b = charToBytesUTF(c);

        for (int j = 0; j < b.length; ++j) {
          filtered.append('%');
          filtered.append(HEX_DIGITS.charAt(b[j] >> 4 & 0xF));
          filtered.append(HEX_DIGITS.charAt(b[j] & 0xF));
        }
      }
    }
    return filtered.toString();
  }

  public static String decodeURIComponent(String input) {
    if (input == null) {
      return input;
    }

    StringBuilder output = new StringBuilder(input.length());
    char c;


    for (int i = 0, more = -1; i < input.length(); i++) {
      c = input.charAt(i);
      switch (c) {
        case '%':
          // restore from hex digit
          for (int bytePattern, sumb = 0; i < input.length(); i++) {
            assert input.charAt(i) == '%' : "Bad encoding: '" + input.charAt(i) +"' at index " + i + " is not %\nsource: " + input;
            bytePattern = Integer.parseInt(input.substring(++i, ++i+1),16);
            if ((bytePattern & 0xc0) == 0x80) { // 10xxxxxx
              sumb = (sumb << 6) | (bytePattern & 0x3f);
              if (--more == 0) {
                output.append((char) sumb);
                break;
              }
            } else if ((bytePattern & 0x80) == 0x00) { // 0xxxxxxx
              output.append((char) bytePattern);
              break;
            } else if ((bytePattern & 0xe0) == 0xc0) { // 110xxxxx
              sumb = bytePattern & 0x1f;
              more = 1;
            } else if ((bytePattern & 0xf0) == 0xe0) { // 1110xxxx
              sumb = bytePattern & 0x0f;
              more = 2;
            } else if ((bytePattern & 0xf8) == 0xf0) { // 11110xxx
              sumb = bytePattern & 0x07;
              more = 3;
            } else if ((bytePattern & 0xfc) == 0xf8) { // 111110xx
              sumb = bytePattern & 0x03;
              more = 4;
            } else { // 1111110x
              sumb = bytePattern & 0x01;
              more = 5;
            }
          }
          break;
        case '+':
          output.append(' ');
          continue;
        default:
          output.append(c);
      }
    }
    return output.toString();
  }

  private static byte[] charToBytesUTF(char c) {
    try {
      return new String(new char[]{c}).getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      return new byte[]{(byte) c};
    }
  }

    public static boolean equalIgnoreWhitespace(String a, String b) {
      if (a == b) {
        return true;
      }
      if (a == null || b == null) {
        return false;
      }
      return a.replaceAll("\\s+", " ")
          .equals(b.replaceAll("\\s+", " "));
    }

    private X_String() {}

  public static byte[] getBytes(String source) {
    return source.getBytes();
  }

  public static String toMetricSuffix(double val) {
    if (val < 0) {
      return "-" + toMetricSuffix(-val);
    }
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
            return "0 ";
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

  public static String joinClasses(String separator, In1Out1<Class<?>, String> mapper, Class<?> ... values) {
    String[] copy = classesMapped(mapper, values);
    return join(separator, copy);
  }

  public static String classToQualified(Class<?> cls) {
    return classesToQualified(cls)[0];
  }
  public static String[] classesToQualified(Class<?> ... values) {
    return classesMapped(Class::getCanonicalName, values);
  }

  public static String[] classesMapped(In1Out1<Class<?>, String> mapper, Class<?> ... values) {
    int i = values.length;
    String[] copy = new String[i];
    for(;i-->0;){
      Class<?> cls = values[i];
      if (cls != null)
        copy[i] = mapper.io(cls);
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
  public static <T> String join(String separator, In1Out1<T, String> mapper, T ... values) {
    return join(separator, iterate(values)
        .map(mapper));
  }
  public static <T> String join(String separator, In1Out1<T, String> mapper, Iterable<T> values) {
    return join(separator, mapped(values).map(mapper));
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
    int i = string.indexOf(c);
    if (i == -1)
      return string;
    return string.substring(0, i);
  }

  public static String lastChunk(String string, char c) {
    assert string != null : "No nulls to X_String.lastChunk, please.";
    int i = string.lastIndexOf(c);
    if (i == -1)
      return string;
    return string.substring(i + 1);
  }

  public static String firstNotEmpty(String groupId1, String groupId2) {
    return isEmptyTrimmed(groupId1) ? groupId2 : groupId1;
  }

  public static String firstNotEmptyDeferred(String groupId1, Out1<String> groupId2) {
    return isEmptyTrimmed(groupId1) ? groupId2.out1() : groupId1;
  }

  public static String firstNotEmpty(String first, String second, String ... rest) {
    if (!isEmptyTrimmed(first)) {
      return first;
    }
    if (!isEmptyTrimmed(second)) {
      return second;
    }
    for (String next : rest) {
      if (!isEmptyTrimmed(next)) {
        return next;
      }
    }
    return "";
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

  public static String chopEndOrReturnEmpty(String source, String match) {
    int ind = source.lastIndexOf(match);
    if (ind == -1) {
      return "";
    }
    return source.substring(ind+1);
  }

  public static String toTitleCase(String name) {
    return isEmpty(name) ? name : Character.toUpperCase(name.charAt(0))+
        (name.length() == 1 ? "" : name.substring(1));
  }

  public static String normalizeNewlines(String text) {
    return text.replaceAll("\\r\\n?", "\n");
  }

  public static String repeat(String s, int i) {
    StringBuilder b = new StringBuilder();
    while (i --> 0) {
      b.append(s);
    }
    return b.toString();
  }

  public static String toTimestamp(int year, int month, int date, int hour, int minute, int milli, int offsetMinutes) {

    // Ya...  It's more lines of code than using a library,
    // but it's also the minimum overhead possible...
    // and no crazy "remember to +/- 1900 to your ints!" semantics ...java.util.Calender

    char[] result = "yyyy-MM-ddTHH:mm.sss+00:00".toCharArray();

    result[3] = Character.forDigit(year%10, 10);
    result[2] = Character.forDigit((year/=10)%10, 10);
    result[1] = Character.forDigit((year/=10)%10, 10);
    result[0] = Character.forDigit((year/10)%10, 10);

    result[5] = Character.forDigit(month/10, 10);
    result[6] = Character.forDigit(month%10, 10);

    result[8] = Character.forDigit(date/10, 10);
    result[9] = Character.forDigit(date%10, 10);

    result[11] = Character.forDigit(hour/10, 10);
    result[12] = Character.forDigit(hour%10, 10);

    result[14] = Character.forDigit(minute/10, 10);
    result[15] = Character.forDigit(minute%10, 10);

    result[19] = Character.forDigit(milli%10, 10);
    result[18] = Character.forDigit((milli/=10)%10, 10);
    result[17] = Character.forDigit((milli/10)%10, 10);

    if (offsetMinutes < 0) {
      result[20] = '-';
      offsetMinutes = -offsetMinutes;
    }
    int hours = offsetMinutes / 60;
    result[21] = Character.forDigit(hours/10, 10);
    result[22] = Character.forDigit(hours%10, 10);

    offsetMinutes = offsetMinutes%60;
    result[24] = Character.forDigit((offsetMinutes/10)%10, 10);
    result[25] = Character.forDigit(offsetMinutes%10, 10);

    return new String(result);

  }

  public static String firstCharToLowercase(String name) {
    if (name == null) {
      return null;
    }
    if (name.length() == 1) {
      return name.toLowerCase();
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  public static String toConstantName(String name) {
    if (name == null) {
      return null;
    }
    return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
  }

  public static String dequote(String s) {
    if (s == null || s.length() < 2) {
      return null;
    }
    if (s.startsWith("\"") && s.endsWith("\"")) {
      return s.substring(1, s.length()-1);
    }
    return s;
  }

  public static String reverse(String code) {
    char[] chars = code.toCharArray();
    for (int start=0,end=chars.length-1;start<=end;start++,end--) {
      char temp = chars[start];
      chars[start] = chars[end];
      chars[end] = temp;
    }
    return new String(chars);
  }

    public static String ensureStartsWith(String base, String prefix) {
      if (base.startsWith(prefix)) {
        return base;
      }
      return prefix + base;
    }

    public static String ensureEndsWith(String base, String prefix) {
      if (base.endsWith(prefix)) {
        return base;
      }
      return base + prefix;
    }

  public static String debean(String name) {
    if (name.startsWith("get") || name.startsWith("has") || name.startsWith("set") || name.startsWith("add")) {
      if (name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
        name = Character.toLowerCase(name.charAt(3)) +
            (name.length() > 4 ? name.substring(4) : "");
      }
    } else if (name.startsWith("is")) {
      if (name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
        name = Character.toLowerCase(name.charAt(2)) +
            (name.length() > 3 ? name.substring(3) : "");
      }
    }
    return name;
  }

  public static String concatIfNotContains(String is, String value, String s) {
    final String clsName = s + is + s;
    return
        clsName.contains(s + value + s)
            ? is
            : is + s + value;
  }

    public static String removePrefix(String from, String prefix) {
      if (from == null || prefix == null) {
        return from;
      }
      if (from.startsWith(prefix)) {
        return from.substring(prefix.length());
      }
      return from;
    }

    public static String removeSuffix(String from, String suffix) {
      if (from == null || suffix == null) {
        return from;
      }
      if (from.endsWith(suffix)) {
        return from.substring(0, from.length() - suffix.length());
      }
      return from;
    }

    public static boolean containsAny(String s, String ... choices) {
      if (s == null) {
        return false; // null contains nothing.
      }
      for (String choice : choices) {
        if (s.equals(choice)) {
          return true;
        }
      }
      return false;
    }

    public static String addLineNumbers(String src) {
      final String[] lines = src.split("\n");
      int maxSize = (int)(Math.log10(lines.length) + 1);
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        lines[i] = (i + 1) + repeat(" ", maxSize - i + 1) + line;
      }
      return join("\n", lines);
    }
}
