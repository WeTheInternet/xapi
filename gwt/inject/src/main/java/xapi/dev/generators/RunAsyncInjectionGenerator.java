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
package xapi.dev.generators;

import java.io.PrintWriter;

import xapi.collect.api.Fifo;
import xapi.dev.util.DefermentWriter;
import xapi.dev.util.InjectionCallbackArtifact;
import xapi.fu.In1;
import xapi.fu.Out1;
import xapi.gwt.collect.JsFifo;
import xapi.util.api.ApplyMethod;
import xapi.util.api.ProvidesValue;
import xapi.util.api.ReceivesValue;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

public class RunAsyncInjectionGenerator extends AbstractInjectionGenerator{

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    Iterable<InjectionCallbackArtifact> artifacts =
        getInjectionMap(logger, context).getArtifacts();
//        ctx.getArtifacts().find(InjectionCallbackArtifact.class);
    if (typeName.endsWith(".Callbacks")){
      typeName = typeName.substring(0,typeName.length()-10);
      logger.log(Type.INFO,""+typeName);
      for (InjectionCallbackArtifact artifact : artifacts){
        if (artifact.getAsyncInjectionName().equals(typeName)){
          //we have our injectable artifact!
          int packageIndex = typeName.lastIndexOf('.');
          String packageName = typeName.substring(0,packageIndex);
          String generatedName = "RunAsync"+typeName.substring(packageIndex+12);

          typeName = packageName+"."+generatedName;
          logger.log(Type.INFO,"WIN "+typeName + " <- "+artifact);

          PrintWriter printWriter = context.tryCreate(logger, packageName, generatedName);
          if (printWriter == null) {
            logger.log(Type.TRACE, "Re-Using existing "+typeName);
            return new RebindResult(RebindMode.USE_EXISTING, typeName);
          }
          ClassSourceFileComposerFactory composer =
              new ClassSourceFileComposerFactory(packageName, generatedName);
          composer.addImplementedInterface(ApplyMethod.class.getName());
          composer.setPrivacy("public final");
          composer.addImport(GWT.class.getName());
          composer.addImport(ApplyMethod.class.getName());
          composer.addImport(In1.class.getName());
          composer.addImport(Out1.class.getName());
          composer.addImport(ProvidesValue.class.getName());
          composer.addImport(ReceivesValue.class.getName());
          composer.addImport(Runnable.class.getName());
          composer.addImport(Fifo.class.getName());
          composer.addImport(JsFifo.class.getName());
          composer.addImport(Timer.class.getName());
          composer.addImport(Scheduler.class.getName());
          composer.addImport(ScheduledCommand.class.getCanonicalName());

          SourceWriter sw = composer.createSourceWriter(printWriter);
          sw.println("private static final Fifo<Object> callbacks;");
          sw.println("static{");
          sw.indent();
          sw.println("callbacks = JsFifo.newFifo();");
          for (String callback : artifact.getCallbacks()){
            sw.println("callbacks.give(GWT.create("+callback+".class));");
          }
          sw.outdent();
          sw.println("}");


          sw.println("@SuppressWarnings({\"unchecked\", \"rawtypes\"})");
          sw.println("public void apply(final Object ... args){");

          DefermentWriter defer = new DefermentWriter(sw);
          defer.printStart();//used to optionally push callbacks into new execution block

          //for now, we are only sending the service object as parameter
          sw.println("  Object service = args[0], callback;");
          sw.println("  while(!callbacks.isEmpty()){");
          sw.indent();
          sw.println("  callback = callbacks.take();");
          sw.println("  if (callback instanceof ReceivesValue){");
          sw.println("    ((ReceivesValue)callback).set(service);");
          sw.println("  }");
          sw.println("  else if (callback instanceof In1){");
          sw.println("    ((In1)callback).in(args);");
          sw.println("  }");
          sw.println("  else if (callback instanceof ApplyMethod){");
          sw.println("    ((ApplyMethod)callback).apply(args);");
          sw.println("  }");
          sw.println("  else if (callback instanceof ProvidesValue){");
          sw.println("    ((ProvidesValue)callback).get().set(service);");
          sw.println("  }");
          sw.println("  else if (callback instanceof Out1){");
          sw.println("    ((Out1)callback).out1().in(service);");
          sw.println("  }");
          sw.println("  else if (callback instanceof Runnable){");
          sw.println("    ((Runnable)callback).run();");
          sw.println("  }");
          sw.println("}");
          sw.outdent();

          defer.printFinish();

          sw.println("}");
//          sw.println("}");
          sw.commit(logger);
          context.commit(logger, printWriter);
          return new RebindResult(RebindMode.USE_ALL_NEW, typeName);
        }
      }
    }
    logger.log(Type.INFO,"No callback class found for "+typeName+"; returning untransformed.");
    return new RebindResult(RebindMode.USE_EXISTING, typeName);
  }

}
