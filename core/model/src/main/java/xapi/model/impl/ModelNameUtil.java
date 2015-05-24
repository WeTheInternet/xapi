/**
 *
 */
package xapi.model.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelNameUtil {

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
    final Matcher matcher = GETTER_MATCHER.matcher(name);
    if (matcher.matches()) {
      name = matcher.group(1);
    }
    return Character.toLowerCase(name.charAt(0))+name.substring(1);
  }

  public static String stripSetter(String name) {
    final Matcher matcher = SETTER_MATCHER.matcher(name);
    if (matcher.matches()) {
      name = matcher.group(1);
    }
    return Character.toLowerCase(name.charAt(0))+name.substring(1);
  }

  public static String stripRemover (String name) {
    final Matcher matcher = REMOVER_MATCHER.matcher(name);
    if (matcher.matches()) {
      name = matcher.group(1);
    }
    return Character.toLowerCase(name.charAt(0))+name.substring(1);
  }

}
