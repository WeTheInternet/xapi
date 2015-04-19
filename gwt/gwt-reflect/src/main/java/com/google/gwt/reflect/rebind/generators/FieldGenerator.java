package com.google.gwt.reflect.rebind.generators;

import static java.lang.reflect.Modifier.PRIVATE;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilType;
import com.google.gwt.thirdparty.xapi.dev.source.ClassBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.MethodBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.SourceBuilder;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsQualified;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.List;

public abstract class FieldGenerator extends MemberGenerator implements MagicMethodGenerator {

  protected abstract boolean isDeclared();

  protected JMethodCall getFactoryMethod(final TreeLogger logger, final JMethodCall callSite,
      final JMethod enclosingMethod, final Context context, final JClassLiteral classLit, final JExpression inst, final JExpression arg0, final UnifyAstView ast)
          throws UnableToCompleteException {

    if (classLit == null) {
      if (logger.isLoggable(logLevel())) {
        logger.log(logLevel(),
            "Non-final class literal used to invoke reflection field; "
                + ReflectionUtilAst.debug(callSite.getInstance()));
      }
      return checkConstPool(ast, callSite, inst, arg0);
    }

    final JStringLiteral stringLit = ReflectionUtilAst.extractImmutableNode(logger, JStringLiteral.class, arg0, ast, false);
    if (stringLit == null) {
      if (logger.isLoggable(logLevel())) {
        logger.log(logLevel(),
            "Non-final string arg used to retrieve reflection field; "
                + ReflectionUtilAst.debug(arg0));
      }
      return checkConstPool(ast, callSite, inst, arg0);
    }
    final String name = stringLit.getValue();

    // We got all our literals; the class, method name and parameter classes
    // now get the requested method
    final ReflectionGeneratorContext ctx = new ReflectionGeneratorContext(logger, classLit, callSite, enclosingMethod, context, ast);

    final JType ref = classLit.getRefType();
    final JClassType oracleType = ast.getTypeOracle().findType(ref.getName().replace('$', '.'));
    final com.google.gwt.core.ext.typeinfo.JField field =
        ReflectionUtilType.findField(logger, oracleType, name, isDeclared());

    if (field == null) {
      // We fail here because the requested method is not findable.
      if (shouldFailIfMissing(logger, ast)) {
        logger.log(Type.ERROR, "Unable to find field " + oracleType.getQualifiedSourceName()+"."+name+ ";");
        logger.log(Type.ERROR, "Did you forget to call StandardGeneratorContext.finish()?");
        throw new UnableToCompleteException();
      } else {
        return checkConstPool(ast, callSite, inst, arg0);
      }
    }
    if (logger.isLoggable(logLevel())) {
      logger.log(logLevel(), "Found injectable field " + field);
    }

    // now, get or make a handle to the requested method,
    return getFieldProvider(logger, ctx, field, classLit, isDeclared());
  }

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall callSite,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {

    final boolean isFromGwtReflect = callSite.getArgs().size() == 2;
    final JExpression inst = isFromGwtReflect ? callSite.getArgs().get(0) : callSite.getInstance();
    final JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, inst, ast, false);
    final List<JExpression> args = callSite.getArgs();
    final JExpression arg0 = args.get(isFromGwtReflect?1:0);

