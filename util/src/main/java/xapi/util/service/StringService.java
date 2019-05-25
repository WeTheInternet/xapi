package xapi.util.service;

public interface StringService {

  boolean notNullOrEmpty(String str);
  String notNullOrEmpty(String str,String dflt);
  byte[] toBytes(String str);

}
