package xapi.dev.exceptions;

public class GeneratorFailedException extends RuntimeException{

  private static final long serialVersionUID = 7606971743497236355L;


  public GeneratorFailedException(String message) {
    super(message);
  }
  public GeneratorFailedException(String message, Throwable e) {
    super(message, e);
  }

}
