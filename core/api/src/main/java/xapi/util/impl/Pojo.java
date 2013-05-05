package xapi.util.impl;

import javax.inject.Provider;

import xapi.util.api.Bean;

public class Pojo <X> implements Provider<X>, Bean<X>{

  private X data;

  public Pojo() {
  }
  public Pojo(X x) {
    set(x);
  }
  
  @Override
  public X get() {
    return data;
  }
  @Override
  public void set(X x) {
    data = x;
  }
}
