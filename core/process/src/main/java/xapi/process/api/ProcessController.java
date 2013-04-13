package xapi.process.api;

public class ProcessController <T> {

  private final Process<T> process;

  public ProcessController(Process<T> process) {
    this.process = process;
  }

}
