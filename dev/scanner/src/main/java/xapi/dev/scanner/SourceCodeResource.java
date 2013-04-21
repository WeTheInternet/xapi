package xapi.dev.scanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SourceCodeResource extends DelegateClasspathResource{

  public SourceCodeResource(ClasspathResource source) {
    super(source);
  }

  public String getSource() throws IOException{
    BufferedReader read = new BufferedReader(new InputStreamReader(open()));
    String next;
    StringBuilder b = new StringBuilder();
    while ((next = read.readLine())!=null) {
      b.append(next);
      b.append('\n');
    }
    return b.toString();
  }

}
