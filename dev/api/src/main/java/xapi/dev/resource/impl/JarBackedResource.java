package xapi.dev.resource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import xapi.dev.resource.api.ClasspathResource;

public class JarBackedResource implements ClasspathResource{

  private final JarEntry entry;
  private final JarFile jar;
  private final int priority;

  public JarBackedResource(JarFile file, JarEntry entry, int priority) {
    this.entry = entry;
    this.jar = file;
    this.priority = priority;
  }

  @Override
  public String getResourceName() {
    return entry.getName();
  }

  @Override
  public InputStream open() throws IOException{
    return jar.getInputStream(entry);
  }

  @Override
  public int priority() {
    return priority;
  }

}
