package xapi.process.api;

import xapi.fu.lazy.ResettableLazy;

import java.io.Serializable;
import java.util.Iterator;

import static xapi.util.X_Util.equal;

/**
 * A state-blob for creating a resumable operation, most often sent to you by an
 * exception, or thrown by you via a RescheduleException.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 */
public class ProcessCursor <K extends Serializable> {

  private transient Iterable<K> stages;
  private final ResettableLazy<Iterator<K>> path;

  public ProcessCursor(Iterable<K> path) {
    this.stages = path;
    this.path = new ResettableLazy<>(stages::iterator);
  }

  K[] done;
  K[] todo;
  int position;

  int maxTries = 3;
  int tries = 1;

  public void bump() {
  }

  public boolean ifNext(K position) {
    if (equal(todo[this.position], position)) {
      bump();
      return true;
    }
    return false;
  }

  public void skip(K position) {
  }

  @Override
  protected void finalize() throws Throwable {
    stages = null;
    path.reset();
  }
}
