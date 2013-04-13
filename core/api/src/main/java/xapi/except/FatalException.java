package xapi.except;

/**
 * A runtime exception indicating that failure is not recoverable.
 *
 * You may mark any fatal exception as {@link IsFatal},
 * and XApi error handlers will use an if (instanceof IsFatal) to avoid retries.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class FatalException extends RuntimeException implements IsFatal{

  private static final long serialVersionUID = 5941204730134282829L;

  protected FatalException() {//for serialization only
  }

  public FatalException(String reason) {
    super(reason);
  }

  public FatalException(String reason, Throwable cause) {
    super(reason, cause);
  }

}
