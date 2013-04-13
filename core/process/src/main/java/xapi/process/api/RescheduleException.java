package xapi.process.api;


public class RescheduleException extends RuntimeException{

  private static final long serialVersionUID = 3093899335105799435L;
  private ProcessCursor cursor;

  public RescheduleException(ProcessCursor cursor) {
    this.cursor = cursor;
  }

}
