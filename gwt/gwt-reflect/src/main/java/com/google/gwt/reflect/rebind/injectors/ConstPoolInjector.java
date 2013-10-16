package com.google.gwt.reflect.rebind.injectors;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Queue;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.UnifyAstListener;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.AccessModifier;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.impl.UnifyAst.UnifyVisitor;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.reflect.rebind.generators.ConstPoolGenerator;
import com.google.gwt.reflect.rebind.generators.MemberGenerator;
import com.google.gwt.reflect.rebind.generators.ReflectionGeneratorContext;

@ReflectionStrategy
public class ConstPoolInjector implements MagicMethodGenerator, UnifyAstListener{

  private static final Type logLevel = Type.TRACE;
  private JMethodCall rememberClass;
  
  @Override
  public JExpression injectMagic(TreeLogger logger, JMethodCall methodCall, JMethod enclosingMethod,
    Context context, UnifyAstView ast) throws UnableToCompleteException {

    ReflectionGeneratorContext ctx = new ReflectionGeneratorContext(logger, null, methodCall, enclosingMethod, context, ast);
    
    HashMap<String,JPackage> packages = new HashMap<String,JPackage>();
    LinkedHashMap<JClassType,ReflectionStrategy> retained = new LinkedHashMap<JClassType,ReflectionStrategy>();
    ReflectionStrategy defaultStrategy = ConstPoolInjector.class.getAnnotation(ReflectionStrategy.class);
    TypeOracle oracle = ctx.getTypeOracle();
    boolean doLog = logger.isLoggable(logLevel);
    if (doLog)
      logger = logger.branch(logLevel, "Injecting all remaining members into ClassPool");
    for (com.google.gwt.core.ext.typeinfo.JClassType type : oracle.getTypes()) {
      ReflectionStrategy strategy = type.getAnnotation(ReflectionStrategy.class);
      if (strategy == null) {
        JPackage pkg = packages.get(type.getPackage().getName());
        if (pkg == null) {
          pkg = type.getPackage();
          packages.put(pkg.getName(), pkg);
        }
        strategy = pkg.getAnnotation(ReflectionStrategy.class);
        if (strategy == null) {
          if (type.findAnnotationInTypeHierarchy(GwtRetention.class) != null) {
            strategy = defaultStrategy;
          }
        }
      }
      if (strategy != null) {
        if (doLog) {
          logger.log(logLevel, "Adding type to ConstPool: "+type.getJNISignature());
        }
        retained.put(type, strategy);
      }
    }
    JDeclaredType gwtReflect = ast.searchForTypeBySource(GwtReflect.class.getName());
    JMethod magicClass = null;
    for (JMethod method : gwtReflect.getMethods()) {
      if (method.getName().equals("magicClass")) {
        magicClass = method;
        break;
      }
    }
    if (magicClass == null) {
      logger.log(Type.ERROR, "Unable to get a handle on GwtReflect.magicClass");
      throw new UnableToCompleteException();
    }

    SourceInfo methodSource = methodCall.getSourceInfo().makeChild(SourceOrigin.UNKNOWN);

    JMethod newMethod = new JMethod(methodSource, "enhanceAll", methodCall.getTarget().getEnclosingType(), ast.getProgram().getTypeVoid(), 
        false, true, true, AccessModifier.PUBLIC);
    
    newMethod.setOriginalTypes(ast.getProgram().getTypeVoid(), methodCall.getTarget().getOriginalParamTypes());
    JMethodBody body = new JMethodBody(methodSource);
    newMethod.setBody(body);
    JBlock block = body.getBlock();
    for (Entry<JClassType,ReflectionStrategy> type : retained.entrySet()) {
      JClassType cls = type.getKey();
      if (cls.isPrivate())
        continue;//use a jsni helper instead here.
      if (cls.getName().endsWith(ReflectionUtilJava.MAGIC_CLASS_SUFFIX) 
          || cls.getName().contains(MemberGenerator.METHOD_SPACER)
          || cls.getName().contains(MemberGenerator.FIELD_SPACER)
          || cls.getName().contains(MemberGenerator.CONSTRUCTOR_SPACER)
          )
        continue;
      JType asType = ast.getProgram().getFromTypeMap(cls.getQualifiedSourceName());
      if (asType == null) {
        continue;
      }
      JMethodCall call = new JMethodCall(methodSource.makeChild(SourceOrigin.UNKNOWN), null, magicClass);
      call.addArg(new JClassLiteral(methodSource.makeChild(SourceOrigin.UNKNOWN), asType));
      JExpression invoke = MagicClassInjector.injectMagicClass(logger, call, magicClass, context, ast);
      if (invoke != null)
        block.addStmt(invoke.makeStatement());
    }
    block.addStmts(((JMethodBody)methodCall.getTarget().getBody()).getStatements());
    methodCall.getTarget().getEnclosingType().addMethod(newMethod);
    JMethodCall call = new JMethodCall(methodSource, null, newMethod);
    
    ast.getRebindPermutationOracle().getGeneratorContext().finish(logger);
    return call.makeStatement().getExpr();
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
    ConstPoolGenerator.cleanup();
  }

}
