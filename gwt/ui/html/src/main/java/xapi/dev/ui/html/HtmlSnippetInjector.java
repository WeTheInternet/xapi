package xapi.dev.ui.html;

import java.util.List;

import xapi.ui.autoui.api.UserInterface;

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
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;

/**
 * A magic method injector for the methods X_Html.toHtml and X_Html.toSnippet:
 * <pre>
 * public static <T> String toHtml(Class<? extends T> type, T model, HtmlBuffer context)
 * public static <T> HtmlSnippet<T> toSnippet(Class<? extends T> type, HtmlBuffer context)
 * </pre>
 * This magic method will create a generated class that is much more efficient at runtime than
 * what the default code does (via reflection).  At present, sending non-class literals to this 
 * method is not supported.  It can be supported by using reflection in Gwt, but it requires a lot
 * of reflection which comes with a lot of overhead, and will not be supported unless there is a
 * very compelling use case offered.  
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class HtmlSnippetInjector implements MagicMethodGenerator {

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall,
      JMethod enclosingMethod, Context context, UnifyAstView ast)
      throws UnableToCompleteException {
    List<JExpression> args = methodCall.getArgs();
    JClassLiteral typeLiteral = ReflectionUtilAst.extractClassLiteral(logger, args.get(0), ast, true);
    boolean isToHtml = args.size() == 3;
    
    
    TypeOracle oracle = ast.getTypeOracle();
    ast.translate(typeLiteral.getRefType());
    
    com.google.gwt.core.ext.typeinfo.JClassType uiOptions;
    try {
      uiOptions = oracle.getType(typeLiteral.getRefType().getName().replace('$', '.'));
    } catch (NotFoundException e) {
      logger.log(Type.ERROR, "Unable to load "+typeLiteral.getRefType()+" from the type oracle");
      throw new UnableToCompleteException();
    }
    
    String provider;
    provider = HtmlSnippetGenerator.generateSnippetProvider(logger, ast, uiOptions);
    // Force load the type
    ast.getTypeOracle().findType(provider);
    
    // Grab the type from the jjs ast
    
    JClassType uiType = (JClassType) ast.searchForTypeBySource(provider);
    SourceInfo info = methodCall.getSourceInfo().makeChild();
    JExpression inst = null;
    for (JMethod method : uiType.getMethods()) {
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
    if (!(args.get(0).getType() instanceof JNullType)) {
      // We need to call renderUi on the new instance.
      JDeclaredType uiCls = ast.searchForTypeBySource(UserInterface.class.getName());
      for (JMethod method : uiCls.getMethods()) {
        if (method.getName().equals("renderUi")) {
          JMethodCall call = new JMethodCall(info.makeChild(), inst, method);
          call.addArg(args.get(0));
          return call;
        }
      }
    }
    return inst;
  }


}
