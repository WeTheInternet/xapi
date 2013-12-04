package xapi.io.api;

/**
 * An interface for reading lines of text,
 * with notification methods for the beginning and end of a stream of lines.
 * 
 * When text is delivered, new lines should be stripped; if you wish to preserve newlines,
 * you must put them back yourself.
 * 
 * Some implementations, like {@link StringReader} support methods to block
 * on a given reader, as a handy means to wait until some other process has completed
 * (see {@link StringReader#waitToEnd()}). 
 * 
 * @author "james@wetheinter.net"
 *
 */
public interface LineReader {
  void onStart();
  void onLine(String line);
  void onEnd();
}