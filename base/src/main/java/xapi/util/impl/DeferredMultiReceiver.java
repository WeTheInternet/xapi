package xapi.util.impl;

import xapi.collect.fifo.Fifo;
import xapi.collect.fifo.SimpleFifo;
import xapi.util.api.ProvidesValue;
import xapi.util.api.ReceivesValue;

public class DeferredMultiReceiver <X> implements ProvidesValue<ReceivesValue<X>>{
  private final Fifo<ProvidesValue<ReceivesValue<X>>> providers;
  private final ReceivesValue<X> adapter;
  public DeferredMultiReceiver() {
    providers = new SimpleFifo<ProvidesValue<ReceivesValue<X>>>();
    adapter = new ReceivesValue<X>(){
      @Override
      public void set(X value) {
        ProvidesValue<ReceivesValue<X>> local;
        while((local=providers.take())!=null){
          local.get().set(value);
        }
        local = null;
      }
    };
  }

  public final void add(ProvidesValue<ReceivesValue<X>> provider){
    assert !providers.contains(provider) : new RuntimeException("You have sent the same provider instance to a DeferredMultiReceiver more than once: "+provider);
    providers.give(provider);
  }
  public final void add(final ReceivesValue<X> provider){
    ProviderAdapter<X> adapter = new ProviderAdapter<X>();
    adapter.set(provider);
    providers.give(adapter);
  }

  @Override
  public final ReceivesValue<X> get() {
    return adapter;
  }
}