package xapi.file;

import java.io.File;
import java.util.jar.JarFile;

import javax.inject.Provider;

import xapi.file.api.FileService;
import xapi.inject.X_Inject;

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

  public static String getResourceMaybeUnzip(String resource, ClassLoader cl) {
    return getResourceMaybeUnzip(resource, cl, 0x755);
  }
  
  public static String getResourceMaybeUnzip(String resource, ClassLoader cl, int chmod) {
    return SERVICE.get().getResourceMaybeUnzip(resource, cl, chmod);
  }

  public static File createTempDir(String prefix) {
    return SERVICE.get().createTempDir(prefix);
  }
  
}
