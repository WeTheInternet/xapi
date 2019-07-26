package xapi.model.impl;


public class StringModel extends AbstractModel{

  public static final String STRING_KEY = "_s";

  public String getStringValue() {
    Object o = getProperty(STRING_KEY);
    if (o == null)
      return "";
    return o.toString();
  }
  public StringModel setStringValue(String value) {
    setProperty(STRING_KEY, value);
    return this;
  }

}
