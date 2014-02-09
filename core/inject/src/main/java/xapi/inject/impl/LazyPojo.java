package xapi.inject.impl;

import xapi.util.api.Bean;
import xapi.util.api.ReceivesValue;
import xapi.util.impl.Pojo;

public abstract class LazyPojo <X> extends SingletonInitializer<X> implements Bean<X>{

  protected class NullCheckOnSet extends Pojo<X>{
    public NullCheckOnSet(X init) {
      super.set(init);
    }
    @Override
    public void set(X x) {
      if (x == null){
        onDestroyed(get());
        reset();
      }else
        super.set(x);
    };
  }

  protected void onDestroyed(X x){

  }

  public void reset() {
    if (isSet()){
      onDestroyed(get());
    }
    proxy = new NullCheckOnGet();
  }

  @Override
  public void set(X x) {
    //set our proxy correctly
    if (x == null){
      reset();
    }else
      proxy = createImmutableProvider(x);
  }

  @Override
  protected javax.inject.Provider<X> createImmutableProvider(X init) {
    return new NullCheckOnSet(init);
  };
  @Override
  public boolean isSet(){
    return proxy instanceof ReceivesValue;
  }
}
