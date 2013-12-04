package xapi.dev.resource.api;

import java.io.IOException;
import java.io.InputStream;

public interface ClasspathResource {

  String getResourceName();
  InputStream open() throws IOException;
  int priority();

}