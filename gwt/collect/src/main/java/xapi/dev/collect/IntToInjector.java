package xapi.dev.collect;

import java.io.PrintWriter;
import java.util.Queue;

import javax.inject.Provider;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.gwt.collect.IntToListGwt;
import xapi.source.X_Source;

public class IntToInjector implements MagicMethodGenerator, UnifyAstListener {

  private JMethod createMethod;
  private JDeclaredType intToListType;
  private JMethod createForClassMethod;

  @Override
  public void destroy(final TreeLogger arg0) {
    createMethod = null;
    createForClassMethod = null;
    intToListType = null;
  }

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall methodCall,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
          throws UnableToCompleteException {
    index(ast);

    final SourceInfo info = methodCall.getSourceInfo().makeChild();
    final JExpression classArg = methodCall.getArgs().get(0);
    final JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(
        logger, classArg, ast, false);
    if (classLit == null) {
      // If the code did not send a class literal, then we should defer to
      // making a runtime call that relies on runtime array reflection.

      // TODO TRACE a warning for this
      if (logger.isLoggable(Type.TRACE)) {
        logger.log(Type.TRACE, "Encountered a non-class literal instantiation "
            + "of an IntTo class @ "+enclosingMethod.getSignature());
      }
      return new JMethodCall(info, null, createForClassMethod, classArg);
    }
    final String litType = classLit.getRefType().getName();



    final String
    binaryName = classLit.getRefType().getName(),
    typeName = binaryName.replace('$', '_');
    final String providerName = typeName+"_ArrayProvider";
    JDeclaredType providerType;
    try {
      try {
        providerType = ast.searchForTypeBySource(providerName);
      } catch (final NoClassDefFoundError e) {
        throw new UnableToCompleteException();
      }
      if (providerType == null) {
        throw new UnableToCompleteException();
      }
    } catch (final UnableToCompleteException e) {

      final String[] names = X_Source.splitClassName(binaryName);
      if ("java".equals(names[0])) {
        names[0] = "javax";
      }

      final String componentType = names[1].replace('$', '.');
      names[1] = names[1].replace('$', '_');

      final SourceBuilder<Object> builder = new SourceBuilder<>("public final class "+names[1]+"_ArrayProvider");

      final String simpleType = builder.getImports()
          .addImports(Provider.class)
          .addImport(X_Source.qualifiedName(names[0], componentType));

      builder.setPackage(names[0]);

      final ClassBuffer out = builder.getClassBuffer().addInterface("Provider<"+simpleType+"[]>");
      out.createMethod("public final "+simpleType+"[] get()")
      .returnValue("new "+simpleType.split("<")[0] +"[0]");

      final StandardGeneratorContext gen = ast.getGeneratorContext();
      final PrintWriter pw = gen.tryCreate(logger, names[0], out.getSimpleName());
      pw.print(builder.toString());
      gen.commit(logger, pw);
      gen.finish(logger);

      providerType = ast.searchForTypeBySource(builder.getQualifiedName());
      providerType = ast.translate(providerType);
    }
    for (final JMethod ctor : providerType.getMethods()) {
      if (ctor instanceof JConstructor) {
        final JMethodCall create = new JMethodCall(info, null, createMethod,
            new JNewInstance(info, (JConstructor)ctor));
        ast.translate(providerType);
        return create;
      }
    }

    logger.log(Type.ERROR, "Unable to complete generation of IntTo<"+litType+">");
    throw new UnableToCompleteException();
  }

  @Override
  public boolean onUnifyAstPostProcess(final TreeLogger arg0, final UnifyAstView arg1,
      final UnifyVisitor arg2, final Queue<JMethod> arg3) {
    return false;
  }

  @Override
  public void onUnifyAstStart(final TreeLogger arg0, final UnifyAstView arg1,
      final UnifyVisitor arg2, final Queue<JMethod> arg3) {

  }

  private JDeclaredType index(final UnifyAstView ast) {
    if (intToListType != null) {
      return intToListType;
    }
    ast.getProgram().addIndexedTypeName(IntToListGwt.class.getName());
    intToListType = ast.searchForTypeBySource(IntToListGwt.class.getName());

    for (final JMethod method : intToListType.getMethods()) {
      if (method.getName().equals("create")) {
        createMethod = ast.translate(method);
        if (createForClassMethod != null) {
          break;
        }
      }
      else if (method.getName().equals("createForClass")) {
        createForClassMethod = ast.translate(method);
        if (createMethod != null) {
          break;
        }
      }
    }
    return intToListType;
  }

}
