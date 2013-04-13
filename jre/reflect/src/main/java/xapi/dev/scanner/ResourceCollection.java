package xapi.dev.scanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import xapi.util.X_Debug;

public class ResourceCollection implements Iterable<ClasspathResource>{

  public class JarResourceIterable implements Iterable<ClasspathResource> {

    JarEntry entry;

    public JarResourceIterable(String pkg, JarFile jarFile) {
      entry = jarFile.getJarEntry(pkg);
      assert entry.isDirectory();
    }

    @Override
    public Iterator<ClasspathResource> iterator() {
      return new JarResourceIterator(entry);
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

    public JarResourceIterator(JarEntry entry) {
      // TODO Auto-generated constructor stub
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
        throw X_Debug.wrap(new FileNotFoundException());
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
      throw X_Debug.wrap(e);
    }
  }

}
