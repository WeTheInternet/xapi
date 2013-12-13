package com.google.gwt.dev.jjs;

import java.util.Queue;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.UnifyAst;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;

/**
 * <p>
 * A listener interface to be notified during the {@link UnifyAst} phase of a gwt compile.
 * <p>
 * Requires GWT 2.5+
 * <p>
 * Currently, the only way for a unify ast listener to be registered is if a class
 * registered as a {@link MagicMethodGenerator} also implements {@link UnifyAstListener}.
 * <p>
 * An instance of this listener will be created, and will have
 * {@link UnifyAstListener#onUnifyAstStart(UnifyAstView, UnifyVisitor, Queue)} called
 * exactly once at the start of the unify ast process.
 * <p>
 * Then, once the normal unify ast process is complete (entry point is visited),
 * the ast parser will then call {@link UnifyAstListener#onUnifyAstPostProcess(UnifyAstView, UnifyVisitor, Queue)}
 * repeatedly, until no UnifyAstListener return true.
 * <p>
 * Finally, {@link UnifyAstListener#destroy()} is called when all listeners are finished
 * their post-processing of the source code.
 * <p>
 * Note that the listener is responsible for attaching method calls to examine.
 * <p>
 * Arbitrary example:
 *<pre>
     JDeclaredType type = ast.searchForTypeBySource("your.fully.qualified.Name");
     for (JMethod method : type.getMethods()) {
      if (method.getName().equals("methodCallToHijack")) {
        JMethodBody body = (JMethodBody)entry.getBody();
        JExpressionStatement inject = getOrMakeStatement(ast);
        // insert our statement wherever
        body.getBlock().addStmt(0, inject);
      }
     }

     public JExpressionStatement getOrMakeStatement(UnifyAstView ast) {
       // Return a JMethodCall, JGwtCreate, or any expressionable statement.
       JDeclaredType type = ast.searchForTypeBySource("your.fully.qualified.HijackerName");
       for (JMethod method : type.getMethods()) {
         if (method.getName().equals("injectedMethod")) {
           return new JMethodCall(method.getSourceInfo(), null, method).makeStatement();
         }
       }
       throw new Error();
     }
     </pre>
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
   * See {@link xapi.dev.test.gwt.rebind.MagicMethodTestGenerator} for a reference implementation,
   * which inserts code before the main module entry point.
   * You may insert code anywhere; see {@link UnifyAstListener} for a reference implementation.
   *
   * @param ast - The ast context object, containing {@link JProgram}, {@link RebindPermutationOracle},
   * and the {@link UnifyAst#searchForTypeBySource(String)} / ByBinary() methods.
   * @param visitor
   *
   * @param todo - The queue of methods to be added to the program.
   */
  void onUnifyAstStart(TreeLogger logger, UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo);
  /**
   * Called at the end of every iteration of {@link UnifyAst#mainLoop()}.
   *
   * This is handy for generators that want to store up some state during
   * an iteration of all newly generated types, but wait until after
   * the queue is drained to add more method calls, perhaps behind
   * a code split, to move any code bloat for non-vital features behind
   * some deferred mechanism.
   *
   * The caller IS responsible for attaching new executions to the ast node graph.
   * See {@link UnifyAstListener} for details.
   *
   * @param unifyAst - The ast being iterated.
   * @param visitor - The visitor who adds ast nodes to program.  Use now to load immediated.
   * @param todo - The queue of methods to be added to the program. Used to defer the parsing of a method call.
   * These JMethods can be newly generated, or found from the JProgram (in {@link UnifyAstView#getProgram()}.
   * @return true if the listener requires (more) post processing.
   * return false to break the loop (do not always return true).
   *
   *
   */
  boolean onUnifyAstPostProcess(TreeLogger logger, UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo);

  /**
   * Called once after the main ast loop is finished;
   * make sure you clear any ThreadLocal or static resources here!
   *
   */
  void destroy(TreeLogger logger);

}
