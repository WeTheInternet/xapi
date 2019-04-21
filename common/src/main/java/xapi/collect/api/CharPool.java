package xapi.collect.api;

/**
 * A char[] interner; for reducing memory footprint down to the minimum set
 * of unique characters possible.
 * <p>
 * We prefer char[] over interned strings in mission critical sections,
 * as they use less memory; a smart collection can compress
 * <p>
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface CharPool {

  final char[] EMPTY_STRING = new char[0];

  char[] getArray(char[] src);
  char[] getArray(char[] src, int start, int len);
  char[] getArray(CharSequence src);
  char[] getArray(CharSequence src, int start, int len);
}
