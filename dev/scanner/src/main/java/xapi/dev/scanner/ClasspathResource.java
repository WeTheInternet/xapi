package xapi.dev.scanner;

import java.io.IOException;
import java.io.InputStream;

public interface ClasspathResource {

  String getResourceName();
  InputStream open() throws IOException;
  int priority();

}