/**
 *
 */
package xapi.io.api;

/**
 * A simple exception thrown when a request is cancelled.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class CancelledException extends Exception{

  private final IORequest<?> request;

  public CancelledException(final IORequest<?> request) {
    super("Request cancelled: "+request.toString());
    this.request = request;
  }


  /**
   * @return -> request
   */
  public IORequest<?> getRequest() {
    return request;
  }
}
