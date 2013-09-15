package xapi.test.gwt.rebind;

import java.util.Queue;

import junit.framework.Assert;
import xapi.test.gwt.MagicMethodGwtTest;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;

/**
 * A reference implementation of a magic method generator.
 *
 * We use this generator to swap out the method {@link MagicMethodGwtTest#replaceMe()},
 * and have it return a different value if production mode.
 *
 * We also insert code at the beginning of our entry point,
 * to test that the {@link UnifyAstListener} api is being implemented correctly.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class MagicMethodTestGenerator implements MagicMethodGenerator, UnifyAstListener {

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod enclosingMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {
    JDeclaredType testClass = enclosingMethod.getEnclosingType();
    for (JField field : testClass.getFields()) {
      if (field.getName().equals("prodMode")) {
        return new JFieldRef(field.getSourceInfo(), methodCall.getInstance(), field, testClass);
      }
    }
    Assert.fail("Did not find field named prodMode in test class " + testClass);
    return null;
  }

  /**
   * Called before any ast is examined.
   *
   * Allows us to insert code before anything else is examined.
   */
  @Override
  public void onUnifyAstStart(UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo) {
    // Demonstrates how to insert code into your entry point, (before it, actually)
    // to execute before anything else in the app.

    // Grab our test class
    JDeclaredType type = ast.searchForTypeBySource(MagicMethodGwtTest.class.getName());
    // Find the method we want to call
    for (JMethod method : type.getMethods()) {
      if (method.getName().equals("callFromGenerator")) {
        // Find the entry point method
        for (JMethod entry : ast.getProgram().getEntryMethods()) {
          if (entry.getBody() instanceof JsniMethodBody) {
            // Skip the jsni $entry method, we _definitely_ don't want to add code here.
          } else {
            // Add to the java entry point method
            // (will be EntryMethodHolder.init(), a virtual method).
            SourceInfo child = entry.getBody().getSourceInfo().makeChild();
            // Create an expression calling the method we want to execute
            JExpressionStatement statement = new JMethodCall(child, null, method).makeStatement();
            // Attach expression to the entry point method.
            JMethodBody body = (JMethodBody)entry.getBody();
            body.getBlock().addStmt(0, statement);
          }
        }
      }
    }
  }

  /**
   * Called once the todo queue of methods to examine is drained.
   *
   * If any UnifyAstListener adds new methods to examine, all listeners will be called again.
   *
   * Blindly adding elements to todo will result in looping behavior.
   */
  @Override
  public boolean onUnifyAstPostProcess(UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo) {
    return false;
  }

  /**
   * Called once every UnifyAstListener has stopped adding JMethods to the todo queue.
   */
  @Override
  public void destroy() {
  }

}
