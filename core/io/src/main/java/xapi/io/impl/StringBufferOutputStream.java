package xapi.io.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class StringBufferOutputStream extends OutputStream {

  private String charset;
  private final ByteArrayOutputStream bout = new ByteArrayOutputStream();

  public StringBufferOutputStream() {
    this("UTF-8");
  }
  
  public StringBufferOutputStream(String charset) {
    this.charset = charset;
  }
  
  @Override
  public void write(int b) throws IOException {
    bout.write(b);
  }
  
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    bout.write(b, off, len);
  }
  
  public String getContent() {
    return toString();
  }
  
  public String toString() {
    try {
      return new String(bout.toByteArray(), charset);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return new String(bout.toByteArray());
    }
  }

  @Override
  public void close() throws IOException {
    bout.close();
  }
  
}
