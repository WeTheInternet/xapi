package xapi.dev.resource.impl;

import java.io.IOException;
import java.io.InputStream;

import xapi.dev.resource.api.ClasspathResource;

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
