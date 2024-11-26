package xapi.test.components.bdd;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/17/17.
 */
public class SourceMatches {
  int expectedLine;
    String expected;
  int actualLine;
    String actual;

  public SourceMatches(int expectedLine, String expected) {
    this.expectedLine = expectedLine;
    this.expected = expected;
  }

  @Override
  public String toString() {
    return
        (expectedLine == 0 ? "\n" : "") +
        "[ " + expectedLine + "\t] " + expected;
  }
}
