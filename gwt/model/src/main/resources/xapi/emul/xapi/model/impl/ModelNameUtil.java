/**
 *
 */
package xapi.model.impl;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelNameUtil {

  private static final RegExp GETTER_MATCHER = RegExp.compile(
    "(?:get|has|is)(\\w+)"
  );

  private static final RegExp SETTER_MATCHER = RegExp.compile(
    "(?:setAll|putAll|putEntries|addAll|add|put|set)(\\w+)"
    );

  private static final RegExp REMOVER_MATCHER = RegExp.compile(
    "(?:removeAll|remove|rem|deleteAll|delete|del)(\\w+)"
    );

  public static String stripGetter (String name) {
    final MatchResult matcher = GETTER_MATCHER.exec(name);
    if (matcher != null) {
      return matcher.getGroup(1);
    }
    return name;
  }

  public static String stripSetter(String name) {
    final MatchResult matcher = SETTER_MATCHER.exec(name);
    if (matcher != null) {
      return matcher.getGroup(1);
    }
    return name;
  }

  public static String stripRemover (String name) {
    final MatchResult matcher = REMOVER_MATCHER.exec(name);
    if (matcher != null) {
      return matcher.getGroup(1);
    }
    return name;
  }

}
