package xapi.process.api;

import xapi.except.NotYetImplemented;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

public interface AsyncLock {

  /**
   * always throws {@link NotYetImplemented}.
   */
  AsyncCondition newCondition();

  /**
   * In a single-threaded environment, tryLock always returns true.
   *
   * In a threadsafe environment, only returns true if the current thread
   * owns the lock.
   *
   */
  boolean tryLock();

  RemovalHandler lock(SuccessHandler<AsyncLock> onLocked);

  /**
   * In a single-threaded environment, this will call any queued async lock
   * requests.
   */
  void unlock();


}
