package com.google.gwt.reflect.client.strategy;


public class UseGwtCreate extends NewInstanceStrategy{

  public static final UseGwtCreate SINGLETON = new UseGwtCreate();

  public UseGwtCreate() {
    super("GWT.create(*)", "[*]");
  }

}
