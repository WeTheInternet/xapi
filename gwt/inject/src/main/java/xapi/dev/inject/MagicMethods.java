/*
 * Copyright 2012, We The Internet Ltd.
 *
 * All rights reserved.
 *
 * Distributed under a modified BSD License as follow:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution, unless otherwise
 * agreed to in a written document signed by a director of We The Internet Ltd.
 *
 * Neither the name of We The Internet nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package xapi.dev.inject;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import xapi.dev.generators.AsyncInjectionGenerator;
import xapi.dev.generators.AsyncProxyGenerator;
import xapi.dev.generators.InstanceInjectionGenerator;
import xapi.dev.generators.SyncInjectionGenerator;
import xapi.dev.util.InjectionCallbackArtifact;
import xapi.inject.AsyncProxy;
import xapi.inject.X_Inject;
import xapi.inject.impl.SingletonProvider;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.MagicMethodGenerator;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.AccessModifier;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.impl.UnifyAst;
import com.google.gwt.dev.util.collect.Lists;

/**
 * A collection of magic method providers used for gwt production mode. These methods are mapped over top of
 * existing static methods, to allow modification / extension of GWT.create() (the original implementation is
 * jacked directly from GWT.create() in {@link UnifyAst}). Because these methods are only used in production
 * mode, the methods they override must implement the same functionality for gwt dev mode / pure java. It is
 * recommended to direct your magic methods outside of the class from which they are called; the contents of
 * the class they are sourced from will be included in production gwt compiles, so simply delegate your static
 * methods from X_Impl.someMethod -> Jre_Impl.someMethod, and do your work there. In order to inject your own
 * magic methods, simply include xapi.X_Inject in your gwt module, then add the following (assuming
 * your method is: <extend-configuration-property name="gwt.magic.methods" value=
 * "com.examply.X_Impl.someMethod(Ljava/lang/Class;)Ljava/lang/Object; *= com.example.dev.MagicMethods::someMethod"
 * /> This value string is a little monsterous, but it simple maps the type signature of the method you want
 * to replace, with the method you will use to build the AST. The type signature of your static injection
 * methods must match the supplied signature of
 * {@link MagicMethodGenerator#injectMagic(TreeLogger, JMethodCall, JMethod, Context, UnifyAstView)}, though you may
 * name your methods anything you please, and must return a JExpression or throw an exception. Returning null
 * will allow your code to compile, but you will hit mysterious null.nullMethod() errors. (in other words,
 * throw an exception instead of return null; unless you love hours of debugging.)
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
public class MagicMethods {

  /**
   * Replaces a call from {@link X_Inject#singletonAsync(Class, xapi.util.api.ReceivesValue)} by first a)
   * generating an async provider, and then b) sending the value receiver into the async provider as a
   * callback. See the {@link AsyncProxy} class and {@link AsyncInjectionGenerator} for implementation.
   *
   * @param logger - The logger to log to.
   * @param methodCall - The method call we are overwriting
   * @param currentMethod - The encapsulated method itself
   * @param context - The method call context, so you can insert clinits / whatnot
   * @param ast - A view over UnifyAst, exposing our basic needs
   * @return - A JExpression to replace this method call with
   * @throws UnableToCompleteException
   */
  public static JExpression rebindSingletonAsync(TreeLogger logger, JMethodCall methodCall,
    JMethod currentMethod, Context context, UnifyAstView ast) throws UnableToCompleteException {
    assert (methodCall.getArgs().size() == 2);
    JExpression classParam = methodCall.getArgs().get(0);
    JExpression receiverParam = methodCall.getArgs().get(1);
    if (!(classParam instanceof JClassLiteral)) {
      ast.error(
        methodCall,
        "Only class literals may be used as arguments to X_Inject.singletonAsync; you sent " +
          classParam.getClass() + " - " + classParam);
      return null;
    }

    logger.log(Type.INFO,
      receiverParam.toString() + " : " + receiverParam.getClass() + " : " + receiverParam.toSource());
    JClassLiteral classLiteral = (JClassLiteral)classParam;
    JDeclaredType answerType = injectSingletonAsync(logger, classLiteral, methodCall, ast);

    JDeclaredType receiverType = ast.searchForTypeBySource("xapi.util.api.ReceivesValue");
    for (JMethod method : receiverType.getMethods()) {
      if (method.getName().equals("set")) {

        SourceInfo info = methodCall.getSourceInfo().makeChild(SourceOrigin.UNKNOWN);
        JExpression result;
        result = JGwtCreate.createInstantiationExpression(methodCall.getSourceInfo(), (JClassType)answerType,
          methodCall.getTarget().getEnclosingType());
        if (result == null) {
          ast.error(methodCall, "Rebind result '" + answerType +
            "' has no default (zero argument) constructors");
          return null;
        }
        JMethodCall call = new JMethodCall(info, result, method);
        call.addArg(receiverParam);
        if (logger.isLoggable(Type.TRACE)) {
          TreeLogger branch = logger.branch(Type.TRACE, "Generated asynchronous magic singleton: ");
          for (String str : call.toSource().split("\n")) {
            branch.log(Type.TRACE, str);
          }
        }
        return call;
      }
    }
    throw new InternalCompilerException("Unable to generate asynchronous class injector");
  }

  private static JDeclaredType injectSingletonAsync(TreeLogger logger, JClassLiteral classLiteral,
    JMethodCall methodCall, UnifyAstView ast) throws UnableToCompleteException {

    JDeclaredType type = (JDeclaredType)classLiteral.getRefType();
    String[] names = type.getShortName().split("[$]");
    // TODO: stop stripping the enclosing class name (need to update generators)
    // String reqType = JGwtCreate.nameOf(type);

    String answer = classLiteral.getRefType().getName();
    answer = answer.substring(0, answer.lastIndexOf('.') + 1) + "impl.AsyncFor_" + names[names.length - 1];
    JDeclaredType answerType = null;
    JDeclaredType knownType = ast.getProgram().getFromTypeMap(answer);

    if (knownType != null) {// if the singleton already exists, just use it
      return ast.searchForTypeBySource(answer);
    } else {// we need to generate the singleton on the fly, without updating rebind cache
      StandardGeneratorContext ctx = ast.getRebindPermutationOracle().getGeneratorContext();
      // make sure the requested interface is compiled for the generator
      ast.searchForTypeBySource(type.getName());
      try {
        // our hardcoded class is definitely a generator ;-}
        Class<? extends Generator> generator = AsyncInjectionGenerator.class;
        // (Class<? extends Generator>) Class
        // .forName("xapi.dev.generators.AsyncInjectionGenerator");
        // creates the singleton and provider
        RebindResult rebindResult = ctx.runGeneratorIncrementally(logger, generator, type.getName());
        // commit the generator result, w/out updating rebind cache (to allow GWT.create() rebinds)
        ctx.finish(logger);
        // pull back the LazySingeton provider
        answerType = ast.searchForTypeBySource(rebindResult.getResultTypeName());
        // sanity check
        if (answerType == null) {
          ast.error(methodCall, "Rebind result '" + answer + "' could not be found.  Please be sure that " +
            type.getName() +
            " has a subclass on the classpath which contains @SingletonOverride or @SingletonDefault annotations.");
          throw new UnableToCompleteException();
        }

      } catch (UnableToCompleteException e) {
        logger.log(Type.ERROR, "Error trying to generator provider for " + type.getName() + ". " +
          "\nPlease make sure this class is non-abstract, or that a concrete class on the classpath " +
          "is annotated with @SingletonOverride or @SingletonDefault", e);
        ast.error(methodCall, "Rebind result '" + answer + "' could not be found");
        throw new UnableToCompleteException();
      }
    }
    if (!(answerType instanceof JClassType)) {
      ast.error(methodCall, "Rebind result '" + answer + "' must be a class");
      throw new UnableToCompleteException();
    }
    if (answerType.isAbstract()) {
      ast.error(methodCall, "Rebind result '" + answer + "' cannot be abstract");
      throw new UnableToCompleteException();
    }
    logger.log(Type.TRACE, "Injecting asynchronous singleton for " + type.getName() + " -> " + answerType);
    return answerType;
  }

  /**
   * Replaces a call from {@link X_Inject#singletonAsync(Class, xapi.util.api.ReceivesValue)} by first a)
   * generating an async provider, and then b) sending the value receiver into the async provider as a
   * callback. See the {@link AsyncProxy} class and {@link AsyncInjectionGenerator} for implementation.
   *
   * @param logger - The logger to log to.
   * @param methodCall - The method call we are overwriting
   * @param currentMethod - The encapsulated method itself
   * @param context - The method call context, so you can insert clinits / whatnot
   * @param ast - A view over UnifyAst, exposing our basic needs
   * @return - A JExpression to replace this method call with
   * @throws UnableToCompleteException
   */
  public static JExpression rebindSingletonAndCallback(TreeLogger logger, JMethodCall methodCall,
    JMethod currentMethod, Context context, UnifyAstView ast) throws UnableToCompleteException {
    assert (methodCall.getArgs().size() == 2);
    JExpression classParam = methodCall.getArgs().get(0);
    JExpression receiveParam = methodCall.getArgs().get(1);
    if (!(classParam instanceof JClassLiteral)) {
      ast.error(
        methodCall,
        "Only class literals may be used as arguments to X_Inject.singletonAsync; you sent " +
          classParam.getClass() + " - " + classParam);
      return null;
    }
    logger.log(Type.INFO,
      receiveParam.toString() + " : " + receiveParam.getClass() + " : " + receiveParam.toSource());
    JClassLiteral classLiteral = (JClassLiteral)classParam;
    JClassLiteral receiverLiteral = (JClassLiteral)receiveParam;

    JDeclaredType type = (JDeclaredType)classLiteral.getRefType();
    String[] names = type.getShortName().split("[$]");
    // TODO: stop stripping the enclosing class name (need to update generators)
    // String reqType = JGwtCreate.nameOf(type);

    String answer = receiverLiteral.getRefType().getName();
    answer = answer.substring(0, answer.lastIndexOf('.') + 1) + "impl.AsyncProxy_" + names[names.length - 1];
    JDeclaredType answerType = null;
    JDeclaredType knownType = ast.getProgram().getFromTypeMap(answer);

    // ensure we have a service provider
    JDeclaredType provider = injectSingletonAsync(logger, classLiteral, methodCall, ast);
    StandardGeneratorContext ctx = ast.getRebindPermutationOracle().getGeneratorContext();
    // ctx.finish(logger);

    if (knownType != null) {// if the singleton already exists, just use it
      answerType = ast.searchForTypeBySource(answer);
      // result =
      // JGwtCreate.createInstantiationExpression(methodCall.getSourceInfo(), (JClassType) answerType,
      // currentMethod.getEnclosingType());
    } else {// we need to generate the singleton on the fly, without updating rebind cache
      // make sure the requested interface is compiled for the generator
      logger.log(Type.INFO, "Rebinding singleton w/ callback: " + type + " -> " + provider.getName());
      ast.searchForTypeBySource(type.getName());
      ast.searchForTypeBySource(JGwtCreate.nameOf(provider));
      try {
        InjectionCallbackArtifact rebindResult;
        try {
          logger.log(Type.INFO, "Loading injected result: " + provider.getName());
          rebindResult = AsyncProxyGenerator.setupAsyncCallback(logger, ctx,
            ctx.getTypeOracle().findType(JGwtCreate.nameOf(type)),
            ((JDeclaredType)receiverLiteral.getRefType()));
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
          throw new UnableToCompleteException();
        }
        // creates the singleton and provider
        // RebindResult rebindResult =
        // ctx.runGeneratorIncrementally(logger, generator, type.getName());
        // commit the generator result, w/out updating rebind cache (to allow GWT.create() rebinds)
        ctx.finish(logger);
        // pull back the LazySingeton provider
        answerType = ast.searchForTypeBySource(rebindResult.getAsyncInjectionName());
        // sanity check
        if (answerType == null) {
          ast.error(methodCall, "Rebind result '" + answer + "' could not be found.  Please be sure that " +
            type.getName() +
            " has a subclass on the classpath which contains @SingletonOverride or @SingletonDefault annotations.");
          return null;
        }
      } catch (UnableToCompleteException e) {
        logger.log(Type.ERROR, "Error trying to generator provider for " + type.getName() + ". " +
          "\nPlease make sure this class is non-abstract, or that a concrete class on the classpath " +
          "is annotated with @SingletonOverride or @SingletonDefault", e);
        ast.error(methodCall, "Rebind result '" + answer + "' could not be found");
        return null;
      }
    }

    for (JMethod method : answerType.getMethods()) {
      if (method.getName().equals("go")) {
        // JExpression inst = JGwtCreate.createInstantiationExpression(answerType.getSourceInfo(),
        // (JClassType)answerType, answerType.getEnclosingType());
        return new JMethodCall(method.getSourceInfo(), null, method);
      }
    }
    throw new InternalCompilerException("Did not generate async proxy for " + answerType);
  }

  /**
   * Replaces a call from {@link X_Inject#singletonLazy(Class)} by first a-0) generating a provider which will
   * be synchronous if an async call hasn't already been made, or a-1) generating a provider which will route
   * through the async provider, and return null before inited. then b) returning a simple lazy provider which
   * will poll the actual provider until it is set. If you use the
   * {@link X_Inject#singletonAsync(Class, xapi.util.api.ReceivesValue)} once, you should not use the
   * other two synchronous provider methods, as they may return null if you happen to request them before the
   * code split containing the service is downloaded.
   *
   * @param logger - The logger to log to.
   * @param methodCall - The method call we are overwriting
   * @param currentMethod - The encapsulated method itself
   * @param context - The method call context, so you can insert clinits / whatnot
   * @param ast - A view over UnifyAst, exposing our basic needs
   * @return - A JExpression to replace this method call with
   */

  public static JExpression rebindSingletonLazy(TreeLogger logger, JMethodCall x, JMethod currentMethod,
    Context context, UnifyAstView ast) {
    assert (x.getArgs().size() == 1);
    JExpression arg = x.getArgs().get(0);
    if (!(arg instanceof JClassLiteral)) {
      ast.error(x,
        "Only class literals may be used as arguments to X_Inject.lazySingleton; you sent " + arg.getClass() +
          " - " + arg);
      return null;
    }
    JClassLiteral classLiteral = (JClassLiteral)arg;
    if (!(classLiteral.getRefType() instanceof JDeclaredType)) {
      ast.error(x, "Only classes and interfaces may be used as arguments to X_Inject.singletonLazy()");
      return null;
    }
    return injectLazySingleton(logger, classLiteral, x, currentMethod.getEnclosingType(), ast);
  }

  private static JExpression injectLazySingleton(TreeLogger logger, JClassLiteral classLiteral, JNode x,
    JDeclaredType enclosingType, UnifyAstView ast) {
    JDeclaredType type = (JDeclaredType)classLiteral.getRefType();
    String[] names = type.getShortName().split("[$]");
    // TODO: stop stripping the enclosing class name (need to update generators)
    // String reqType = JGwtCreate.nameOf(type);

    String answer = classLiteral.getRefType().getName();
    answer = answer.substring(0, answer.lastIndexOf('.') + 1) + "impl.SingletonFor_" +
      names[names.length - 1];
    JDeclaredType answerType = null;
    JDeclaredType knownType = ast.getProgram().getFromTypeMap(answer);
    if (knownType != null) {// if the singleton already exists, just use it
      answerType = ast.searchForTypeBySource(answer);
    } else {// we need to generate the singleton on the fly, without updating rebind cache
      StandardGeneratorContext ctx = ast.getRebindPermutationOracle().getGeneratorContext();
      // make sure the requested interface is compiled for the generator
      ast.searchForTypeBySource(type.getName());
      try {
        // our hardcoded class is definitely a generator ;-}
        Class<? extends Generator> generator = SyncInjectionGenerator.class;
        // creates the singleton and provider
        RebindResult result = ctx.runGeneratorIncrementally(logger, generator, type.getName());
        // commit the generator result, w/out updating rebind cache (to allow GWT.create() rebinds)
        ctx.finish(logger);
        // pull back the LazySingeton provider
        logger.log(Type.TRACE, "Loading injected result: " + result.getResultTypeName());
        answerType = ast.searchForTypeBySource(result.getResultTypeName());
        // sanity check
        if (answerType == null) {
          ast.error(x, "Rebind result '" + answer + "' could not be found");
          return null;
        }
      } catch (UnableToCompleteException e) {
        logger.log(Type.ERROR, "Error trying to generator provider for " + type.getName() + ". " +
          "\nPlease make sure this class is non-abstract, or that a concrete class on the classpath " +
          "is annotated with @SingletonOverride or @SingletonDefault", e);
        ast.error(x, "Rebind result '" + answer + "' could not be found");
        return null;
      }
    }
    if (!(answerType instanceof JClassType)) {
      ast.error(x, "Rebind result '" + answer + "' must be a class");
      return null;
    }
    if (answerType.isAbstract()) {
      ast.error(x, "Rebind result '" + answer + "' cannot be abstract");
      return null;
    }
    logger.log(Type.TRACE, "Injecting lazy singleton for " + type.getName() + " -> " + answerType);
    JExpression result = JGwtCreate.createInstantiationExpression(x.getSourceInfo(), (JClassType)answerType,
      answerType);
    if (result == null) {
      ast.error(x, "Rebind result '" + answer + "' has no default (zero argument) constructors");
      return null;
    }
    return result;

  }

  /**
   * Replaces a call from {@link X_Inject#singleton(Class)} by first a-0) generating a provider which will be
   * synchronous if an async call hasn't already been made, or a-1) generating a provider which will route
   * through the async provider, and return null before inited. then b) creates a lazy provider to call into
   * the synchronous provider finally c) calls .get() on the provider and return the value. If you use the
   * {@link X_Inject#singletonAsync(Class, xapi.util.api.ReceivesValue)} once, you should not use the
   * other two synchronous provider methods, as they may return null if you happen to request them before the
   * code split containing the service is downloaded.
   *
   * @param logger - The logger to log to.
   * @param methodCall - The method call we are overwriting
   * @param currentMethod - The encapsulated method itself
   * @param context - The method call context, so you can insert clinits / whatnot
   * @param ast - A view over UnifyAst, exposing our basic needs
   * @return - A JExpression to replace this method call with
   */
  public static JExpression rebindSingleton(TreeLogger logger, JMethodCall x, JMethod currentMethod,
    Context context, UnifyAstView ast) {
    assert (x.getArgs().size() == 1);
    JExpression arg = x.getArgs().get(0);
    if (!(arg instanceof JClassLiteral)) {
      ast.error(x,
        "Only class literals may be used as arguments to X_Inject.lazySingleton; you sent " + arg.getClass() +
          " - " + arg);
      return null;
    }
    JClassLiteral classLiteral = (JClassLiteral)arg;
    return injectSingleton(logger, classLiteral, x, ast);
  }

  private static final Map<JDeclaredType,JExpression> cachedProviders = new IdentityHashMap<JDeclaredType,JExpression>();

  private static JExpression injectSingleton(TreeLogger logger, JClassLiteral classLiteral, JNode x,
    UnifyAstView ast) {
    // check for cached result.

    // inject our provider class
    JDeclaredType type = (JDeclaredType)classLiteral.getRefType();
    if (cachedProviders.containsKey(type)) return cachedProviders.get(type);
    JExpression expr = injectLazySingleton(logger, classLiteral, x, type, ast);
    String[] names = type.getShortName().split("[$]");

    String answer = classLiteral.getRefType().getName();
    answer = answer.substring(0, answer.lastIndexOf('.') + 1) + "impl.SingletonFor_" +
      names[names.length - 1];

    JDeclaredType enclosing = ast.searchForTypeBySource(answer);
    JDeclaredType lazyProvider = ast.searchForTypeBySource(SingletonProvider.class.getName());
    for (JMethod method : lazyProvider.getMethods()) {
      if (method.getName().equals("get")) {
        // Create a new method for each singleton to access the desired provider
        SourceInfo info = null;
        JMethod getSingleton = null;
        String targetName = "singleton" + type.getShortName().replaceAll("[$]", "_");
        for (JMethod existingMethod : enclosing.getMethods()) {
          if (existingMethod.getName().equals(existingMethod)) {
            getSingleton = existingMethod;
            info = getSingleton.getSourceInfo();
            logger.log(Type.TRACE, "Reusing generated method " + getSingleton.toSource());
            break;
          }
        }
        if (getSingleton == null) {

          info = expr.getSourceInfo().makeChild(SourceOrigin.UNKNOWN);
          JMethodBody body = new JMethodBody(info);
          getSingleton = new JMethod(info, targetName, enclosing, type, false, true, true,
            AccessModifier.PRIVATE);
          // insert our generated method into the enclosing type; needed for super dev mode
          enclosing.addMethod(getSingleton);

          // freeze this method
          getSingleton.setBody(body);
          getSingleton.freezeParamTypes();
          getSingleton.setSynthetic();
          info.addCorrelation(info.getCorrelator().by(getSingleton));

          JMethodCall call = new JMethodCall(info, expr, method);
          JReturnStatement value = new JReturnStatement(x.getSourceInfo(), call);
          if (enclosing.getClinitTarget() != null) {
            JDeclaredType clinit = enclosing.getClinitTarget();
            JMethod clinitMethod = clinit.getMethods().get(0);
            assert (JProgram.isClinit(clinitMethod));
            JMethodCall doClinit = new JMethodCall(clinit.getSourceInfo(), null, clinitMethod);
            body.getBlock().addStmt(doClinit.makeStatement());
          }
          body.getBlock().addStmt(value);
          if (logger.isLoggable(Type.DEBUG)) {
            logger = logger.branch(Type.DEBUG, "Generated magic singleton: ");
            for (String str : getSingleton.toSource().split("\n")) {
              logger.branch(Type.DEBUG, str);
            }
          }
        }
        expr = new JMethodCall(info, null, getSingleton);
        cachedProviders.put(type, expr);
        return expr;
      }
    }
    throw new InternalCompilerException("Unable to generate synchronous injected class access");
  }

  public static <T> JExpression rebindNewInstance(TreeLogger logger, JMethodCall x, JMethod currentMethod,
    Context context, final UnifyAstView ast) {
//    JExpression instance = x.getInstance();
//    JMethod target = x.getTarget();
//    JExpressionStatement type = instance.makeStatement();
//    String source = type.toSource();
//    JExpression expr = type.getExpr();
//    JType exprType = expr.getType();
//    JExpressionStatement statement = x.makeStatement();
    return x;
  }

  /**
   * Replaces a call from {@link X_Inject#singleton(Class)} by first a-0) generating a provider which will be
   * synchronous if an async call hasn't already been made, or a-1) generating a provider which will route
   * through the async provider, and return null before inited. then b) creates a lazy provider to call into
   * the synchronous provider finally c) calls .get() on the provider and return the value. If you use the
   * {@link X_Inject#singletonAsync(Class, xapi.util.api.ReceivesValue)} once, you should not use the
   * other two synchronous provider methods, as they may return null if you happen to request them before the
   * code split containing the service is downloaded.
   *
   * @param logger - The logger to log to.
   * @param methodCall - The method call we are overwriting
   * @param currentMethod - The encapsulated method itself
   * @param context - The method call context, so you can insert clinits / whatnot
   * @param ast - A view over UnifyAst, exposing our basic needs
   * @return - A JExpression to replace this method call with
   */

  public static JExpression rebindInstance(TreeLogger logger, JMethodCall x,
      JMethod currentMethod, Context context, final UnifyAstView ast) {
    assert (x.getArgs().size() == 1);
    JExpression arg = x.getArgs().get(0);

    if (!(arg instanceof JClassLiteral)) {
      //uh-oh; our class argument isn't actually a literal.
      //it may be a reference to a magic class,
      //in which case it will have java.lang.Class as a supertype.

      //our search semantics (for methods) are as follows:
      //first check the first arguments of methods for magic class or class lit.
      //if one is found, emit log and use it
      //if not, throw UnableToComplete (TODO a xinject.strict flag to disallow type search)

      if (arg instanceof JVariableRef) {
        JVariableRef local = (JVariableRef)arg;
        JExpression init = local.getTarget().getDeclarationStatement().initializer;
        if (init instanceof JVariableRef) {
          JVariableRef ref = (JVariableRef)init;
          String fromSourceInfo = ref.getSourceInfo().getFileName()
            .replace("gen/","").replace("_MC.java", "")
            .replaceAll("/",".");

          JDeclaredType type;
          //TODO error handling
          type = ast.searchForTypeBySource(fromSourceInfo);
          arg = new JClassLiteral(type.getSourceInfo(), type);
        }
//        arg = ast.getProgram().getClassLiteralField(((JFieldRef)init).getEnclosingType()).getLiteralInitializer();
      } else if (arg instanceof JMethodCall) {
        JMethodCall call = (JMethodCall)arg;
        System.out.println(call.getType());
        arg = call.getArgs().get(0);
      }
      logger.log(Type.ERROR, "Could not generate X_Inject.instance for "+arg.getClass().getName());
    }
    JClassLiteral classLiteral = (JClassLiteral) arg;
    return injectInstance(logger, classLiteral, x, currentMethod, ast);
  }

  private static JExpression injectInstance(TreeLogger logger, JClassLiteral classLiteral,
    JMethodCall x, JMethod method, UnifyAstView ast) {
    JDeclaredType type = (JDeclaredType)classLiteral.getRefType();
    // JExpression expr = injectLazySingleton(logger, classLiteral, x, type, ast);
    // String[] names = type.getShortName().split("[$]");

    String instanceType = classLiteral.getRefType().getName();
    StandardGeneratorContext ctx = ast.getRebindPermutationOracle().getGeneratorContext();
    // make sure the requested interface is compiled for the generator
    ast.searchForTypeBySource(type.getName());
    JDeclaredType injectedInstance;
    try {
      // our hardcoded class is definitely a generator ;-}
      Class<? extends Generator> generator = InstanceInjectionGenerator.class;
      // creates the singleton and provider
      RebindResult result = ctx.runGeneratorIncrementally(logger, generator, type.getName());
      // commit the generator result, w/out updating rebind cache (to allow GWT.create() rebinds)
      ctx.finish(logger);
      // pull back the LazySingeton provider
      logger.log(Type.INFO, "Loading injected result: " + result.getResultTypeName());
      injectedInstance = ast.searchForTypeBySource(result.getResultTypeName());
      // sanity check
      if (injectedInstance == null) {
        ast.error(x, "Rebind result '" + instanceType + "' could not be found");
        throw new InternalCompilerException("Unable to generate instance provider");
      }
    } catch (UnableToCompleteException e) {
      logger.log(Type.ERROR, "Error trying to generate provider for " + type.getName() + ". " +
        "\nPlease make sure this class is non-abstract, or that a concrete class on the classpath " +
        "is annotated with @SingletonOverride or @SingletonDefault", e);
      ast.error(x, "Rebind result '" + instanceType + "' could not be found");
      throw new InternalCompilerException("Unable to generate instance provider");
    }
    if (!(injectedInstance instanceof JClassType)) {
      ast.error(x, "Rebind result '" + instanceType + "' must be a class");
      throw new InternalCompilerException("Unable to generate instance provider");
    }
    if (injectedInstance.isAbstract()) {
      ast.error(x, "Rebind result '" + instanceType + "' cannot be abstract");
      throw new InternalCompilerException("Unable to generate instance provider");
    }
    // now that we have our injected answer,
    // let's run it through normal gwt deferred binding as well

    // copied from UnifyAst#handleGwtCreate
    String reqType = JGwtCreate.nameOf(injectedInstance);
    List<String> answers;
    try {
      answers = Lists.create(ast.getRebindPermutationOracle().getAllPossibleRebindAnswers(logger, reqType));
      ast.getRebindPermutationOracle().getGeneratorContext().finish(logger);
    } catch (UnableToCompleteException e) {
      ast.error(x, "Failed to resolve '" + reqType + "' via deferred binding");
      return null;
    }
    ArrayList<JExpression> instantiationExpressions = new ArrayList<JExpression>(answers.size());
    for (String answer : answers) {
      JDeclaredType answerType = ast.searchForTypeBySource(answer);
      if (answerType == null) {
        ast.error(x, "Rebind result '" + answer + "' could not be found");
        return null;
      }
      if (!(answerType instanceof JClassType)) {
        ast.error(x, "Rebind result '" + answer + "' must be a class");
        return null;
      }
      if (answerType.isAbstract()) {
        ast.error(x, "Rebind result '" + answer + "' cannot be abstract");
        return null;
      }
      JDeclaredType enclosing = injectedInstance.getEnclosingType();
      if (enclosing == null)
        enclosing = method.getEnclosingType();
      JExpression result = JGwtCreate.createInstantiationExpression(x.getSourceInfo(),
        (JClassType)answerType, enclosing);
      if (result == null) {
        ast.error(x, "Rebind result '" + answer + "' has no default (zero argument) constructors");
        return null;
      }
      instantiationExpressions.add(result);
    }
    assert answers.size() == instantiationExpressions.size();
    if (answers.size() == 1) {
      return instantiationExpressions.get(0);
    } else {
      return new JGwtCreate(x.getSourceInfo(), reqType, answers, ast.getProgram().getTypeJavaLangObject(),
        instantiationExpressions);
    }

    // TODO: cache each injection to detect the first time a class is injected,
    // then see if the given injection target is Preloadable,
    // so we can call it's clinit before it is ever accessed,
    // to reduce the bloat of clinits by visiting preloadable methods before
    // any client code can possibly access them (less clinit in runtime code)

  }

}
