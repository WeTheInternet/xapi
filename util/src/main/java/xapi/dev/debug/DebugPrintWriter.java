package xapi.dev.debug;

import java.io.PrintStream;
import java.io.PrintWriter;

public class DebugPrintWriter extends PrintWriter{

  private final PrintStream listener;

  public DebugPrintWriter(PrintWriter delegate) {
    this(delegate, System.out);
  }

  public DebugPrintWriter(PrintWriter delegate, PrintStream listener) {
    super(delegate);
    this.listener = listener;
  }

  @Override
  public void println() {
    super.println();
    listener.println();
  }
  @Override
  public void write(char[] buf, int off, int len) {
    super.write(buf, off, len);
    try {
        listener.write(new String(buf).getBytes(), off, len);
    } catch (Exception e) {
        throw new RuntimeException("Unable to write " + new String(buf), e);
    }
  }
  @Override
  public void write(int c) {
    try {
        super.write(c);
        listener.write(c);
    } catch (Exception e) {
        throw new RuntimeException("Unable to write " + c, e);
    }
  }
  @Override
  public void write(String s, int off, int len) {
    try {
        super.write(s, off, len);
        listener.write(s.getBytes(), off, len);
    } catch (Exception e) {
        throw new RuntimeException("Unable to write " + s, e);
    }
  }

}
