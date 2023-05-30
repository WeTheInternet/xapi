package xapi.file;

import xapi.file.api.FileService;
import xapi.fu.In2;
import xapi.fu.Out1;
import xapi.fu.itr.MappedIterable;
import xapi.inject.X_Inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.jar.JarFile;

public class X_File {

  private static final Out1<FileService> SERVICE = X_Inject.singletonLazy(FileService.class);

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
    return SERVICE.out1().chmod(chmod, file);
  }

  /**
   * just like the sh ln command, the first argument is the "real file" the created link points to.
   * @param linkSource A single string which defines where the symlink leads to
   * @param linkFiles One or more symlink files to create, which point to linkSource
   * @return true if all link files were either created, and exist, pointing to the correct location.
   */
  public static boolean ln(String linkSource, String ... linkFiles) {
    return SERVICE.out1().ln(linkSource, linkFiles);
  }
  public static String unzip(String resource, JarFile jarFile) {
    return unzip(resource, jarFile, 0x755);
  }

  public static String unzip(String resource, JarFile jarFile, int chmod) {
    return SERVICE.out1().unzip(resource, jarFile, chmod);
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
    return SERVICE.out1().getResourceMaybeUnzip(resource, cl, chmod);
  }

  public static String unzippedFilePath(String file, int chmod) {
    return SERVICE.out1().getFileMaybeUnzip(file, chmod);
  }

  public static File createTempDir(String prefix) {
    return SERVICE.out1().createTempDir(prefix, false);
  }

  public static File createTempDir(String prefix, boolean deleteOnExit) {
    return SERVICE.out1().createTempDir(prefix, deleteOnExit);
  }

  public static String getPath(String path) {
    return SERVICE.out1().getPath(path);
  }

  public static void saveFile(String path, String fileName, String contents) {
    SERVICE.out1().saveFile(path, fileName, contents);
  }

  public static void mkdirsTransient(File dest) {
    SERVICE.out1().mkdirsTransient(dest);
  }

  public static void deepDelete(String dir) {
    if (dir != null) {
      SERVICE.out1().delete(dir, true);
    }
  }

    public static MappedIterable<String> getAllFiles(String file) {
      return SERVICE.out1().getAllFiles(file);
    }

  public static void loadFile(File file, In2<String, Throwable> callback) {
    SERVICE.out1().loadFile(file, callback);
  }

  public static boolean exists(String s) {
    return SERVICE.out1().exists(s);
  }
}
