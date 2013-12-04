package xapi.file.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import xapi.annotation.inject.SingletonDefault;
import xapi.file.api.FileService;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.util.X_Debug;

@SingletonDefault(implFor=FileService.class)
public class FileServiceImpl implements FileService {

  @Override
  public File chmod(int chmod, File file) {
    assertValidChmod(chmod);
    if (file.exists()) {
      {
        final boolean isExecutable = (chmod & 0x111) > 0;
        try {
          file.setExecutable(isExecutable, isExecutable ? (chmod & 0x011) == 0 : (chmod & 0x11) > 0);
        } catch (SecurityException e) {
          file.setExecutable(isExecutable);
        }
      }
      {
        final boolean isWritable = (chmod & 0x222) > 0;
        try {
          file.setWritable(isWritable, isWritable ? (chmod & 0x022) == 0 : (chmod & 0x2) > 0);
        } catch (SecurityException e) {
          file.setWritable(isWritable);
        }
      }
      {
        final boolean isReadable = (chmod & 0x444) > 0;
        try {
          file.setReadable(isReadable, isReadable? (chmod & 0x044) == 0 : (chmod & 0x44) > 0);
        } catch (SecurityException e) {
          file.setReadable(isReadable);
        }
      }
    }
    return file;
  }
  
  @Override
  public File createTempDir(String prefix) {
    File f = null;
    try {
      f = File.createTempFile(prefix, "");
      try {
        f.delete();
      } catch (Exception e) {
        chmod(0x444, f);
        f.delete();
      }
      f.mkdirs();
      chmod(0x777, f);
      f.deleteOnExit();
    } catch (IOException e) {
      X_Log.warn("Unable to create temporary directory for ", prefix, e);
      X_Debug.maybeRethrow(e);
    }
    return f;
  }
  
  @Override
  public String getResourceMaybeUnzip(String resource, ClassLoader cl, int chmod) {
    if (cl == null) {
      cl = Thread.currentThread().getContextClassLoader();
    }
    if (cl == null) {
      cl = getClass().getClassLoader();
    }
    URL url = cl.getResource(resource);
    try {
      if (url == null)
        throw new RuntimeException("Resource "+resource +" not available on classpath.");
      if (url.getProtocol().equals("file")) {
        String loc = url.toExternalForm();
        if (loc.contains("jar!")) {
          return unzip(resource, new JarFile(X_Source.stripJarName(loc)), chmod);
        } else {
          String file = X_Source.stripFileName(loc); 
          chmod(chmod, new File(file));
          return file;
        }
      } else if (url.getProtocol().equals("jar")) {
        return unzip(resource, ((JarURLConnection)(url.openConnection())).getJarFile(), chmod);
      } else {
        X_Log.warn("Unknown get resource protocol "+url.getProtocol());
      }
    } catch (Throwable e) {
      X_Log.error("Error trying to load / unzip resouce "+resource+" using file "+url, e);
      X_Debug.maybeRethrow(e);
    }
    return null;
  }


  @Override
  public String unzip(String resource, JarFile jarFile, int chmod) {
    final ZipEntry entry = jarFile.getEntry(resource);
    String fileName = null;
    final File file;
    try {
      // Don't close this jar; it's in use by the classloader
      InputStream is = jarFile.getInputStream(entry);
      file = File.createTempFile(resource.replace('/', '_'), "");
      fileName = file.getCanonicalPath();
      try (FileOutputStream fOut = new FileOutputStream(file)) {
        X_IO.drain(fOut, is);
      }
      chmod(chmod, file);
      file.deleteOnExit();
      return fileName;
    } catch (Throwable e) {
      X_Log.error("Error encountered unzipping jar entry",resource,"from",jarFile," Entry:", entry, e);
    }
    return fileName;
  }

  protected void assertValidChmod(int chmod) {
    assert isHexadecimalChmod(chmod) : "Do not send "
        + (chmod > 0x777 ? "values greated than 0x777" : "decimal values")
        + " to X_File.chmod; you sent "+chmod+
      " when you really meant to send 0x"+chmod+"="+Integer.parseInt(Integer.toString(chmod), 16);
  }

  /**
   * This test is an APPROXIMATION of whether a given integer is hexadecimal,
   * and is used only in an assertion statement warding off malformed permissions.
   * 
   * Any value > 777 and < 0x777 is automatically deemed valid;
   * after that, all we can do is check each number's 4th bit, (chmod & 888 == 0)
   * 
   * @param chmod - The chmod value to check
   * @return true is this value COULD be a valid hexadecimal chmod.
   * returns chmod < 0x778 && (chmod & 888) == 0;
   * 
   */
  protected static boolean isHexadecimalChmod(int chmod) {
    return chmod < 0x778 && ((chmod & 0x888) == 0);
  }

}
