package xapi.process.impl;

import xapi.process.X_Process;
import xapi.process.api.AsyncLock;
import xapi.util.api.SuccessHandler;

import java.lang.reflect.Array;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class ProcessQueueAbstract<T> extends AbstractQueue<T> {

  private final int size;
  private final T[] all;
  private final AsyncLock lock;
  private int writeIndex;
  private int readIndex;

  public ProcessQueueAbstract(int knownSize) {
    this.size = knownSize;
    Class<T> cls = typeClass();
    all = (T[]) Array.newInstance(cls, knownSize);
    lock = X_Process.newLock();
  }

  /**
   * @return The class of components to be used in the process queue.
   * <p>
   * If you wish to have Gwt support, be sure that this class has been enhanced
   * with array reflection support.  Simply calling static { Array.newInstance(MyClass.class, 0); }
   * using a class literal will then allow later calls using a class reference to succeed.
   */
  protected abstract Class<T> typeClass();

  @Override
  public boolean offer(final T e) {
    if (lock.tryLock()) {
      // just do it
      try {
        doPut(e);
      } finally {
        lock.unlock();
      }
    } else {
      lock.lock(
          new SuccessHandler<AsyncLock>() {
            @Override
            public void onSuccess(AsyncLock t) {
              try {
                doPut(e);
              } finally {
                t.unlock();
              }
            }
          }
      );
    }
    return true;
  }

  private void doPut(T e) {
    if (writeIndex < size) {
      all[writeIndex++] = e;
    } else {

    }
  }

  void unlock() {
    lock.unlock();
  }

  @Override
  public T poll() {
    if (readIndex == size)
      throw new NoSuchElementException();
    return all[readIndex++];
  }

  @Override
  public T peek() {
    return all[readIndex];
  }

  @Override
  public Iterator<T> iterator() {
    return null;
  }

  @Override
  public int size() {
    return writeIndex - readIndex;
  }

}