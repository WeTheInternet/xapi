package com.google.gwt.dev.jjs;

import java.util.Queue;

import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.impl.UnifyAst;

/**
 * A listener interface to be notified
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface UnifyAstListener {

  /**
   * Called once at the very beginning of an application's program.
   *
   * This is handy if you would like to inject bootstrap code into a module,
   * without having to override that module itself.
   *
   * Base library code should never add work to the queue, and only use
   * this method to perform any initialization that may be required.
   *
   * @param unifyAst - The ast being iterated
   * @param todo - The queue of methods to be added to the program.
   */
  void onUnifyAstStart(UnifyAstView ast, Queue<JMethod> todo);
  /**
   * Called at the end of every iteration of {@link UnifyAst#mainLoop()}.
   *
   * This is handy for generators that want to store up some state during
   * an iteration of all newly generated types, but wait until after
   * the queue is drained to add more method calls, perhaps behind
   * a code split, to move any code bloat for non-vital features behind
   * some deferred mechanism.
   *
   * @param unifyAst - The ast being iterated
   * @param todo - The queue of methods to be added to the program.
   *
   * You should NOT add to this queue unless you have new work to do;
   * the queue being empty after all listeners are notified is what signals
   * the end of the unify ast loop.
   *
   */
  void onUnifyAstFinished(UnifyAstView ast, Queue<JMethod> todo);

  /**
   * Called once after the main ast loop is finished;
   * make sure you clear any ThreadLocal or static resources here!
   *
   */
  void destroy();

}
