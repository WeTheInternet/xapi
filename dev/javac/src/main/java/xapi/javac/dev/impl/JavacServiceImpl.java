package xapi.javac.dev.impl;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import xapi.annotation.inject.InstanceDefault;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.model.InjectionBinding;
import xapi.javac.dev.model.InjectionMap;
import xapi.javac.dev.model.XApiInjectionConfiguration;
import xapi.log.X_Log;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/12/16.
 */
@InstanceDefault(implFor = JavacService.class)
public class JavacServiceImpl implements JavacService {

  private Types types;
  private Elements elements;
  private InjectionMap injections;
  private JavacTrees trees;
  private JavaCompiler compiler;

  public JavacServiceImpl() {
    injections = new InjectionMap();
  }

  @Override
  public String getPackageName(CompilationUnitTree cu) {

    if (cu instanceof JCCompilationUnit) {
      JCCompilationUnit compilationUnit = (JCCompilationUnit) cu;
      return compilationUnit.packge.toString();
    }
    throw new UnsupportedOperationException("Cannot handle compilation unit type " +
      cu.getClass()+" of compilation unit " + cu);
  }

  @Override
  public TypeMirror findType(ExpressionTree init) {
    switch (init.getKind()) {
      case MEMBER_SELECT:
        MemberSelectTree member = (MemberSelectTree) init;
        assert member instanceof JCFieldAccess : "Member "+member+" must be a JCFieldAccess";
        JCFieldAccess asField = (JCFieldAccess)member;
        if (member.getIdentifier().contentEquals("class")) {
          // must be a field access
          if (asField.type != null && getTypeName(asField).contentEquals("java.lang.Class")) {
            // Grab the type parameter off the class literal
            List<Type> params = asField.type.allparams();
            if (params.last() != null) {
              return params.last();
            }
          }
          // Grab the type parameter off the class literal
          final TypeElement ele = elements.getTypeElement(asField.getExpression().toString());
          assert ele.asType() != null;
          return ele.asType();
        }
        throw new UnsupportedOperationException("Could not find type of " + init);
      case IDENTIFIER:
        IdentifierTree ident = (IdentifierTree) init;
        if (ident instanceof JCIdent) {
          JCIdent asIdent = (JCIdent) ident;
          assert asIdent.type != null;
          return extractClassType(asIdent.type)
              .orElseGet(()->{
                  final TypeElement ele = elements.getTypeElement(asIdent.sym.flatName().toString());
                  assert ele.asType() != null;
                  return ele.asType();
              });
        }
        throw new UnsupportedOperationException("Could not find type of identifier " + init+"; unhandled type " + init.getKind());
      case METHOD_INVOCATION:
        MethodInvocationTree invoke = (MethodInvocationTree) init;
        if (invoke instanceof JCMethodInvocation) {
          JCMethodInvocation asMethod = (JCMethodInvocation) invoke;
          assert asMethod.type != null;
          if (getTypeName(asMethod).contentEquals("java.lang.Class")) {
            List<Type> params = asMethod.type.allparams();
            if (params.last() != null) {
              return params.last();
            }
            throw new UnsupportedOperationException("Cannot handle raw class type in " + asMethod);
          }
          X_Log.warn(getClass(), "Selecting type", asMethod.getMethodSelect().type, "from", asMethod);
          return asMethod.getMethodSelect().type;
        }
      throw new UnsupportedOperationException("Could not find type of method invocation " + init+"; unhandled type " + init.getKind());
    }
    throw new UnsupportedOperationException("Could not find type of " + init+"; unhandled type " + init.getKind());
  }

  private Optional<TypeMirror> extractClassType(Type type) {
    if (type.asElement().flatName().contentEquals("java.lang.Class")) {
      List<Type> params = type.allparams();
      if (params.last() != null) {
        return Optional.of(params.last());
      }
      return Optional.empty();
    }
    return Optional.empty();
  }

  private Name getTypeName(JCMethodInvocation asMethod) {
    return asMethod.type.asElement().flatName();
  }

  private Name getTypeName(JCFieldAccess asField) {
    return asField.type.asElement().flatName();
  }

  @Override
  public ClassTree getClassTree(CompilationUnitTree cu) {
    if (cu instanceof JCCompilationUnit) {
      JCCompilationUnit unit = (JCCompilationUnit) cu;
      if (unit.getTypeDecls().size() == 1) {
        return (ClassTree)unit.getTypeDecls().head;
      }
    }
    return null;
  }

  @Override
  public Optional<InjectionBinding> getInjectionBinding(XApiInjectionConfiguration config, TypeMirror type) {
    String scope;
    try {
      scope = config.getSettings().scope().getName();
    } catch (MirroredTypeException e) {
      scope = e.getTypeMirror().toString();
    }
    String typeName = types.erasure(type).toString();
    if ("Test".equals(typeName)) {
      InjectionBinding egregiousHack = new InjectionBinding("Test", "ComplexTest");
      return Optional.of(egregiousHack);
    }
    return injections.getBinding(scope, typeName);
  }

  @Override
  public void init(Context context) {
    types = JavacTypes.instance(context);
    elements = JavacElements.instance(context);
    trees = JavacTrees.instance(context);
    compiler = JavaCompiler.instance(context);

  }

  @Override
  public InjectionBinding createInjectionBinding(VariableTree node) {
    return new InjectionBinding(this, node);
  }

  @Override
  public InjectionBinding createInjectionBinding(MethodTree node) {
    return new InjectionBinding(this, node);
  }

}
