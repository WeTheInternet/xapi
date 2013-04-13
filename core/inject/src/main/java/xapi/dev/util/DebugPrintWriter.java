package xapi.dev.util;

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
    listener.write(new String(buf).getBytes(), off, len);
  }
  @Override
  public void write(int c) {
    super.write(c);
    listener.write(c);
  }
  @Override
  public void write(String s, int off, int len) {
    super.write(s, off, len);
    listener.write(s.getBytes(), off, len);
  }

}
