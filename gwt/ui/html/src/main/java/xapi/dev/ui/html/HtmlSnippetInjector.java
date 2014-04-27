package xapi.dev.ui.html;

import java.util.List;

import xapi.log.X_Log;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.html.api.HtmlSnippet;

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
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
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
    boolean isToHtml = "toHtml".equals(methodCall.getTarget().getName());
    int instanceIndex = args.size() - 2;
    
    logger.log(Type.INFO, "Injecting "+methodCall.getTarget().getName()+" for "+typeLiteral.getRefType().getName());
    
    TypeOracle oracle = ast.getTypeOracle();
    ast.translate(typeLiteral.getRefType());
    
    com.google.gwt.core.ext.typeinfo.JClassType templateType;
    try {
      templateType = oracle.getType(typeLiteral.getRefType().getName().replace('$', '.'));
    } catch (NotFoundException e) {
      logger.log(Type.ERROR, "Unable to load "+typeLiteral.getRefType()+" from the type oracle");
      throw new UnableToCompleteException();
    }

    com.google.gwt.core.ext.typeinfo.JClassType modelType;
    try {
      if (args.size() == (isToHtml ? 3 : 2)) {
        modelType = templateType;
      } else {
        JClassLiteral modelLiteral = ReflectionUtilAst.extractClassLiteral(logger, args.get(1), ast, true);
        ast.translate(modelLiteral.getRefType());
        modelType = oracle.getType(modelLiteral.getRefType().getName().replace('$', '.'));
      }
    } catch (NotFoundException e) {
      logger.log(Type.ERROR, "Unable to load "+typeLiteral.getRefType()+" from the type oracle");
      throw new UnableToCompleteException();
    }
    
    String provider;
    provider = HtmlSnippetGenerator.generateSnippetProvider(logger, ast, templateType, modelType);
    // Force load the type
    ast.getTypeOracle().findType(provider);
    
    // Grab the type from the jjs ast
    
    JClassType uiType = (JClassType) ast.searchForTypeBySource(provider);
    SourceInfo info = methodCall.getSourceInfo().makeChild();
    JExpression inst = null;
    for (JMethod method : uiType.getMethods()) {
      if (method instanceof JConstructor) {
        JNewInstance newInst = new JNewInstance(info, (JConstructor) method, null, args.get(args.size()-1).makeStatement().getExpr());
        inst = newInst;
        logger.log(Type.INFO, method.getSignature()+" : "+newInst.getArgs());
        break;
      }
    }
    X_Log.info(getClass(), "Generated X_Html implementation ", uiType);
    if (isToHtml) {
      JDeclaredType snippet = ast.searchForTypeBySource(HtmlSnippet.class.getName());
      for (JMethod method : snippet.getMethods()) {
        if (method.getName().equals("convert")) {
          return new JMethodCall(info, inst, method, args.get(instanceIndex));
        }
      }
    }
    if (inst == null) {
      logger.log(Type.ERROR, "Unable to find method newUi in generated UI provider "+provider);
      throw new UnableToCompleteException();
    }
    return inst;
  }


}
