package xapi.dev.resource.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import xapi.dev.resource.api.ClasspathResource;

public class SourceCodeResource extends DelegateClasspathResource{

  public SourceCodeResource(ClasspathResource source) {
    super(source);
  }

  public String getSourceUnfafe() {
    try {
      return getSource();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
