package xapi.util.service;

public interface StringService {

  String[] binarySuffix = new String[]{"","K","M","G","T","P","E","Z"};
  String[] metricSuffix = new String[]{" nano"," micro"," milli"," "," kilo"," mega"," giga"};
  boolean notNullOrEmpty(String str);
  String notNullOrEmpty(String str,String dflt);
  byte[] toBytes(String str);

}
