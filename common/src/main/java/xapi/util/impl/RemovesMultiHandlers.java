package xapi.util.impl;

import xapi.collect.fifo.Fifo;
import xapi.collect.fifo.SimpleFifo;
import xapi.util.api.RemovalHandler;

/**
 * Handy wrapper class for bundling multiple RemovesHandlers into a single callback.
 *
 * Most often used to combie multiple RemovalHandler objects into a single returned object.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class RemovesMultiHandlers implements RemovalHandler{

  private final Fifo<RemovalHandler> handles;
  public RemovesMultiHandlers() {
    // we by default use the simple, concurrent fifo, to avoid dependency
    // on the injection library too early in our module structure
    handles = new SimpleFifo<RemovalHandler>();
  }

  @Override
  public void remove() {
    while(!handles.isEmpty()){
      RemovalHandler handle = handles.take();
      handle.remove();
    }
  }
  /**
   * Push a handler onto our callback stack
   *
   * @param handle - Handler to be called on remove
   * @return - this, for chaining
   */
  public RemovesMultiHandlers addHandler(RemovalHandler handle){
    handles.give(handle);
    return this;
  }

}