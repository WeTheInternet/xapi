package xapi.jre.util;

import java.nio.charset.Charset;

import xapi.annotation.inject.SingletonDefault;
import xapi.util.service.StringService;

@SingletonDefault(implFor=StringService.class)
public class JreStringService implements StringService {

  private static final Charset UTF8;
  static {
    UTF8 = Charset.forName("UTF8");
  }

  @Override
  public boolean notNullOrEmpty(String str) {
    return str != null && str.length() != 0;
  }

  @Override
  public String notNullOrEmpty(String str, String dflt) {
    return str == null || str.length() == 0 ? dflt : str;
  }

  @Override
  public byte[] toBytes(String str) {
    return str.getBytes(UTF8);
  }

}
