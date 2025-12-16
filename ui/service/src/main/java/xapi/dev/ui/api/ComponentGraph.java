package xapi.dev.ui.api;

import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.expr.UiExpr;
import xapi.fu.Rethrowable;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class ComponentGraph implements Rethrowable {

  // we don't use synchronized, so just make the writable field volatile
  private volatile ComponentGraph parent;
  private volatile ComponentGraph firstChild;
  private volatile ComponentGraph nextSibling;
  private volatile ComponentGraph lastChild;

  private UiExpr self;
  private UiAttrExpr parentAttr;
  private UiContainerExpr parentContainer;
  // binary semaphore; only one thread may appendChild at once on a given node,
  // and a single thread may not call .appendChild on a node while .appendChild
  // has already been called.
  private final Semaphore lock = new Semaphore(1);

  protected ComponentGraph() {
  }

  public ComponentGraph(UiExpr self) {
    this.self = self;
  }

  public ComponentGraph appendChild(UiExpr into) {
    try {
      if (!lock.tryAcquire(5, TimeUnit.MICROSECONDS)) {
        rethrow(new TimeoutException("Waiting 5 seconds to descend into NodeScope; " +
            "either you are doing way too much work in .createChild(), or you have " +
            "tried to call .descend() on a node that was already descending()."));
      }
    } catch (InterruptedException e) {
      // stop running if out thread has been cancelled...
      Thread.currentThread().interrupt();
      throw rethrow(e);
    }
    try {
      // Creates a child with values copies from this class
      ComponentGraph child = createChild(into);

      // Update our child pointers accordingly
      if (firstChild == null) {
        firstChild = lastChild = child;
      } else {
        lastChild.nextSibling = child;
        lastChild = child;
      }
      // done
      return child;
    } finally {
      lock.release();
    }
  }

  protected ComponentGraph createChild(UiExpr into) {
    final ComponentGraph child = new ComponentGraph();
    initializeChild(child, into);
    return child;
  }

  protected void initializeChild(ComponentGraph child, UiExpr into) {
    child.self = into;
    child.parent = this;
    child.parentAttr = parentAttr;
    child.parentContainer = parentContainer;
    if (self instanceof UiAttrExpr) {
      child.parentAttr = (UiAttrExpr) self;
    } else if (self instanceof UiContainerExpr) {
      child.parentContainer = (UiContainerExpr) self;
    }
  }

  public ComponentGraph getParent() {
    return parent;
  }

  public ComponentGraph getFirstChild() {
    return firstChild;
  }

  public ComponentGraph getNextSibling() {
    return nextSibling;
  }

  public ComponentGraph getLastChild() {
    return lastChild;
  }

  public UiExpr getSelf() {
    return self;
  }

  public UiAttrExpr getParentAttr() {
    return parentAttr;
  }

  public UiContainerExpr getParentContainer() {
    return parentContainer;
  }

  public UiContainerExpr getDeepestContainer() {
    return self instanceof UiContainerExpr ? (UiContainerExpr) self : parentContainer;
  }

  /**
   * In order to determine if a node should be considered the child of
   * an attribute, that child must have the same container as the parent attribute.
   *
   * In most parsed xapi templates, this will return true until a container
   * child is encountered, and will be false for children in that container.
   */
  public boolean isAttributeChild() {
    return parentAttr != null && parentAttr.getParentNode() == parentContainer;
  }
}
