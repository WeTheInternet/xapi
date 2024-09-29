package xapi.util.impl;

import xapi.collect.fifo.Fifo;
import xapi.collect.fifo.SimpleFifo;
import xapi.util.api.ReceivesValue;

/**
 * An object designed to delegate a received value to multiple ReceivesValue callbacks.
 *
 * <br/><br/>
 *
 * Fires callbacks based on insertion order. <br/>
 * If you need more control of firing order, use {@link ReceivesPrioritizedValue} instead.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 * @param <T> - The type of object / event / signal being sent.
 */
public class ReceivesMultiValue<T> implements ReceivesValue<T>{
  /** A simple array of callbacks to fire.  Uses low-level js arrays in gwt.*/
  final Fifo<ReceivesValue<T>> handlers = new SimpleFifo<ReceivesValue<T>>();

  public void set(T value) {
    while(!handlers.isEmpty()){
      handlers.take().set(value);
    }
  };
  /**
   * Adds a receiver to the end of the callback array.
   *
   * @param receiver - A new receiver to add
   * @return true if the receiver was added to our callbacks; false if already present.
   */
  public boolean addReceiver(ReceivesValue<T> receiver){
    assert receiver != null : "Do not send null receivers to "+this+"; (ReceivesMultiValue) ";
    assert receiver != this : "Do not send a ReceivesMultiValue to itself.  Class: "+this+";";

    if (handlers.contains(receiver))
      return false;
    handlers.give(receiver);
    return true;
  }

  /**
   * Clear our array of callbacks
   */
  public void clearReceivers(){
    handlers.clear();
  }
  /**
   * @param receiver - The receiver to remove.
   */
  public void removeReceiver(ReceivesValue<T> receiver){
    handlers.remove(receiver);
  }

}