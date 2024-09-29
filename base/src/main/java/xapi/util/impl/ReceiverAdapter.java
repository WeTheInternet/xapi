package xapi.util.impl;

import xapi.util.api.ReceivesValue;

public class ReceiverAdapter <T> implements ReceivesValue<ReceivesValue<T>>{
  private final T value;
  public ReceiverAdapter(T value) {
    this.value = value;
  }
  @Override
  public void set(ReceivesValue<T> receiver) {
  	if (receiver != null)
  		receiver.set(value);
  }
}