    // and return a call to the generated Method provider
    return
        getFactoryMethod(logger, callSite, enclosingMethod, context, classLit, inst, arg0, ast)
        .makeStatement().getExpr();
  }

  public JMethodCall getFieldProvider(final TreeLogger logger, final ReflectionGeneratorContext ctx, final com.google.gwt.core.ext.typeinfo.JField field,
      final JClassLiteral classLit, final boolean declaredOnly) throws UnableToCompleteException {
    final String clsName = classLit.getRefType().getName();
    final ReflectionManifest manifest = ReflectionManifest.getReflectionManifest(logger, clsName, ctx.getGeneratorContext());
    final String factoryCls = getOrMakeFieldFactory(logger, ctx, field, field.getEnclosingType(), manifest, declaredOnly);
    ctx.finish(logger);
    final JDeclaredType factory = ctx.getAst().searchForTypeBySource(factoryCls);
    // pull out the static accessor method
    for (final JMethod factoryMethod : factory.getMethods()) {
      if (factoryMethod.isStatic() && factoryMethod.getName().equals("instantiate")) {
        return new JMethodCall(factoryMethod.getSourceInfo(), null, factoryMethod);
      }
    }
    logger.log(Type.ERROR, "Unable to find static initializer for Field subclass "+factoryCls);
    throw new UnableToCompleteException();
  }

  public String getOrMakeFieldFactory(final TreeLogger logger, final ReflectionGeneratorContext ctx, final com.google.gwt.core.ext.typeinfo.JField field,
      final com.google.gwt.core.ext.typeinfo.JType classType, final ReflectionManifest manifest, final boolean declaredOnly) throws UnableToCompleteException {
    // get cached manifest for this type
    final String clsName = classType.getQualifiedSourceName();
    final TypeOracle oracle = ctx.getTypeOracle();
    final String name = field.getName();
    final JClassType cls = oracle.findType(clsName);
    if (cls == null) {
      logger.log(Type.ERROR, "Unable to find enclosing class "+clsName);
      throw new UnableToCompleteException();
    }

    final String fieldFactoryName = FieldGenerator.getFieldFactoryName(cls, name);
    JClassType factory;
    final String pkgName = field.getEnclosingType().getPackage().getName();
    factory = oracle.findType(pkgName, fieldFactoryName);
    if (factory == null) {
      return generateFieldFactory(logger, ctx, field, fieldFactoryName, manifest);
    } else {
      return (pkgName.length()==0?"":pkgName+".")+ fieldFactoryName;
    }
  }

  @Override
  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Field";
  }

  public static String getFieldFactoryName(final JClassType type,
    final String name) {
    final StringBuilder b = new StringBuilder(type.getName());
    b.append(MemberGenerator.FIELD_SPACER).append(name);
    return b.toString();
  }

  public String generateFieldFactory(final TreeLogger logger,
      final ReflectionGeneratorContext ctx,
      final JField field, String factoryName, final ReflectionManifest manifest)
        throws UnableToCompleteException {
      final String pkg = field.getEnclosingType().getPackage().getName();
      final JClassType enclosingType = field.getEnclosingType();
      final com.google.gwt.core.ext.typeinfo.JType fieldType = field.getType().getErasedType();
      final String jni = field.getType().getJNISignature();
      factoryName = factoryName.replace('.', '_');

      final SourceBuilder<JField> out = new SourceBuilder<JField>
      ("public final class " + factoryName).setPackage(pkg);

      out.getClassBuffer().createConstructor(PRIVATE);

      final GeneratorContext context = ctx.getGeneratorContext();
      final PrintWriter pw = context.tryCreate(logger, pkg, factoryName);
      if (pw == null) {
        if (isDebug(enclosingType, ReflectionStrategy.FIELD)) {
          logger.log(Type.INFO, "Skipped writing field for " + factoryName
            + ", as factory already exists");
        }
        return out.getQualifiedName();
      }

      final ClassBuffer cb = out.getClassBuffer();

      final GwtRetention retention = manifest.getRetention(field);

      final String ref = (field.isStatic() ? "" : "o.") + "@"
        + enclosingType.getQualifiedSourceName() + "::" + field.getName();
      final MethodBuffer accessor = cb
        .createMethod("private static JavaScriptObject getAccessor()")
        .setUseJsni(true)
        .println("return {").indent();
      accessor
      .println("getter: function(o) {");
      if (!field.isStatic()) {
        accessor.indentln(NULL_CHECK);
      }
      final boolean isPrimitive = field.getType().isPrimitive() != null;

      accessor.indent().print("return ");
      accessor.append(ref);

      accessor
      .println(";")
      .outdent()
      .print("}");
      if (field.isFinal()) {
        accessor.println().outdent().println("};");
      } else {
        accessor.println(", setter: function(o, v) {");
        if (!field.isStatic()) {
          accessor.indentln(NULL_CHECK);
        }
        accessor.indentln(ref + " = ");

        final StringBuilder unboxer = new StringBuilder();
        unboxer.append("v");

        accessor
        .indentln(unboxer + ";")
        .println("}")
        .outdent().println("};");
      }

      final MethodBuffer instantiate = cb
        .createMethod("public static Field instantiate()")
        .print("return new ")
        .addImports(Field.class, JavaScriptObject.class);

      if (isPrimitive) {
        switch (jni.charAt(0)) {
        case 'Z':
          instantiate.addImport("java.lang.reflect.Boolean_Field");
          instantiate.print("Boolean_Field(");
          break;
        case 'B':
          instantiate.addImport("java.lang.reflect.Byte_Field");
          instantiate.print("Byte_Field(");
          break;
        case 'S':
          instantiate.addImport("java.lang.reflect.Short_Field");
          instantiate.print("Short_Field(");
          break;
        case 'C':
          instantiate.addImport("java.lang.reflect.Char_Field");
          instantiate.print("Char_Field(");
          break;
        case 'I':
          instantiate.addImport("java.lang.reflect.Int_Field");
          instantiate.print("Int_Field(");
          break;
        case 'J':
          accessor.addAnnotation(UnsafeNativeLong.class);
          instantiate.addImport("java.lang.reflect.Long_Field");
          instantiate.print("Long_Field(");
          break;
        case 'F':
          instantiate.addImport("java.lang.reflect.Float_Field");
          instantiate.print("Float_Field(");
          break;
        case 'D':
          instantiate.addImport("java.lang.reflect.Double_Field");
          instantiate.print("Double_Field(");
          break;
        default:
          logger.log(Type.ERROR, "Bad primitive type in field generator "
            + fieldType.getQualifiedSourceName());
          throw new UnableToCompleteException();
        }
      } else {
        final String imported = instantiate.addImport(fieldType
          .getQualifiedSourceName());
        instantiate.print("Field(" + imported + ".class, ");
      }

      final String enclosing = instantiate.addImport(field.getEnclosingType()
        .getQualifiedSourceName());
      instantiate
        .print(enclosing + ".class, ")
        .print("\"" + field.getName() + "\", ")
        .print(ReflectionUtilType.getModifiers(field) + ", getAccessor(), ");

      appendAnnotationSupplier(logger, instantiate, field, retention, ctx);

      instantiate.print(");");

      final String src = out.toString();
      pw.println(src);
      if (isDebug(enclosingType, ReflectionStrategy.FIELD)) {
        logger.log(Type.INFO, "Field provider for " + field.toString() + "\n"
          + src);
      }

      ConstPoolGenerator.maybeCommit(logger, context);
      context.commit(logger, pw);
      return out.getQualifiedName();
    }

  /**
   * @see com.google.gwt.reflect.rebind.generators.MemberGenerator#getNotFoundExceptionType()
   */
  @Override
  protected IsQualified getNotFoundExceptionType() {
    return new IsQualified("java.lang", NoSuchFieldException.class.getSimpleName());
  }
}
