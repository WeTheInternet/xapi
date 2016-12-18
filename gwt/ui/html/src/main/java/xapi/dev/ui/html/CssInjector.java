package xapi.dev.ui.html;

import xapi.ui.api.StyleService;
import xapi.ui.html.X_Html;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.*;
import com.google.gwt.reflect.rebind.ReflectionUtilAst;

import java.util.List;

/**
 * A magic method injector for {@link X_Html#injectCss(Class, xapi.ui.api.StyleService)}
 * <p>
 * This magic method will replace the call to X_Html.injectStyle with one or more calls
 * to {@link StyleService#addCss(String, int)}, matching all style defined on the Class,
 * it's respective methods, and all annotated supertypes.
 *
 * It is recommended to do this statically in the classes themselves, so that you get
 * "just in time injection" so that referencing the class injects the css as needed,
 * and not referencing the class prevents the inclusion of css that you don't need.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class CssInjector implements MagicMethodGenerator {

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall methodCall,
      final JMethod enclosingMethod, final Context context, final UnifyAstView ast)
      throws UnableToCompleteException {
    final List<JExpression> args = methodCall.getArgs();
    final JClassLiteral typeLiteral = ReflectionUtilAst.extractClassLiteral(logger, args.get(0), ast, true);

    logger.log(Type.TRACE, "Injecting css for "+typeLiteral.getRefType().getName());

    // Ensure the type is loaded by the type oracle.
    final TypeOracle oracle = ast.getTypeOracle();
    final JType translated = ast.translate(typeLiteral.getRefType());

    // Load the type from the type oracle
    com.google.gwt.core.ext.typeinfo.JClassType templateType;
    try {
      templateType = oracle.getType(typeLiteral.getRefType().getName().replace('$', '.'));
    } catch (final NotFoundException e) {
      logger.log(Type.ERROR, "Unable to load "+typeLiteral.getRefType()+" from the type oracle");
      throw new UnableToCompleteException();
    }

    logger.log(TreeLogger.TRACE, "Template translated : " + translated.toSource());
    HtmlGeneratorResult provider;
    provider = CssInjectorGenerator.generateSnippetProvider(logger, ast, templateType);
    // Force load the type, to ensure it is parsed into AST
    ast.getTypeOracle().findType(provider.getFinalName());

    // Grab the type from the jjs ast
    final JClassType injectorType = (JClassType) ast.searchForTypeBySource(provider.getFinalName());
    final SourceInfo info = methodCall.getSourceInfo().makeChild();
    for (final JMethod method : injectorType.getMethods()) {
      if (method.getName().equals("inject")) {
        JExpression arg;
        if (args.size() > 1) {
          // The X_Html method receives a reference to the StyleService to use
          arg = args.get(1);
        } else {
          // The X_Element method does not take a StyleService, and instead uses it's own default
          final JDeclaredType elemental = ast.searchForTypeBySource("xapi.elemental.X_Elemental");
          final String serviceType = "Lxapi/elemental/api/ElementalService;";
          final JMethod getService = elemental.findMethod("getElementalService()"+serviceType, false);
          arg = new JMethodCall(method.getSourceInfo(), null, getService);
        }
        return new JMethodCall(info, null, method, arg);
      }
    }
    logger.log(Type.ERROR, "Unable to find method inject in generated UI provider "+provider);
    throw new UnableToCompleteException();
  }


}
