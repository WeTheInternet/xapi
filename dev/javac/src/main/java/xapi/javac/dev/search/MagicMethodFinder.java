package xapi.javac.dev.search;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Name;
import xapi.collect.api.IntTo;
import xapi.fu.Out2;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.api.MagicMethodInjector;
import xapi.javac.dev.api.MethodMatcher;
import xapi.source.read.JavaModel.IsNamedType;

import static xapi.collect.X_Collect.newList;

import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/2/16.
 */
public class MagicMethodFinder extends TreeScanner {

  private final Iterable<MethodMatcher<MagicMethodInjector>> matchers;
  private final JavacService service;
  private final CompilationUnitTree cup;
  private final IntTo<Out2<MethodInvocationTree, MagicMethodInjector>> matched;

  public MagicMethodFinder(Iterable<MethodMatcher<MagicMethodInjector>> matchers, JavacService service, CompilationUnitTree cup) {
    this.matchers = matchers;
    this.service = service;
    this.cup = cup;
    matched = newList(Out2.class);
  }

  public IntTo<Out2<MethodInvocationTree, MagicMethodInjector>> getMatched() {
    return matched;
  }

  @Override
  public void visitApply(JCMethodInvocation invocation) {
    final JCExpression method = invocation.getMethodSelect();
    final Name fullName = TreeInfo.fullName(method);
    final IsNamedType name = service.getName(cup, invocation);
    for (MethodMatcher<MagicMethodInjector> matcher : matchers) {
      final Optional<MagicMethodInjector> result = matcher.matches(name);
      if (result.isPresent()) {
        matched.add(Out2.out2(invocation, result.get()));
      }
    }
    super.visitApply(invocation);
  }

}
