package xapi.dev.scanner;

import java.io.IOException;
import java.io.InputStream;

public class DelegateClasspathResource  implements ClasspathResource{

  private final ClasspathResource source;

  public DelegateClasspathResource(ClasspathResource source) {
    this.source = source;
  }

  @Override
  public final String getResourceName() {
    return source.getResourceName();
  }

  @Override
  public final InputStream open() throws IOException {
    return source.open();
  }

  @Override
  public final int priority() {
    return source.priority();
  }

}
