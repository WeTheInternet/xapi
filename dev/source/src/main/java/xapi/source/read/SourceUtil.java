package xapi.source.read;

public class SourceUtil {

  private SourceUtil() {}

  public static String toSourceName(String simpleName) {
    return simpleName.replace('$', '.');
  }

  public static String toFlatName(String simpleName) {
    return simpleName.replace('$', '_').replace('.', '_');
  }



}
