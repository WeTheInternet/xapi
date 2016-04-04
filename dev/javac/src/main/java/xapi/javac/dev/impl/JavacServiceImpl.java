package xapi.javac.dev.impl;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringDictionary;
import xapi.fu.In2;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.model.InjectionBinding;
import xapi.javac.dev.model.InjectionMap;
import xapi.javac.dev.model.XApiInjectionConfiguration;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsNamedType;
import xapi.source.read.JavaModel.IsType;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileManager;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

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
  private StringDictionary<String> props;
  private final ClassTo cache;
  private JavaFileManager filer;

  public JavacServiceImpl() {
    cache = X_Collect.newClassMap();
    injections = new InjectionMap();
    remember(InjectionMap.class, injections);
    props = X_Collect.newDictionary();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, C extends Class<T>> T recall(C cls) {
    return (T) cache.get(cls);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, C extends Class<T>> T remember(C cls, T value) {
    return (T) cache.put(cls, value);
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
  public String getFileName(CompilationUnitTree cu) {
    return getClassTree(cu).getSimpleName().toString();
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
    String name = new File(cu.getSourceFile().getName()).getName().replace(".java", "");
    if (cu instanceof JCCompilationUnit) {
      JCCompilationUnit unit = (JCCompilationUnit) cu;
      return unit.getTypeDecls().stream()
          .filter(decl->decl instanceof ClassTree)
          .map(decl->(ClassTree)decl)
          .filter(decl->decl.getSimpleName().contentEquals(name))
          .findFirst()
          .get();
    }
    return null;
  }

  @Override
  public String getQualifiedName(CompilationUnitTree cup, ClassTree classTree) {
    String pkg = getPackageName(cup);
    // TODO consider enclosing elements correctly...
    return X_Source.qualifiedName(pkg, classTree.getSimpleName().toString());
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
    filer = context.get(JavaFileManager.class);
    if (filer == null) {
      filer = new JavacFileManager(context, true, Charset.forName("UTF-8"));
    }
    remember(JavaFileManager.class, filer);
    remember(Types.class, types);
    remember(Elements.class, elements);
    remember(Trees.class, trees);
    remember(JavaCompiler.class, compiler);

    try {
      final Enumeration<URL> propFiles = Thread.currentThread().getContextClassLoader().getResources(
          "META-INF/xapi.properties");
      while (propFiles.hasMoreElements()) {
        final URL location = propFiles.nextElement();
        final Properties properties = new Properties();
        properties.load(location.openStream());
        if (properties.isEmpty()) {
          continue;
        }
        final Set<String> names = properties.stringPropertyNames();
        for (String name : names) {
          final String value = (String) properties.get(name);
          final Object result = props.setValue(name, value);
          if (result != null) {
            if (!result.equals(value)) {
              X_Log.warn(getClass(), "Overwriting property ", name, " was ", result, " set to: ", value);
            }
          }

        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public InjectionBinding createInjectionBinding(VariableTree node) {
    return new InjectionBinding(this, node);
  }

  @Override
  public InjectionBinding createInjectionBinding(MethodTree node) {
    return new InjectionBinding(this, node);
  }

  @Override
  public IsType getInvocationTargetType(CompilationUnitTree cup, MethodInvocationTree node) {
    if (node instanceof JCMethodInvocation) {
      final TreePath path = trees.getPath(cup, node);
      final TreePath parent = path.getParentPath();
      final Tree target = parent.getLeaf();
      switch (target.getKind()) {
        case VARIABLE:
          JCVariableDecl var = (JCVariableDecl) target;
          final JCTree type = var.getType();
          final JavacScope scope = trees.getScope(path);
          final IsType cls = getTypeOf(cup, scope, type);
          return cls;
        default:
          X_Log.error(getClass(), "Unhandled invocation target type: ", target.getKind(), target);
      }
    } else {
      X_Log.warn(getClass(), "Does not support MethodInvocationTree ", node);
    }
    return null;
  }

  private IsType getTypeOf(CompilationUnitTree cup, JavacScope scope, JCTree type) {
    switch (type.getKind()) {
      case IDENTIFIER:
        JCIdent ident = (JCIdent) type;
        for (ImportTree importTree : cup.getImports()) {
          if (importTree.isStatic()) {
            continue; // TODO: handle differently... (write a test!)
          }
          final Tree id = importTree.getQualifiedIdentifier();
          final String name = TreeInfo.fullName((JCTree) id).toString();
          if (name.endsWith(ident.getName().toString())) {
            // we have a winner!
            return new IsType(name.toString());
          }
          if ("*".equals(name)) {

          }

        }
        final String pkg = getPackageName(cup);
        return new IsType(pkg, ident.getName().toString());
    }
    return null;
  }

  @Override
  public IsNamedType getName(CompilationUnitTree cup, MethodInvocationTree node) {
    if (node instanceof JCMethodInvocation) {
      final String simpleName = TreeInfo.name(((JCMethodInvocation) node).meth).toString();
      String fullName = TreeInfo.fullName(((JCMethodInvocation) node).meth).toString();
      String className = fullName.indexOf('.') == -1 ? fullName : fullName.replaceFirst("[.](?:[^.]+)$", "");
      if (fullName.equals(className)) {
        // This is a local method reference.  Find the enclosing class type
        ClassTree cls = getEnclosingClass(cup, node);
        className = getNameOf(cls);
        return IsNamedType.namedType(className, simpleName);
      }
      final TreePath path = trees.getPath(cup, node);
      final JavacScope scope = trees.getScope(path);
      if (scope.isStarImportScope()) {
        // need to do lookup on the simple name of the method
      } else {
        // need to do lookup on the class of the imported method
      }
      final Optional<? extends ImportTree> match = cup.getImports().stream()
          .filter(importName -> {
            final Name n = TreeInfo.name((JCTree) importName.getQualifiedIdentifier());
            return n.contentEquals(simpleName);
          })
          .findFirst();
      return IsNamedType.namedType(className, simpleName);
    } else {
      X_Log.warn(getClass(), "Does not support MethodInvocationTree ", node);
    }
    return null;
  }

  public String getNameOf(ClassTree cls) {
    if (cls instanceof JCClassDecl) {
      return ((JCClassDecl)cls).sym.getQualifiedName().toString();
    }
    throw new NoSuchElementException("Cannot find name of " + cls);
  }

  @Override
  public ClassTree getEnclosingClass(CompilationUnitTree cup, Tree node) {
    TreePath path = trees.getPath(cup, node);
    while (!(path.getLeaf() instanceof ClassTree) && path.getParentPath() != null) {
      path = path.getParentPath();
    }
    if (path.getLeaf() instanceof ClassTree) {
      return (ClassTree) path.getLeaf();
    }
    throw new NoSuchElementException("Cannot find a parent class of " + node);
  }

  @Override
  public void readProperties(In2<String, String> in) {
    props.forEach(in);
  }
}
