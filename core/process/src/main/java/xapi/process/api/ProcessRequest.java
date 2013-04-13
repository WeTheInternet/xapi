package xapi.process.api;

public interface ProcessRequest {

  String[] arguments();
  
  Class<?> mainClass();
  
  String owner();
  
}
