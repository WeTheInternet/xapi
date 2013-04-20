package xapi.process.api;


@SuppressWarnings("rawtypes")
public class RescheduleException extends RuntimeException{

  private static final long serialVersionUID = 3093899335105799435L;

  private final ProcessCursor cursor;

  public RescheduleException(ProcessCursor cursor) {
    this.cursor = cursor;
  }

  public ProcessCursor getCursor() {
    return cursor;
  }

}
