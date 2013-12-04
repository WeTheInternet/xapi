package xapi.gwtc.compiler.model;

import java.io.File;

public class ClasspathEntry {

  private boolean isDirectory;
  private boolean maven;
  private boolean remote;
  private String url;

  public boolean isDirectory() {
    return isDirectory;
  }

  public ClasspathEntry setDirectory(boolean isDirectory) {
    this.isDirectory = isDirectory;
    return this;
  }
  
  public String getDirectory() {
    String url = getUrl();
    if (isDirectory())
      return url;
    int ind = url.lastIndexOf(File.separatorChar);
    if (ind == -1)
      return url;
    return url.substring(0, ind);
  }

  public boolean isMaven() {
    return maven;
  }

  public ClasspathEntry setMaven(boolean maven) {
    this.maven = maven;
    return this;
  }

  public boolean isRemote() {
    return remote;
  }

  public ClasspathEntry setRemote(boolean remote) {
    this.remote = remote;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public ClasspathEntry setUrl(String url) {
    this.url = url;
    return this;
  }

}
