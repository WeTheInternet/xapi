package xapi.io.api;


public interface LineReader {
  void onStart();
  void onLine(String line);
  void onEnd();
}