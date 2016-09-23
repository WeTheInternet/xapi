package xapi.source.write;

import xapi.fu.Out1;

import java.util.function.Predicate;

/**
 * A simple interface for converting objects into strings.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface StringerMatcher {

  StringerMatcher DEFAULT_TO_STRINGER = new DefaultStringerMatcher();

  String toString(Object o);

  Predicate<String> matcherFor(String value);

}
final class DefaultStringerMatcher implements StringerMatcher {
  @Override
  public String toString(Object o) {
    return String.valueOf(
        o instanceof Out1 ? ((Out1)o).out1() : o
    );
  }

  @Override
  public Predicate<String> matcherFor(String value) {
    return value::equals;
  }
}
