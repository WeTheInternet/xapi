package xapi.dev.resource.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import xapi.dev.resource.api.ClasspathResource;
import xapi.util.X_Util;

public class ResourceCollection implements Iterable<ClasspathResource>{

  public class JarResourceIterable implements Iterable<ClasspathResource> {

    final JarEntry entry;
    private final JarFile jarFile;

    public JarResourceIterable(String pkg, JarFile jarFile) {
      entry = jarFile.getJarEntry(pkg);
      this.jarFile = jarFile;
      assert entry.isDirectory();
    }

    @Override
    public Iterator<ClasspathResource> iterator() {
      return new JarResourceIterator(entry, jarFile);
    }

  }

  public class FileResourceIterable implements Iterable<ClasspathResource> {

    private File file;

    public FileResourceIterable(File file) {
      assert file.isDirectory();
      this.file = file;
    }

    @Override
    public Iterator<ClasspathResource> iterator() {
      return new FileResourceIterator(file);
    }

  }

  protected static class FileResourceIterator implements Iterator<ClasspathResource> {

    File root;
    File[] directory;
    File parent;

    public FileResourceIterator(File file) {
      root = file;
      directory = file.listFiles();
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public ClasspathResource next() {
      return null;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("ResouceCollection does not support Iterator.remove()");
    }

  }

  protected static class JarResourceIterator implements Iterator<ClasspathResource> {

    private JarFile jarFile;
    private JarEntry jarEntry;
    private final int priority;

    public JarResourceIterator(JarEntry entry, JarFile jarFile) {
      this(entry, jarFile, 0);
    }
    public JarResourceIterator(JarEntry entry, JarFile jarFile, int priority) {
      this.jarEntry = entry;
      this.jarFile = jarFile;
      this.priority = priority;
    }

    @Override
    public boolean hasNext() {
      return jarFile != null;
    }

    @Override
    public ClasspathResource next() {
      try {
        return new JarBackedResource(jarFile, jarEntry, priority);
      } finally {
        jarFile = null;
        jarEntry = null;
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("ResouceCollection does not support Iterator.remove()");
    }
  }

  private final Iterable<ClasspathResource> iter;
  public ResourceCollection(File file) {
    iter = new FileResourceIterable(file);
  }

  public ResourceCollection(String pkg, JarFile jarFile) {
    pkg = pkg.replace('\\', '/');
    if (!pkg.endsWith("/"))
      pkg = pkg + "/";
    iter = new JarResourceIterable(pkg, jarFile);
  }

  @Override
  public Iterator<ClasspathResource> iterator() {
    return iter.iterator();
  }

  static ResourceCollection fromUrl(URL url, String pkg) {
    String path = url.toExternalForm();
    File file;
    boolean jarUrl = path.startsWith("jar:");
    if (jarUrl) path = path.substring("jar:".length());
    boolean fileUrl = path.startsWith("file:");
    if (fileUrl) path = path.substring("file:".length());
    boolean jarFile = path.contains(".jar!");
    if (jarFile) path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
    if (!(file = new java.io.File(path)).exists()) {
      path = path.replace("%20", " ");
      if (!(file = new java.io.File(path)).exists()) {
        //should be impossible since we get these urls from classloader
        throw X_Util.rethrow(new FileNotFoundException());
      }
    }
    try {
      //TODO getOrMake; use an InitWithParamMap
      if (url.getProtocol().equals("jar")) {
        return new ResourceCollection(pkg, ((JarURLConnection)url.openConnection()).getJarFile());
      }
      assert url.getProtocol().equals("file") : "ResourceCollection only handles url and file protocols";

      if (jarFile) {
        return new ResourceCollection(pkg, new JarFile(file));
      }
      return new ResourceCollection(file);
    }catch (IOException e) {
      throw X_Util.rethrow(e);
    }
  }

}
