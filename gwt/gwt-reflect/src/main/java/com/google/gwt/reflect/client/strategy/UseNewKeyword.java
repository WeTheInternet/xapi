package com.google.gwt.reflect.client.strategy;


public class UseNewKeyword extends NewInstanceStrategy {

  public static final UseNewKeyword SINGLETON = new UseNewKeyword();

  public UseNewKeyword() {
    super("new *()", "[*]");
  }

}
