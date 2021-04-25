package com.github.javaparser.ast.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BaseParameter;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EmptyMemberDeclaration;
import com.github.javaparser.ast.body.EmptyTypeDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.MultiTypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.TypeDeclarationStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import xapi.fu.Out1;

import static xapi.string.X_String.join;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * For cases when you want to collect a set of modifications,
 * then apply them all in a single traversal of the node graph,
 * this class allows you to supply an IdentityHashMap of nodes
 * in the graph with new nodes you want to insert.
 *
 * This visitor overrides all the methods to check this map for
 * supplied replacements.  This is simpler in many cases than
 * creating highly specific adapters, especially when you want
 * to expose a generic API for arbitrary replacements that may
 * not actually be aware of what nodes are to be replaced.
 *
 * Created by James X. Nelson (james@wetheinter.net) on 6/22/16.
 */
public class ConcreteModifierVisitor extends ModifierVisitorAdapter <Map<Node, Node>> {

  public static void replaceResolved(Map<Node, Out1<Node>> replacements) {
    Map<Node, Node> copy = new IdentityHashMap<>();
    replacements.forEach((k, v)->copy.put(k, v.out1()));
    replaceAll(copy);
  }

  public static void replaceAll(Map<Node, Node> replacements) {
    final Node ancestor = findAncestor(replacements.keySet());
    // traverse only from the common ancestor down.
    // A single pass through from one ancestor will complete all needed work,
    // and only visit each node once, to avoid idempotency errors.
    if (ancestor != null) {
      ancestor.accept(new ConcreteModifierVisitor(), replacements);
    }
  }

  private ConcreteModifierVisitor() {
  }

  /**
     find the lowest common ancestor of all nodes;
     for very large graphs, this will be faster than
     traversing a potentially very large number of uninteresting nodes,
     up to N-?, where ? is < N, but can be almost N,

     Given that we do map lookups on every node,
     we prefer reducing the search size of large graphs,
     which can take a big hit if we process N-? total nodes in a graph,
     up to N-? times { ~N * ~N ~= O(nlogn)}.

     By choosing the nearest-to-root ancestor,
     we may wind up processing N-? nodes more than we have to,
     but only do so once (thereby

   */
  private static Node findAncestor(Set<Node> nodes) {
    if (nodes.isEmpty()) {
      return null;
    }
    final Iterator<Node> itr = nodes.iterator();
    final Node first = itr.next();
    if (nodes.size() == 1) {
      return first.getParentNode();
    }
    // with more than one item, we need to paint a map of candidates.
    // matches will store a chain of parents of the first node;
    // an identity hashmap is used for fast querying of whether the item was seen or not
    IdentityHashMap<Node, Boolean> matches = new IdentityHashMap<>();
    // the chain of parents is stored in this linked list,
    // which we use for fast addition/removal at either end of the list.
    LinkedList<Node> possible = new LinkedList<>();
    // traverse up from the first node, marking all parents as candidates
    Node node, candidate = node = first;
    while (node.getParentNode() != null) {
      candidate = node;
      node = node.getParentNode();
      matches.put(node, false);
      possible.addFirst(node);
    }
    // Now, all ancestors of the first node are candidates,
    // and the candidate pointer is pointing at the root of the graph

    // lets check all remaining nodes for their intersection points.
    loop:
    while (itr.hasNext()) {
      node = itr.next();
      while (node.getParentNode() != null) {
        Boolean seen = matches.get(node = node.getParentNode());
        if (seen != null) {
          // if we've seen this parent before, lets determine if this is a new best ancestor
          if (seen) {
            // if the state is anything other than false, we have already resolved this node.
            continue loop;
          }
          // if we encountered an unseen ancestor, then it must be the next best
          matches.put(candidate = node, true);
          // now remove all dangling descendants who are no longer ancestor candidates
          while (possible.getLast() != node) {
            // mark children of the best candidate so we don't consider them again
            matches.put(possible.removeLast(), true);
          }
          continue loop;
        }
      }
      assert false : "Malformed graph structure; at least two nodes do not share a common ancestor in:\n" + join("\n", nodes);
    }
    // The last item in the possible list is the common ancestor
    assert possible.isEmpty() || candidate == possible.removeLast() : "Malformed graph structure; best candidate did not match tail of possible list";
    return candidate;
  }

  @Override
  protected Node visit(
      BaseParameter n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      AnnotationDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      AnnotationMemberDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ArrayAccessExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ArrayCreationExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ArrayInitializerExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      AssertStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      AssignExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      BinaryExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      BlockStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      BooleanLiteralExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      BreakStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      CastExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      CatchClause n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      CharLiteralExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ClassExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ClassOrInterfaceDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ClassOrInterfaceType n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      CompilationUnit n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ConditionalExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ConstructorDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ContinueStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      DoStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      DoubleLiteralExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      EmptyMemberDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      EmptyStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      EmptyTypeDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      EnclosedExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      EnumConstantDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      EnumDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ExplicitConstructorInvocationStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ExpressionStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      FieldAccessExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      FieldDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ForeachStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ForStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      IfStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ImportDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      InitializerDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      InstanceOfExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      IntegerLiteralExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      IntegerLiteralMinValueExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      JavadocComment n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      LabeledStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      LongLiteralExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      LongLiteralMinValueExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      MarkerAnnotationExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      MemberValuePair n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      MethodCallExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      MethodDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      NameExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      NormalAnnotationExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      NullLiteralExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ObjectCreationExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      PackageDeclaration n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      Parameter n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      MultiTypeParameter n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      PrimitiveType n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      QualifiedNameExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ReferenceType n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ReturnStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      SingleMemberAnnotationExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      StringLiteralExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      TemplateLiteralExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      SuperExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      SwitchEntryStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      SwitchStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      SynchronizedStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ThisExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      ThrowStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      TryStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      TypeDeclarationStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      TypeParameter n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      UnaryExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      UnknownType n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      VariableDeclarationExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      VariableDeclarator n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      VariableDeclaratorId n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      VoidType n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      WhileStmt n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      WildcardType n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      LambdaExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      MethodReferenceExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      TypeExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      BlockComment n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      LineComment n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      IntersectionType n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      UnionType n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      UiBodyExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      UiAttrExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      UiContainerExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      DynamicDeclarationExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(
      JsonContainerExpr n, Map<Node, Node> arg
  ) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(JsonPairExpr n, Map<Node, Node> arg) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(CssBlockExpr n, Map<Node, Node> arg) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(CssContainerExpr n, Map<Node, Node> arg) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(CssRuleExpr n, Map<Node, Node> arg) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(CssSelectorExpr n, Map<Node, Node> arg) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(CssValueExpr n, Map<Node, Node> arg) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }

  @Override
  public Node visit(SysExpr n, Map<Node, Node> arg) {
    if (arg.containsKey(n)) {
      return arg.get(n);
    }
    return super.visit(n, arg);
  }
}
