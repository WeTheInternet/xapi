package xapi.javac.dev.model;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public class SourceMapPosition implements Comparable<SourceMapPosition> {

  private final int startRow, startColumn, endRow, endColumn;

  public SourceMapPosition(int startRow, int startColumn, int endRow, int endColumn) {
    this.startRow = startRow;
    this.startColumn = startColumn;
    this.endRow = endRow;
    this.endColumn = endColumn;
  }

  public SourceMapPosition(int start, int end, String source) {
    int cnt = 0;
    int row = 0;
    assert source.indexOf('\r') == -1 : "Please normalize newlines before creating SourceMapPosition. Carriage returns, \\r are not supported";
    String[] sourceLines = source.split("\n");
    if (start > end) {
      throw new IllegalArgumentException("Cannot have source range start after it ends. You sent [" + start +":" + end + "]");
    }
    if (end > source.length()) {
      throw new IllegalArgumentException("End index " + end+" longer than source file: " + source);
    }
    for (;;) {
      String sourceLine = sourceLines[row++];
      int len = sourceLine.length();
      int limit = cnt + len;
      if (start > limit) {
        cnt+=len;
        continue;
      }
      int col = 1;
      while (start + col < limit) {
        col++;
      }
      startRow = row;
      startColumn = col;
      if (end < limit) {
        // The end is on the same line.  lets go ahead and calculate now.
        while(end + col < limit) {
          col++;
        }
        endRow = row;
        endColumn = col;
        return;
      }
      break;
    }

    for (;;) {
      String sourceLine = sourceLines[row++];
      int len = sourceLine.length();
      int limit = cnt + len;
      if (end > limit) {
        cnt+=len;
        continue;
      }
      int col = 1;
      while (end + col < limit) {
        col++;
      }
      endRow = row;
      endColumn = col;
      return;
    }
  }

  public int getStartRow() {
    return startRow;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public int getEndRow() {
    return endRow;
  }

  public int getEndColumn() {
    return endColumn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof SourceMapPosition))
      return false;

    final SourceMapPosition that = (SourceMapPosition) o;

    if (startRow != that.startRow)
      return false;
    if (startColumn != that.startColumn)
      return false;
    if (endRow != that.endRow)
      return false;
    return endColumn == that.endColumn;

  }

  @Override
  public int hashCode() {
    int result = startRow;
    result = 31 * result + startColumn;
    result = 31 * result + endRow;
    result = 31 * result + endColumn;
    return result;
  }

  @Override
  public String toString() {
    return "Range[" +
        startRow + "," + startColumn +
        ":" + endRow + "," + endColumn +
        ']';
  }

  @Override
  public int compareTo(SourceMapPosition o) {
    if (startRow != o.startRow) {
      return startRow - o.startRow;
    }
    if (startColumn != o.startColumn) {
      return startColumn - o.startColumn;
    }
    if (endRow != o.endRow) {
      return endRow - o.endRow;
    }
    return endColumn - o.endColumn;
  }
}
