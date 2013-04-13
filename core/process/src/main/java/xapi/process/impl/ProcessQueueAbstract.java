package xapi.process.impl;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;

import xapi.process.X_Process;
import xapi.process.api.AsyncLock;
import xapi.reflect.X_Reflect;
import xapi.util.api.SuccessHandler;

public abstract class ProcessQueueAbstract <T> extends AbstractQueue<T> {

  private final int size;
  private int writeIndex;
  private int readIndex;
  private final T[] all;
  private final AsyncLock lock;

  public ProcessQueueAbstract(int knownSize) {
    this.size = knownSize;
    Class<T> cls = typeClass();
    all = X_Reflect.newArray(cls, knownSize);
    lock = X_Process.newLock();
  }

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
      lock.lock(new SuccessHandler<AsyncLock>() {
        @Override
        public void onSuccess(AsyncLock t) {
          try {
            doPut(e);
          } finally {
            t.unlock();
          }
        }
      });
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