/**
 *
 */
package xapi.dev.elemental;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceInfoCorrelation;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;

import xapi.dev.elemental.ElementalGeneratorContext.ElementalGeneratorResult;
import xapi.elemental.X_Elemental;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.log.X_Log;
import xapi.ui.api.Stylizer;
import xapi.ui.api.Widget;
import xapi.util.api.ConvertsValue;
import elemental.dom.Element;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class ElementalInjector implements
    MagicMethodGenerator,
    UnifyAstListener {

  private static ThreadLocal<ElementalInjector> context = new ThreadLocal<>();
  private JMethodCall service;
  private ElementalGeneratorContext ctx;

  public static ElementalGeneratorContext getContext() {
    return context.get().ctx;
  }

  public ElementalInjector() {
    assert context.get() == null : "ElementalInjector must only be created once per thread";
    context.set(this);
  }

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall methodCall,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {
    final List<JExpression> args = methodCall.getArgs();
    JExpression instance = methodCall.getInstance();
    final SourceInfo info = methodCall.getSourceInfo().makeChild();
    final String name = methodCall.getTarget().getName();
    final boolean invokeProvider = name.equals(ElementalService.METHOD_TO_ELEMENT);
    assert invokeProvider ||
      name.equals(ElementalService.METHOD_TO_ELEMENT_BUILDER) :
          "Unsupported method type "+name;

    if (instance == null) {
      instance = getService(logger, ast);
    }

    final int locationOfTemplate = args.size() - (invokeProvider ? 2 : 1);

    final JClassLiteral templateLiteral,
      modelLiteral = ReflectionUtilAst.extractClassLiteral(logger, args.get(0), ast, true);

    if (locationOfTemplate == 0) {
      templateLiteral = modelLiteral;
    } else {
      templateLiteral = ReflectionUtilAst.extractClassLiteral(logger, args.get(locationOfTemplate), ast, true);
    }

    final com.google.gwt.core.ext.typeinfo.JClassType modelType =
        ast.getTypeOracle().findType(modelLiteral.getRefType().getName());
    final com.google.gwt.core.ext.typeinfo.JClassType templateType =
        ast.getTypeOracle().findType(templateLiteral.getRefType().getName());
    JClassType classLit;
    // Implement the type's element builders.
    final ElementalGeneratorResult impl =
      ElementalGenerator.generateProvider(logger, new ModelProviderImpl(modelType), templateType, ast, ctx);
    ast.getGeneratorContext().finish(logger);
    classLit = ast.translate((JClassType) ast.searchForTypeBySource(impl.getFinalName()));
    for (final JMethod init : classLit.getMethods()) {
      if (init instanceof JConstructor) {
        final JConstructor ctor = (JConstructor) init;
        final JNewInstance invoke = new JNewInstance(info, ctor, instance);
        if (invokeProvider) {
          JMethod convert;
          try {
            convert = ast.getProgram().getIndexedMethod("ConvertsValue.convert");
          } catch (final InternalCompilerException e) {
            logger.log(Type.WARN, "Unable to look up indexed ConvertsValue.convert(); looking up method manually");
            error:
            {
              for (final JMethod method : ast.searchForTypeBySource(
                  ConvertsValue.class.getName()).getMethods()) {
                if (method.getName().equals("convert")) {
                  convert = method;
                  break error;
                }
              }
              throw e;
            }
          }
          final JMethodCall expr = new JMethodCall(info, invoke, convert, args.get(1));
          try {
            convert =
                ast.getProgram().getIndexedMethod(Widget.class.getSimpleName()+ ".getElement");
          } catch (final InternalCompilerException e) {
            logger.log(Type.WARN, "Unable to look up indexed PotentialNode.getElement(); looking up method manually");
            error:
            {
              for (final JMethod method : ast.searchForTypeBySource(
                  PotentialNode.class.getName()).getMethods()) {
                if (method.getName().equals("getElement")) {
                  convert = method;
                  break error;
                }
              }
              throw e;
            }
          }
          return new JMethodCall(info, expr, convert);
        }
        return invoke;
      }
    }
    logger.log(Type.ERROR, "Unable to generate elemental builder for "
        + methodCall);
    throw new UnableToCompleteException();
  }

  /**
   * @param logger
   * @param ast
   * @return
   * @throws UnableToCompleteException
   */
  private JExpression getService(final TreeLogger logger, final UnifyAstView ast)
      throws UnableToCompleteException {
    if (service == null) {
      JMethod serviceMethod;
      try {
        serviceMethod =
          ast.getProgram().getIndexedMethod("X_Elemental.getElementalService");
      } catch (final InternalCompilerException e) {
        error:
        {
          for (final JMethod method : ast.searchForTypeBySource(
              X_Elemental.class.getName()).getMethods()) {
            if (method.getName().equals("getElementalService")) {
              serviceMethod = method;
              break error;
            }
          }
          logger.log(Type.ERROR,
              "Unable to find X_Elemental.getElementalService on classpath");
          throw new UnableToCompleteException();
        }
      }
      service =
        new JMethodCall(new SourceInfoCorrelation(SourceOrigin.UNKNOWN), null,
            serviceMethod);
    }
    return service;
  }

  @Override
  public void onUnifyAstStart(final TreeLogger logger, final UnifyAstView ast,
      final UnifyVisitor visitor, final Queue<JMethod> todo) {
    this.ctx = new ElementalGeneratorContext(logger, ast);
    final JProgram prog = ast.getProgram();
    try {
      final Method method =
        prog.getClass().getMethod("addIndexedTypeName", String.class);
      method.invoke(prog, X_Elemental.class.getName());
      method.invoke(prog, ElementalService.class.getName());
      method.invoke(prog, ConvertsValue.class.getName());
      method.invoke(prog, Element.class.getName());
      method.invoke(prog, Widget.class.getName());
      method.invoke(prog, Stylizer.class.getName());
      method.invoke(prog, PotentialNode.class.getName());
    } catch (final Throwable e) {
      X_Log.warn(getClass(), "Could not load indexed methods", e);
    }
  }

  @Override
  public boolean onUnifyAstPostProcess(final TreeLogger logger, final UnifyAstView ast,
      final UnifyVisitor visitor, final Queue<JMethod> todo) {
    return false;
  }

  @Override
  public void destroy(final TreeLogger logger) {
    context.remove();
    ctx = null;
    service = null;
  }

}
