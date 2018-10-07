package xapi.dev.model;

import xapi.annotation.model.IsModel;
import xapi.dev.api.ApiGeneratorTools;
import xapi.fu.In1;
import xapi.fu.api.Ignore;
import xapi.gwt.model.ModelGwt;
import xapi.inject.X_Inject;
import xapi.model.impl.ModelUtil;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;

import java.util.*;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.*;
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
    models = new LinkedHashMap<>();
  }

  @SuppressWarnings("unused") // called by reflection
  public static JExpression rebindInstance(
    final TreeLogger logger, final JMethodCall call, final JMethod method, final Context context, final UnifyAstView ast) throws UnableToCompleteException {

    final ModelMagic instance = active.get();
    if (instance == null) {
      logger.log(Type.ERROR, "Null static instance of ModelMagic");
      throw new UnableToCompleteException();
    }
    return instance.injectMagic(logger, call, method, context, ast);
  }

  @Override
  public void onUnifyAstStart(final TreeLogger logger, final UnifyAstView ast, final UnifyVisitor visitor, final Queue<JMethod> todo) {
  }

  @Override
  public boolean onUnifyAstPostProcess(final TreeLogger logger, final UnifyAstView ast, final UnifyVisitor visitor, final Queue<JMethod> todo) {
    return false;
  }

  @Override
  public void destroy(final TreeLogger logger) {
    // clean up our static reference
    // this is necessary to allow super-dev-mode to recompile correctly.
    // without scope on when we are recompiling versus when we are doing an
    // initial compile, we can't figure out if we should reuse or regen types.
    active.remove();
  }

  @Override
  public JExpression injectMagic(final TreeLogger logger, final JMethodCall call, final JMethod currentMethod, final Context context,
    final UnifyAstView ast) throws UnableToCompleteException {
    final List<JExpression> args = call.getArgs();
    final String methodName = call.getTarget().getName();
    if (args.size() != 1) {
      logger.log(Type.ERROR, "X_Model."+methodName+"() expects one and only one parameter; a class literal.");
      throw new UnableToCompleteException();
    }
    final JExpression arg0 = args.get(0);
    if (!(arg0 instanceof JClassLiteral)) {
      logger.log(Type.ERROR, "X_Model."+methodName+"() expects a class literal as argument; you sent a "+ arg0.getClass()+" : "+arg0);
      logger.log(Type.ERROR, "Method call: "+call.toSource());
      logger.log(Type.ERROR, "Method call: "+call.getTarget().toSource());
      if (logger.isLoggable(Type.INFO)) {
        logger.log(Type.INFO, "Enclosing type source: "+call.getTarget().getEnclosingType().toSource());
      }
      throw new UnableToCompleteException();
    }
    final JClassLiteral classLit = (JClassLiteral)arg0;

    // from our classLiteral, generate a Model class.
    // TODO see if we can detect package protected type and adjust package accordingly
    final String simpleName = mangleName(classLit.getRefType().getName().replace('$', '.'), false);
    final String modelName = "xapi.model."+simpleName;

    JDeclaredType existing = ast.searchForTypeBySource(modelName);
    final String fqcn = classLit.getRefType().getName();
    final StandardGeneratorContext ctx = ast.getRebindPermutationOracle().getGeneratorContext();
    if (null == existing) {
      // Type does not yet exist, let's create one!
      final RebindResult result = ModelGeneratorGwt.execImpl(logger,
        ctx, fqcn);
      ctx.finish(logger);
      existing = ast.searchForTypeBySource(result.getResultTypeName());
      if (existing == null) {
        logger.log(Type.ERROR, "Unable to find model "+result.getResultTypeName()+"; perhaps it has compile errors?");
        throw new UnableToCompleteException();
      }
    } else if (!models.containsKey(fqcn)) {
      // The type was loaded from source; we still need to create an artifact for it.
      final com.google.gwt.core.ext.typeinfo.JClassType type  = ModelGeneratorGwt.normalize(ctx, fqcn);
      final ModelArtifact artifact = getOrMakeModel(logger, ctx, type);
      ModelGeneratorGwt.visitModelStructure(logger, ctx, type, this, artifact, In1.ignored());
      // TODO: do not require SourceBuilder here... gonna be tough to get rid of;
      // need to cleanly separate "grab metadata" from "generate thing we need".
      // Best bet is a new interface, that accepts an abstraction over gwt ast types,
      // so we can run the same set of model-generating code in gwt, xapi, jvm, etc.
      // This new interface would only be used to collect the information in ModelArtifact,
      // (the extracted source-generating / gwt-committing logic gets it's own class).
      artifact.generateModelClass(logger, ModelGeneratorGwt.createBuilder(type, this), ctx, type);
      ctx.commitArtifact(logger, artifact);
    }
    final JClassType asCls = (JClassType)existing;
    final String targetMethod = "create".equals(methodName) ? "newInstance" : "register";
    for (final JMethod method : asCls.getMethods()) {
      if (method.getName().equals(targetMethod)) {
        // static method call.
        return new JMethodCall(method.getSourceInfo(), null, method);
      }
    }
    logger.log(Type.ERROR, "Unable to find .newInstance() in generated model " +
        "class "+modelName);
    throw new UnableToCompleteException();
  }

  public static void initialize() {
    if (active.get() == null) {
      active.set(X_Inject.instance(ModelMagic.class));
    }
  }

  public String mangleName(final String fqcn, final boolean minify) {
    String minified = nameMap.get(fqcn);
    if (minified == null) {
      if (minify) {
        minified = nextId();
      } else {
        final StringTokenizer tokens = new StringTokenizer(fqcn, ".");
        final StringBuilder b = new StringBuilder('M');
        while (tokens.hasMoreElements()) {
          b.append(tokens.nextToken().charAt(0)).append('_');
        }
        minified = b+fqcn.substring(fqcn.lastIndexOf('.')+1);
      }
      nameMap.put(fqcn, minified);
    }
    return minified;
  }

  public boolean hasModel(final String typeName) {
    return models.containsKey(typeName);
  }

  public ModelArtifact getOrMakeModel(final TreeLogger logger,
    final GeneratorContext ctx, final com.google.gwt.core.ext.typeinfo.JClassType type) {
    ModelArtifact model = models.get(type.getName());
    if (model == null) {
      final IsModel isModel = type.getAnnotation(IsModel.class);
      final String name;
      if (isModel == null) {
        name = ModelUtil.guessModelType(type.getSimpleSourceName());
      } else {
        name = isModel.modelType();
      }
      ApiGeneratorTools<?> t = new ApiGeneratorTools() {};
      model = new ModelArtifact(t, name, type.getQualifiedBinaryName());
      model.applyDefaultAnnotations(logger, type);
      models.put(type.getName(), model);
    } else {
      model.setReused();
    }
    return model;
  }

  private com.google.gwt.core.ext.typeinfo.JClassType root;
  public com.google.gwt.core.ext.typeinfo.JClassType getRootType
    (final TreeLogger logger, final GeneratorContext ctx) throws UnableToCompleteException {
    if (root != null) {
      return root;
    }
    final String rootType = X_Properties.getProperty(X_Namespace.PROPERTY_MODEL_ROOT,
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

  private void bailNoGwtModel(final TreeLogger logger) throws UnableToCompleteException {
    logger.log(Type.ERROR, "Could not find "+ModelGwt.class.getName()+" on " +
    		"source lookup path for gwt compile.  Ensure you inherit artifact xapi-gwt-model.");
    throw new UnableToCompleteException();
  }

  public static boolean shouldIgnore(HasAnnotations source) {
    final Ignore anno = source.getAnnotation(Ignore.class);
    if (anno == null) {
      return false;
    }
    for (String s : anno.value()) {
      switch (s) {
        case Ignore.ALL:
        case IsModel.NAMESPACE:
          return true;
      }
    }
    // Empty ignore value counts as "always ignore"
    return anno.value().length == 0;
  }

  public static boolean shouldIgnore(com.google.gwt.core.ext.typeinfo.JMethod method) {
    if (method.isDefaultMethod()) {
      return true;
    }
    switch (method.getName()) {
      case "onChange":
      case "onGlobalChange":
        return true;
    }
    return shouldIgnore((HasAnnotations)method);
  }
}
