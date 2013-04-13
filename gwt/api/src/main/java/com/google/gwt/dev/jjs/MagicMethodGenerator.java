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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;

/**
 * This is the expected type signature for magic method injectors;
 * you do not need to explicitly implement this interface,
 * or the suggested method name; however, implementing this interface
 * allows you shorter gwt module xml, since you are not required to supply
 * the full method name.
 *
 *  The full, brutal syntax to add magic method injection is as follows:
  <extend-configuration-property name="gwt.magic.methods"
    value="xapi.inject.X_Inject.singleton(Ljava/lang/Class;)Ljava/lang/Object; *= xapi.dev.inject.MagicMethods::rebindSingleton"/>
 *
 * The first half of the value is the fully qualified jsni signature of the method you wish to overwrite:
 * com.package.Clazz.method(Ljsni/param/Signatures;)Ljsni/return/Type;
 *
 * In the middle, we use " *= ", without quotes, as the delimiter between signatures.
 *
 *
 * Finally, the provider of the AST injection, either a Fully Qualified Class Name
 * that does implement MagicMethodGenerator (or the exact injectMethod it provides),
 * or a fully qualified static method that has the same parameter signature as
 * {@link #injectMagic(TreeLogger, JMethodCall, JMethod, Context, UnifyAstView)}
 *
 * com.package.Clazz   // implements MagicMethodGenerator
 * or
 * com.package.Clazz::staticMethodName  // static method, same param signature as injectMagic
 *
 * This type signature is likely to change,
 * but only to make the api more concise,
 * or to give you access to more runtime data.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface MagicMethodGenerator{
  /**
   * This is the magic method type signature used to replace a given JMethodCall
   * with generated AST expressions (and a StandardGeneratorContext to create new classes).
   *
   * Note that magic methods suffer from the same requirement as GWT.create:
   * if you want to know what a class is at generate time, you must accept a
   * class literal, and not a class reference.
   *
   * If you wish to generate code that accepts class references, you must
   * generate some kind of collections-backed factory method which can accept
   * your runtime class reference and do something useful with it.
   *
   * A generator that accepts ONLY class literals will be capable of inlining
   * all requested code at the call site,
   * rather than monolithic-compiling a bunch of code into a given factory.
   *
   * NOTE THAT MAGIC METHOD INJECTION DOES NOT WORK IN REGULAR GWT DEV MODE,
   * (or any pure java runtime).
   *
   * You are recommended to use super dev mode;
   * but make sure your code works in gwt dev
   * by having the method you are overwriting
   * call into a jre-friendly enviro instead.
   *
   * You may not put code that is uncompilable in gwt behind a magic method call;
   * dev mode still has to choke it down, and if you need to use your generators,
   * you'll have to create a monolithic factory that probably has to scan all
   * types in the type oracle, and potential cause gross code bloat.
   *
   * Given that dev mode performance doesn't affect production mode performance,
   * we only ever start dev mode during testing and to use the java debugger.
   *
   * @param logger - The logger to log to.
   * @param methodCall - The method call we are overwriting
   * @param currentMethod - The encapsulated method itself
   * @param context - The method call context, so you can insert clinits / whatnot
   * DO NOT CALL context.replaceMe() or context.removeMe(); return a value instead.
   * @param ast - A view over UnifyAst, exposing our basic needs
   * @return - A JExpression to replace the method call with
   *
   */
  JExpression injectMagic(TreeLogger logger, JMethodCall methodCall,
      JMethod currentMethod, Context context, UnifyAstView ast)
        throws UnableToCompleteException;
}