package com.google.gwt.reflect.client.strategy;


public abstract class NewInstanceStrategy {

  private final String[] bits;

  public NewInstanceStrategy(String template, String replacement) {
    bits = template.split(replacement);
  }

  public String generate(String className) {
    if (bits.length == 0)
      return "";
    StringBuilder b = new StringBuilder(bits[0]);
    for (int i = 1, m = bits.length; i < m; i++) {
      b.append(className).append(bits[i]);
    }
    String def = b.toString();
    if (def.trim().matches("(return .*)|(throw .*)"))
      return def;
    return "return " + def+";";
  }

}
