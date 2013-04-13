package java.util.concurrent;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

public class ConcurrentLinkedQueue<T> extends LinkedList<T> implements Queue<T>, Serializable{

  private static final long serialVersionUID = 6442115036147799728L;

  //exposed for serialization compatibility
  @SuppressWarnings("unused")
  private T makeSerializable;
}
