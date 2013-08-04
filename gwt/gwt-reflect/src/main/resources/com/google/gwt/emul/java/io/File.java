package java.io;

/**
 * Emulated File class without any implementation whatsoever.
 * 
 * This is to allow (unused) references to File in shared code;
 * web mode can't use anything other than the File.separator / pathSeparator,
 * at least until we come up with a good virtualization strategy,
 * perhaps using web mode blobs and asynchronous access to files,
 * since we really can't block on reads in web mode.
 * 
 * A possibly better option would be to use the File object just to hold 
 * metadata from a server-side "file", and require web mode use of the 
 * Input/OutputStreams to use asynchronous methods,
 * so we can dispatch get/set through http, without blocking.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class File {

  //Web mode files are always uris
  public static final char separatorChar = '/';
  public static final String separator = "/";
  
  //Stick to unix separator. Plus ; is reserved in uri template syntax ;-}
  public static final char pathSeparatorChar = ':';
  public static final String pathSeparator = ":";
  
  //don't allow construction or implement any methods in web mode;
  //trying to use files should still throw errors,
  //until we can come up with a good virtualization strategy.
  private File() {}
  
}
