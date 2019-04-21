package xapi.util.impl;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.util.api.ReceivesValue;

/**
 * An extension of {@link ReceivesMultiValue} which uses three prioritized callback buckets.
 * <br/><br/>
 *
 * {@link #addPre(ReceivesValue, boolean)} -
 * pushes callback onto stack called before all others.
 * Send true to push on head of pre stack
 * <br/><br/>
 *
 * {@link #addPost(ReceivesValue)} -
 * pushes callback onto bottom of the stack called after all others.
 * Send true to push on head of pre stack
 * <br/><br/>
 *
 * {@link #addToHead(ReceivesValue)} -
 * push callback on top of main callback stack
 * <br/><br/>
 *
 * {@link #addReceiver(ReceivesValue)} -
 * push callback on bottom of main callback stack
 * <br/><br/>
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 * @param <T>
 */
public class ReceivesPrioritizedValue<T> extends ReceivesMultiValue<T>{
  //our extra callback stacks
  final Fifo<ReceivesValue<T>> pre = new SimpleFifo<ReceivesValue<T>>();
  final Fifo<ReceivesValue<T>> post = new SimpleFifo<ReceivesValue<T>>();

  /**
   * Adds a callback that will be fired before the main and post callback stacks
   *
   * @param receiver - The receiver to add to pre-fire stack
   * @param top - true to unshift onto head, false to push onto tail
   */
  public void addPre(ReceivesValue<T> receiver){
    assert receiver != null : "Do not send null receivers to "+this+"; (ReceivesMultiValue.addBefore) ";
    assert receiver != this : "Do not send a ReceivesMultiValue to itself.  Class: "+this+";";
    pre.give(receiver);
  }
  /**
   * Adds a callback that will be fired after the pre and main callback stacks
   *
   * @param receiver - The receiver to add to pre-fire stack
   * @param top - true to unshift onto head, false to push onto tail
   */
  public void addPost(ReceivesValue<T> receiver){
    assert receiver != null : "Do not send null receivers to "+this+"; (ReceivesMultiValue.addAfter) ";
    assert receiver != this : "Do not send a ReceivesMultiValue to itself.  Class: "+this+";";
    post.give(receiver);
  }
  /**
   * Send an object to all of our callbacks, in prioritized order.
   */
  @Override
  public void set(T value) {
    for (ReceivesValue<T> receiver : pre.forEach()){
      receiver.set(value);
    }
    for (ReceivesValue<T> receiver : handlers.forEach()){
      receiver.set(value);
    }
    for (ReceivesValue<T> receiver : post.forEach()){
      receiver.set(value);
    }
  };
  /**
   * Clear all callback stacks.
   */
  @Override
  public void clearReceivers() {
    super.clearReceivers();
    post.clear();
    pre.clear();
  }
  /**
   * Explicitly remove the given receiver from all callback stacks.
   */
  @Override
  public void removeReceiver(ReceivesValue<T> receiver) {
    super.removeReceiver(receiver);
    pre.remove(receiver);
    post.remove(receiver);
  }
}