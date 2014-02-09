package xapi.file.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
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

  private final static class Cleanup extends Thread {
    private final List<String> toKill = new ArrayList<String>();
    @Override
    public void run() {
      for (String kill : toKill) {
        rm(kill, Boolean.getBoolean("xapi.file.autorm"));
      }
    }
  }
  private static final Cleanup GC = new Cleanup();
  static {
    Runtime.getRuntime().addShutdownHook(GC);
  }
  
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
  public void delete(String kill, boolean recursive) {
    rm(kill, recursive);
  }

  private static void rm(String kill, boolean recursive) {
    File f = new File(kill);
    if (recursive) {
      HashSet<File> cycle = new HashSet<File>();
      rmRecursive(f, cycle);
    }
    if (f.exists() && !f.delete()) {
      X_Log.warn(FileServiceImpl.class, "Unable to delete file ",f);
    }
  }

  private static void rmRecursive(File f, HashSet<File> cycle) {
    if (cycle.add(f)) {
      if (f.isDirectory()) {
        for (File child : f.listFiles()) {
          rmRecursive(child, cycle); // Prevent symlink cycle recursion
        }
        if (!f.delete()) {
          X_Log.warn(FileServiceImpl.class,"Unable to delete",f);
        }
      } else if (f.isFile()) {
        if (!f.delete()) {
          X_Log.warn(FileServiceImpl.class,"Unable to delete",f);
        }
      }
    }
  }

  @Override
  public File createTempDir(String prefix, boolean deleteOnExit) {
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
      if (deleteOnExit) {
        GC.toKill.add(f.getCanonicalPath());
      }
      chmod(0x777, f);
    } catch (IOException e) {
      X_Log.warn("Unable to create temporary directory for ", prefix, e);
      X_Debug.maybeRethrow(e);
    }
    return f;
  }
  
  @Override
  public String getPath(String path) {
    try {
      return new File(path).getCanonicalPath();
    } catch (IOException e) {
      return new File(path).getAbsolutePath();
    }
  }
  
  @Override
  public String getFileMaybeUnzip(String file, int chmod) {
    File f = new File(file);
    if (f.getAbsolutePath().contains("jar!")) {
      try {
        return unzip(file, new JarFile(f), chmod);
      } catch (IOException e) {
        X_Log.error(getClass(), "Unable to unzip", f, "from file", file,"with chmod",Integer.toHexString(chmod), e);
      }
    }
    return getPath(file);
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
  public boolean saveFile(String path, String fileName, String contents) {
    return saveFile(path, fileName, contents, "UTF-8");
  }
  
  @Override
  public boolean saveFile(String path, String fileName, String contents, String charset) {
    File f = new File(path);
    if (!f.exists()) {
      if (!f.mkdirs()) {
        X_Log.warn("Unable to create parent directory", path,"in", f, new Throwable());
        return false;
      }
    }
    f = new File(f, fileName);
    if (!f.exists()) {
      try {
        f.createNewFile();
      } catch (IOException e) {
        X_Log.warn("Unable to create new file", fileName,"in", f, e);
        return false;
      }
    }
    try {
      X_IO.drain(new FileOutputStream(f), new ByteArrayInputStream(contents.getBytes()));
    } catch (IOException e) {
      X_Log.warn("Unable to save contents to file", f, e);
      return false;
    }
    return true;
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
  
  @Override
  public void mkdirsTransient(File dest) {
    if (!dest.exists()) {
      File parent = dest.getParentFile();
      List<File> parents = new ArrayList<File>();
      while (parent != null) {
        if (parent.exists()) {
          break;
        } else {
          parents.add(parent);
        }
        parent = parent.getParentFile();
      }
      if (!parents.isEmpty()) {
        dest.getParentFile().mkdirs();
        for (
            ListIterator<File> iter = parents.listIterator(parents.size());
            iter.hasNext();) {
          File prev = iter.next();
          prev.deleteOnExit();
        }
      }
    }
  }

}
