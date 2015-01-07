package xapi.dev.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringTokenizer;

import xapi.gwt.model.ModelGwt;
import xapi.inject.X_Inject;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;

public class ModelMagic implements UnifyAstListener, MagicMethodGenerator {

  static final ThreadLocal<ModelMagic> active = new ThreadLocal<ModelMagic>();

  private final Map<String,ModelArtifact> models;

  private static int nextUnique = 1;
  private final Map<String,String> nameMap = new HashMap<String,String>();

  synchronized String nextId() {
    return "M"+Long.toString(nextUnique++, 26);
  }

  public ModelMagic() {
    active.set(this);
    models = new LinkedHashMap<String,ModelArtifact>();
  }

  public static JExpression rebindInstance(
    TreeLogger logger, JMethodCall call, JMethod method, Context context, UnifyAstView ast) throws UnableToCompleteException {

    ModelMagic instance = active.get();
    if (instance == null) {
      logger.log(Type.ERROR, "Null static instance of ModelMagic");
      throw new UnableToCompleteException();
    }
    return instance.injectMagic(logger, call, method, context, ast);
  }

  @Override
  public void onUnifyAstStart(TreeLogger logger, UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo) {

  }

  @Override
  public boolean onUnifyAstPostProcess(TreeLogger logger, UnifyAstView ast, UnifyVisitor visitor, Queue<JMethod> todo) {
    return false;
  }

  @Override
  public void destroy(TreeLogger logger) {
    // clean up our static reference
    // this is necessary to allow super-dev-mode to recompile correctly.
    // without scope on when we are recompiling versus when we are doing an
    // initial compile, we can't figure out if we should reuse or regen types.
    active.remove();
  }

  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall call, JMethod currentMethod, Context context,
    UnifyAstView ast) throws UnableToCompleteException {
    List<JExpression> args = call.getArgs();
    if (args.size() != 1) {
      logger.log(Type.ERROR, "X_Model.create() expects one and only one parameter; a class literal.");
      throw new UnableToCompleteException();
    }
    JExpression arg0 = args.get(0);
    if (!(arg0 instanceof JClassLiteral)) {
      logger.log(Type.ERROR, "X_Model.create() expects a class literal as argument; you sent a "
        + arg0.getClass()+" : "+arg0);
      throw new UnableToCompleteException();
    }
    JClassLiteral classLit = (JClassLiteral)arg0;

    // from our classLiteral, generate a Model class.
    // TODO see if we can detect package protected type and adjust package accordingly
    String simpleName = mangleName(classLit.getRefType().getName().replace('$', '.'), false);
    String modelName = "xapi.model."+simpleName;

    JDeclaredType existing;
    try {
      existing = ast.searchForTypeBySource(modelName);
    } catch (NoClassDefFoundError e) {
      // Type does not yet exist, let's create one!
      StandardGeneratorContext ctx = ast.getRebindPermutationOracle().getGeneratorContext();
      RebindResult result = ModelGeneratorGwt.execImpl(logger,
        ctx, classLit.getRefType().getName());
      ctx.finish(logger);
      existing = ast.searchForTypeBySource(result.getResultTypeName());
    }
    JClassType asCls = (JClassType)existing;
    for (JMethod method : asCls.getMethods()) {
      if (method.getName().equals("newInstance")) {
        // static method call.
        return new JMethodCall(method.getSourceInfo(), null, method);
      }
    }
    logger.log(Type.ERROR, "Unable to find .newModel() in generated model " +
        "class "+modelName);
    throw new UnableToCompleteException();
  }

  public static void initialize() {
    if (active.get() == null) {
      active.set(X_Inject.instance(ModelMagic.class));
    }
  }



  public String mangleName(String fqcn, boolean minify) {
    String minified = nameMap.get(fqcn);
    if (minified == null) {
      if (minify) {
        minified = nextId();
      } else {
        StringTokenizer tokens = new StringTokenizer(fqcn, ".");
        StringBuilder b = new StringBuilder('M');
        while (tokens.hasMoreElements()) {
          b.append(tokens.nextToken().charAt(0)).append('_');
        }
        minified = b+fqcn.substring(fqcn.lastIndexOf('.')+1);
      }
      nameMap.put(fqcn, minified);
    }
    return minified;
  }

  public boolean hasModel(String typeName) {
    return models.containsKey(typeName);
  }

  public ModelArtifact getOrMakeModel(TreeLogger logger,
    GeneratorContext ctx, com.google.gwt.core.ext.typeinfo.JClassType type) {
    ModelArtifact model = models.get(type.getName());
    if (model == null) {
      model = new ModelArtifact(type.getName());
      models.put(type.getName(), model);
    }
    return model;
  }

  private com.google.gwt.core.ext.typeinfo.JClassType root;
  public com.google.gwt.core.ext.typeinfo.JClassType getRootType
    (TreeLogger logger, GeneratorContext ctx) throws UnableToCompleteException {
    if (root != null) {
      return root;
    }
    String rootType = X_Properties.getProperty(X_Namespace.PROPERTY_MODEL_ROOT,
      ModelGwt.class.getName());
    root = ctx.getTypeOracle().findType(rootType);
    if (root == null) {
      if (rootType.equals(ModelGwt.class.getName())) {
        bailNoGwtModel(logger);
      } else {
        root = ctx.getTypeOracle().findType(ModelGwt.class.getName());
        if (root == null) {
          bailNoGwtModel(logger);
        }
      }
    }
    logger.log(Type.DEBUG, "");
    return root;
  }

  private void bailNoGwtModel(TreeLogger logger) throws UnableToCompleteException {
    logger.log(Type.ERROR, "Could not find "+ModelGwt.class.getName()+" on " +
    		"source lookup path for gwt compile.  Ensure you inherit xapi-gwt-model:sources.");
    throw new UnableToCompleteException();
  }


}
