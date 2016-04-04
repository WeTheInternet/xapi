package xapi.javac.dev.model;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public class SourceRange implements Comparable<SourceRange> {

  private final int start, end;

  public SourceRange(int start, int end) {
    this.start = start;
    this.end = end;
  }

  public int getEnd() {
    return end;
  }

  public int getStart() {
    return start;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof SourceRange))
      return false;

    final SourceRange that = (SourceRange) o;

    if (start != that.start)
      return false;
    return end == that.end;

  }

  @Override
  public int hashCode() {
    int result = start;
    result = 31 * result + end;
    return result;
  }

  @Override
  public int compareTo(SourceRange o) {
    if (start != o.start) {
      return start - o.start;
    }
    return end - o.end;
  }

  @Override
  public String toString() {
    return "Range[" + start + ":" + end + "]";
  }

  public String slice(String source) {
    return source.substring(start, end);
  }
}
