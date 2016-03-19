package xapi.javac.dev.search;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Name;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.model.InjectionBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Searches for calls to GWT.create within a compilation unit
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class InjectionTargetSearchVisitor extends TreePathScanner<List<InjectionBinding>, List<InjectionBinding>>{

  private final Trees trees;
  private final JavacService service;
  private final List<InjectionBinding> targets;
  private Predicate<VariableTree> injectNulls;

  public InjectionTargetSearchVisitor(JavacService service, Trees trees) {
    this.service = service;
    this.trees = trees;
    injectNulls = v->false;
    targets = new ArrayList<>();
  }

  @Override
  public List<InjectionBinding> visitVariable(
      VariableTree node, List<InjectionBinding> injectionBindings
  ) {
    if (hasInjectAnnotation(node.getModifiers()) ) {
      // We've found an injection target!  Lets store this as a potential binding
      targets.add(service.createInjectionBinding(node));
    } else if (isInitializedToNull(node) && injectNulls.test(node)) {
      targets.add(service.createInjectionBinding(node));
    }
    return super.visitVariable(node, injectionBindings);
  }

  private boolean isInitializedToNull(VariableTree node) {
    return node.getInitializer() != null && node.getInitializer().getKind() == Kind.NULL_LITERAL;
  }

  private boolean isInjectionAnnotation(AnnotationTree anno) {
    final Tree type = anno.getAnnotationType();
    final Name name = TreeInfo.name((JCTree) type);
    return name.contentEquals("Inject"); // Any annotation named inject is suitable
  }

  @Override
  public List<InjectionBinding> visitLabeledStatement(
      LabeledStatementTree node, List<InjectionBinding> injectionBindings
  ) {
    if (node.getLabel().contentEquals("inject")) {
      // using an inject label, like so:
      inject : {
        // will cause all variables initialized to null to become injected.
        final Predicate<VariableTree> oldPredicate = injectNulls;
        try {
          injectNulls = v->true;
          return super.visitLabeledStatement(node, injectionBindings);
        } finally {
          injectNulls = oldPredicate;
        }
      }
    }
    return super.visitLabeledStatement(node, injectionBindings);
  }

  @Override
  public List<InjectionBinding> visitMethod(
      MethodTree node, List<InjectionBinding> injectionBindings
  ) {
    if (hasInjectAnnotation(node.getModifiers())) {
      injectionBindings.add(service.createInjectionBinding(node));
    }
    return super.visitMethod(node, injectionBindings);
  }

  private boolean hasInjectAnnotation(ModifiersTree modifiers) {
    return modifiers.getAnnotations()
        .stream()
        .anyMatch(this::isInjectionAnnotation);
  }

  @Override
  public List<InjectionBinding> visitMethodInvocation(MethodInvocationTree node, List<InjectionBinding> list) {
    // Is this a method invocation to a magic method?
    // For now, lets just handle X_Inject.
    if (isXInjectMethodCall(node)) {

    }
    return super.visitMethodInvocation(node, list);
  }

  private boolean isXInjectMethodCall(MethodInvocationTree node) {
    final ExpressionTree expr = node.getMethodSelect();
    if (expr instanceof JCIdent) {
      final String exprName = TreeInfo.fullName((JCIdent)expr).toString();
      if (exprName.startsWith(X_Inject.class.getCanonicalName())) {
        // for now, we are just going to handle X_Inject.instance, X_Inject.singleton and X_Inject.initialize
        final String methodName = exprName.substring(X_Inject.class.getCanonicalName().length() + 1, exprName.length());
        if (methodName.equals("inject")) {
          // we want to inject an instance
        } else if (methodName.equals("singleton")) {
          // we want to inject a singleton
        } else if (methodName.equals("initialize")) {
          // we want to perform field injection on an arbitrary object (fun!)
        }
      }
    }
    return false;
  }
}
