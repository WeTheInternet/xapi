package xapi.file;

import xapi.file.api.FileService;
import xapi.fu.MappedIterable;
import xapi.inject.X_Inject;

import javax.inject.Provider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.jar.JarFile;

public class X_File {

  private static final Provider<FileService> SERVICE = X_Inject.singletonLazy(FileService.class);

  /**
   * Performs a chmod-like operation on a file.
   *
   * Special group permissions are ignored by java 6 and <,
   * but owner/all permissions are settable via HEXADECIMAL integer flags.
   *
   * 0x755 != 755.
   *
   * A java 7 implementation will fully respect {@link java.nio.file.attribute.PosixFilePermission}.
   *
   * A method may be provided to translate decimal input,
   * but for now, there is an assertion guarding this method.
   *
   * @param chmod - A HEXADECIMAL value < 0x777
   * @param file - The file to apply the setting upon
   * @return That same file.
   */
  public static File chmod(int chmod, final File file) {
    return SERVICE.get().chmod(chmod, file);
  }

  public static String unzip(String resource, JarFile jarFile) {
    return unzip(resource, jarFile, 0x755);
  }

  public static String unzip(String resource, JarFile jarFile, int chmod) {
    return SERVICE.get().unzip(resource, jarFile, chmod);
  }

  public static InputStream unzipFile(String file) throws FileNotFoundException {
    String path = unzippedFilePath(file, 0x755);
    return new FileInputStream(path);
  }

  public static InputStream unzipFile(String file, int chmod) throws FileNotFoundException {
    String path = unzippedFilePath(file, chmod);
    return new FileInputStream(path);
  }

  public static InputStream unzipResource(String resource, ClassLoader cl) throws FileNotFoundException {
    return unzipResource(resource, cl, 0x755);
  }

  public static InputStream unzipResource(String resource, ClassLoader cl, int chmod) throws FileNotFoundException {
    String path = unzippedResourcePath(resource, cl, chmod);
    return new FileInputStream(path);
  }

  public static String unzippedResourcePath(String resource, ClassLoader cl) {
    return unzippedResourcePath(resource, cl, 0x755);
  }

  public static String unzippedResourcePath(String resource, ClassLoader cl, int chmod) {
    return SERVICE.get().getResourceMaybeUnzip(resource, cl, chmod);
  }

  public static String unzippedFilePath(String file, int chmod) {
    return SERVICE.get().getFileMaybeUnzip(file, chmod);
  }

  public static File createTempDir(String prefix) {
    return SERVICE.get().createTempDir(prefix, false);
  }

  public static File createTempDir(String prefix, boolean deleteOnExit) {
    return SERVICE.get().createTempDir(prefix, deleteOnExit);
  }

  public static String getPath(String path) {
    return SERVICE.get().getPath(path);
  }

  public static void saveFile(String path, String fileName, String contents) {
    SERVICE.get().saveFile(path, fileName, contents);
  }

  public static void mkdirsTransient(File dest) {
    SERVICE.get().mkdirsTransient(dest);
  }

  public static void deepDelete(String dir) {
    if (dir != null) {
      SERVICE.get().delete(dir, true);
    }
  }

    public static MappedIterable<String> getAllFiles(String file) {
      return SERVICE.get().getAllFiles(file);
    }
}
