package xapi.file.api;

import java.io.File;
import java.util.jar.JarFile;

public interface FileService {

  String getResourceMaybeUnzip(String resource, ClassLoader cl, int chmod);

  File createTempDir(String prefix);

  String unzip(String resource, JarFile jarFile, int chmod);


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
  File chmod(int chmod, File file);
  
}
