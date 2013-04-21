package xapi.dev.scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileBackedResource implements ClasspathResource{

  private final File file;
  private final int priority;
  private final String resourceName;

  public FileBackedResource(String resourceName, File file, int priority) {
    this.resourceName = resourceName;
    this.file = file;
    this.priority = priority;
  }

  @Override
  public String getResourceName() {
    return resourceName;
  }

  @Override
  public InputStream open() throws IOException{
    return new FileInputStream(file);
  }

  @Override
  public int priority() {
    return priority;
  }

}
