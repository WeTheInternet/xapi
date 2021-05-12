package xapi.dev.resource.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import xapi.dev.resource.api.ClasspathResource;
import xapi.debug.X_Debug;
import xapi.util.X_Util;

public class StringDataResource extends DelegateClasspathResource{

  public StringDataResource(ClasspathResource source) {
    super(source);
  }

  /**
   * @throws IOException
   */
  public Iterable<String> readLines() throws IOException {
    return new Iterable<String>(){
      @Override
      public Iterator<String> iterator() {
        try {
          // reader is closed by StringReader
          BufferedReader reader = new BufferedReader(new InputStreamReader(open()));
          return new StringReader(reader);
        } catch (IOException e) {
          throw X_Util.rethrow(e);
        }
      }
    };
  }

  public String readAll() {
    StringBuilder b = new StringBuilder();
    try {
      for (String line : readLines())
        b.append(line).append('\n');
      return b.toString();
    } catch (IOException e) {
      throw X_Debug.rethrow(e);
    }
  }

}
class StringReader implements Iterator<String> {

  private BufferedReader reader;
  private String next;

  public StringReader(BufferedReader reader) {
    this.reader = reader;
  }

  @Override
  public boolean hasNext() {
    try {
      next = reader.readLine();
      if (next == null) {
        reader.close();
        return false;
      }
      return true;
    }catch(IOException e) {
      throw X_Util.rethrow(e);
    }
  }

  @Override
  public String next() {
    return next;
  }

  @Override
  public void remove() {
  }

}