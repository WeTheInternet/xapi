package xapi.dev.ui.autoui;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;

import java.util.List;

import xapi.ui.autoui.api.UserInterface;

/**
 * A magic method injector for the method X_AutoUi.makeUi:
 * <pre>
 * public static <T, U extends UserInterface<T>> U makeUi(T model, Class<? extends T> uiOptions, Class<U> uiType)
 * </pre>
 * This magic method will create a generated class that is much more efficient at runtime than
 * what the default code does (via reflection).  At present, sending non-class literals to this
 * method is not supported.  It can be supported by using reflection in Gwt, but it requires a lot
 * of reflection which comes with a lot of overhead, and will not be supported unless there is a
 * very compelling use case offered.
 * <br/>
 * If you do want to use an abstraction where you do not know the literal types when you invoke
 * the ui generator, simply create a factory abstraction which does know the literal types,
 * and pass those around to do your decoupled ui generation.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class AutoUiInjector implements MagicMethodGenerator {

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall methodCall,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {
    final List<JExpression> args = methodCall.getArgs();
    final JClassLiteral uiOptionsLiteral = ReflectionUtilAst.extractClassLiteral(logger, args.get(1), ast, true);
    final JClassLiteral uiTypeLiteral = ReflectionUtilAst.extractClassLiteral(logger, args.get(2), ast, true);


    final TypeOracle oracle = ast.getTypeOracle();
    ast.translate(uiOptionsLiteral.getRefType());
    com.google.gwt.core.ext.typeinfo.JClassType uiOptions;
    try {
      uiOptions = oracle.getType(uiOptionsLiteral.getRefType().getName().replace('$', '.'));
    } catch (final NotFoundException e) {
      logger.log(Type.ERROR, "Unable to load "+uiOptionsLiteral.getRefType()+" from the type oracle");
      throw new UnableToCompleteException();
    }

    String provider;
    try {
      final com.google.gwt.core.ext.typeinfo.JClassType uiType = oracle.getType(uiTypeLiteral.getRefType().getName().replace('$', '.'));
      provider = AutoUiGenerator.generateUiProvider(logger, ast, uiOptions, uiType);
    } catch (final NotFoundException e) {
      logger.log(Type.ERROR, "Unable to load "+uiOptionsLiteral.getRefType()+" from the type oracle");
      throw new UnableToCompleteException();
    }
    // Force load the type
    ast.getTypeOracle().findType(provider);

    // Grab the type from the jjs ast

    final JClassType uiType = (JClassType) ast.searchForTypeBySource(provider);
    final SourceInfo info = methodCall.getSourceInfo().makeChild();
    JExpression inst = null;
    for (final JMethod method : uiType.getMethods()) {
      if ("newUi".equals(method.getName())) {
        inst = new JMethodCall(info, null, method);
        break;
      }
    }
    if (inst == null) {
      logger.log(Type.ERROR, "Unable to find method newUi in generated UI provider "+provider);
      throw new UnableToCompleteException();
    }

    // Possible render the ui immediately, if a non-null model was supplied
    if (args.get(0) != JNullLiteral.INSTANCE) {
      // We need to call renderUi on the new instance.
      final JDeclaredType uiCls = ast.searchForTypeBySource(UserInterface.class.getName());
      for (final JMethod method : uiCls.getMethods()) {
        if (method.getName().equals("renderUi")) {
          final JMethodCall call = new JMethodCall(info.makeChild(), inst, method);
          call.addArg(args.get(0));
          return call;
        }
      }
    }
    return inst;
  }


}
