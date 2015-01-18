package xapi.dev.collect;

import java.io.PrintWriter;

import javax.inject.Provider;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.gwt.collect.IntToListGwt;
import xapi.source.X_Source;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;

public class IntToInjector implements MagicMethodGenerator {

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall,
      JMethod enclosingMethod, Context context, UnifyAstView ast)
      throws UnableToCompleteException {
    JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, methodCall.getArgs().get(0), ast, true);
    String litType = classLit.getRefType().getName();

    ast.getProgram().addIndexedTypeName(IntToListGwt.class.getName());
    JDeclaredType type = ast.searchForTypeBySource(IntToListGwt.class.getName());

    SourceInfo info = methodCall.getSourceInfo();

    for (JMethod method : type.getMethods()) {
      if (method.getName().equals("create")) {

        method = ast.translate(method);

        String
            binaryName = classLit.getRefType().getName(),
            typeName = binaryName.replace('$', '_');
        final String providerName = typeName+"_ArrayProvider";
        JDeclaredType providerType;
        try {
          try {
            providerType = ast.searchForTypeBySource(providerName);
          } catch (NoClassDefFoundError e) {
            throw new UnableToCompleteException();
          }
          if (providerType == null) {
            throw new UnableToCompleteException();
          }
        } catch (UnableToCompleteException e) {

          String[] names = X_Source.splitClassName(binaryName);
          if ("java".equals(names[0])) {
            names[0] = "javax";
          }

          String componentType = names[1].replace('$', '.');
          names[1] = names[1].replace('$', '_');

          SourceBuilder<Object> builder = new SourceBuilder<>("public final class "+names[1]+"_ArrayProvider");

          String simpleType = builder.getImports()
              .addImports(Provider.class)
              .addImport(X_Source.qualifiedName(names[0], componentType));

          builder.setPackage(names[0]);

          ClassBuffer out = builder.getClassBuffer().addInterface("Provider<"+simpleType+"[]>");
          out.createMethod("public final "+simpleType+"[] get()")
             .returnValue("new "+simpleType.split("<")[0] +"[0]");

          StandardGeneratorContext gen = ast.getGeneratorContext();
          PrintWriter pw = gen.tryCreate(logger, names[0], out.getSimpleName());
          pw.print(builder.toString());
          gen.commit(logger, pw);
          gen.finish(logger);

          providerType = ast.searchForTypeBySource(builder.getQualifiedName());
          providerType = ast.translate(providerType);
        }
        for (JMethod ctor : providerType.getMethods()) {
          if (ctor instanceof JConstructor) {
            JMethodCall create = new JMethodCall(info, null, method, new JNewInstance(info, (JConstructor)ctor));
            ast.translate(providerType);
            return create;
          }
        }
      }
    }

    logger.log(Type.ERROR, "Unable to complete generation of IntTo<"+litType+">");
    throw new UnableToCompleteException();
  }

}
