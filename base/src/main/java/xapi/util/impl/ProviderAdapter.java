package xapi.util.impl;

import xapi.util.api.ProvidesValue;
import xapi.util.api.ReceivesValue;

public class ProviderAdapter <X> implements ProvidesValue<ReceivesValue<X>>, ReceivesValue<ReceivesValue<X>>{

  private ReceivesValue<X> receiver;

  @Override
  public ReceivesValue<X> get() {
    return receiver;
  }
  @Override
  public void set(ReceivesValue<X> receiver) {
    this.receiver = receiver;
  }
  @Override
  public boolean equals(Object obj) {
    if (obj == this)return true;
    if (!(obj instanceof ProviderAdapter))return false;
    ProviderAdapter<?> that = ((ProviderAdapter<?>)obj);
    if (receiver == null)return that.receiver == null;
    return this.receiver == that.receiver;
  }
}