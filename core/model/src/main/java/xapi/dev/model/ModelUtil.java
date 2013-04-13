package xapi.dev.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelUtil {

  private static final Pattern GETTER_MATCHER = Pattern.compile(
    "(?:get|has|is)(\\w+)"
  );

  private static final Pattern SETTER_MATCHER = Pattern.compile(
    "(?:setAll|putAll|addAll|add|put|set)(\\w+)"
    );

  private static final Pattern REMOVER_MATCHER = Pattern.compile(
    "(?:removeAll|remove|rem|deleteAll|delete|del)(\\w+)"
    );

  public static String stripGetter (String name) {
    Matcher matcher = GETTER_MATCHER.matcher(name);
    if (matcher.matches()) {
      name = matcher.group(1);
    }
    return Character.toLowerCase(name.charAt(0))+name.substring(1);
  }

  public static String stripSetter(String name) {
    Matcher matcher = SETTER_MATCHER.matcher(name);
    if (matcher.matches()) {
      name = matcher.group(1);
    }
    return Character.toLowerCase(name.charAt(0))+name.substring(1);
  }

  public static String stripRemover (String name) {
    Matcher matcher = REMOVER_MATCHER.matcher(name);
    if (matcher.matches()) {
      name = matcher.group(1);
    }
    return Character.toLowerCase(name.charAt(0))+name.substring(1);
  }

}
