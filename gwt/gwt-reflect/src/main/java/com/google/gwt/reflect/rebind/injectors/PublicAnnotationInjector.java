/**
 *
 */
package com.google.gwt.reflect.rebind.injectors;

import static com.google.gwt.reflect.rebind.ReflectionUtilAst.debug;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.generators.ConstPoolGenerator;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator;
import com.google.gwt.reflect.rebind.generators.MemberGenerator;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsNamedType;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsQualified;

import java.lang.annotation.Annotation;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class PublicAnnotationInjector extends MemberGenerator implements MagicMethodGenerator {


  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall callSite,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {
    final boolean isDebug = logger.isLoggable(logLevel());
    final boolean isFromGwtReflect = callSite.getArgs().size() > 1;
    final JExpression inst = isFromGwtReflect ? callSite.getArgs().get(0) : callSite.getInstance();
    final JExpression annoParam = callSite.getArgs().get(isFromGwtReflect ? 1 : 0);

    if (isDebug) {
      logger.log(logLevel(), "Searching for annotation " + toString(annoParam) +
          " in literal from "+toString(inst));
    }

    final JClassLiteral classLit = ReflectionUtilAst.extractClassLiteral(logger, inst, ast, false);

    if (classLit == null) {
      if (isDebug) {
        logger.log(logLevel(),
            "Non-final class literal used to invoke getAnnotation via reflection; "
                + debug(callSite));
      }
      return maybeCheckConstPool(logger, ast, callSite, inst, annoParam);
    }
    if (isDebug) {
      logger.log(logLevel(), "Found class literal "+classLit.getRefType().getName());
      logger.log(logLevel(), "Searching for annotation "+annoParam.getClass().getName()+": "+annoParam);
    }

    final JClassLiteral annoLit = ReflectionUtilAst.extractClassLiteral(logger, annoParam, ast, false);
    if (annoLit == null) {
      if (isDebug) {
        logger.log(logLevel(),
            "Non-literal annotation class used to load annotation " + debug(annoParam)
              + "from type "+toString(classLit)+" ("
                + ReflectionUtilAst.debug(callSite)+")");
      }
      return maybeCheckConstPool(logger, ast, callSite, inst, annoParam);
    }
    if (isDebug) {
      logger.log(logLevel(), "Found annotation class: "+toString(annoLit));
    }

    final Type warnLevel = warnLevel(logger, ast);

    // We got our class and annotation literals, now look up the types we need and find the annotation to return
    final JClassType oracleType = ast.getTypeOracle().findType(classLit.getRefType().getName().replace('$', '.'));
    final String annoName = annoLit.getRefType().getName();
    final Annotation[] annotations = isDeclared() ? oracleType.getDeclaredAnnotations() : oracleType.getAnnotations();
    for (final Annotation annotation : annotations) {
      if (annoName.equals(annotation.annotationType().getName())) {
        // We have our winner! Generate a provider
        final IsQualified providerName = ReflectionUtilJava.generatedAnnotationProviderName(classLit, annoLit);

        JDeclaredType type = ast.searchForTypeBySource(providerName.getQualifiedName());

        IsNamedType provider = null;
        if (type != null) {
          provider = GwtAnnotationGenerator.findExisting(logger, annotation, ast.getTypeOracle());
        }
        if (provider == null) {
          final ConstPoolGenerator constGenerator = ConstPoolGenerator.getGenerator();
          provider = constGenerator.rememberAnnotation(logger, ast.getGeneratorContext(), annotation);

          ConstPoolGenerator.maybeCommit(logger, ast.getGeneratorContext());
          ast.getGeneratorContext().finish(logger);

          type = ast.searchForTypeBySource(provider.getQualifiedName());
          if (type == null) {
            logger.log(warnLevel, "Unable to find provider type "+provider.getQualifiedName());
            break;
          }
        }
        type = ast.translate(type);

        if (isDebug) {
          logger.log(logLevel(), "Generating annotation provider for "+annotation+" @ "+callSite.toSource());
        }
        for (final JField field : type.getFields()) {
          if (field.getName().equals(provider.getName())) {
            return new JFieldRef(callSite.getSourceInfo().makeChild(), null, field, type);
          }
        }
        logger.log(warnLevel, "Unable to find provider method "+provider.getQualifiedMemberName()+"()");
      }
    }

    logger.log(warnLevel, "Unable to find annotation of type "+annoName+" on type "+toString(classLit));
    return maybeCheckConstPool(logger, ast, callSite, inst, annoParam);
  }

  protected boolean isDeclared() {
    return false;
  }

  @Override
  protected String memberGetter() {
    return "get"+(isDeclared()?"Declared":"")+"Annotation";
  }

  /**
   * @see com.google.gwt.reflect.rebind.generators.MemberGenerator#getNotFoundExceptionType()
   */
  @Override
  protected IsQualified getNotFoundExceptionType() {
    throw new UnsupportedOperationException();
  }

  /**
   * @see com.google.gwt.reflect.rebind.generators.MemberGenerator#throwNotFoundException(com.google.gwt.core.ext.TreeLogger, com.google.gwt.dev.jjs.ast.JMethodCall, com.google.gwt.dev.jjs.UnifyAstView)
   */
  @Override
  public JExpression throwNotFoundException(final TreeLogger logger, final JMethodCall callSite, final UnifyAstView ast)
      throws UnableToCompleteException {
    return JNullLiteral.INSTANCE;
  }

}
