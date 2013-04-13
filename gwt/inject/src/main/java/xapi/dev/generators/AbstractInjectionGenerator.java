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
import static java.io.File.separator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import xapi.annotation.inject.SingletonOverride;
import xapi.collect.api.Fifo;
import xapi.dev.util.DefermentWriter;
import xapi.dev.util.GwtInjectionMap;
import xapi.dev.util.InjectionCallbackArtifact;
import xapi.dev.util.InjectionUtils;
import xapi.dev.util.DefermentWriter.DefermentStrategy;
import xapi.gwt.collect.JsFifo;
import xapi.inject.AsyncProxy;
import xapi.inject.impl.SingletonInitializer;
import xapi.inject.impl.SingletonProvider;
import xapi.util.api.ApplyMethod;
import xapi.util.api.ReceivesValue;
import xapi.util.impl.ReceiverAdapter;

import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.rebind.SourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

/**
 * <br>Base class for all generators providing injected singletons.
 * <br>
 * <br>This class is used in both gwt-dev and gwt-prod,
 * <br>with static utility methods being provided to subclasses and other package-level utilities.
 * <br>
 * <br>Important methods:
 * <br>{@link #ensureCallbackClass(TreeLogger, String, String, String, String, GeneratorContext)}
 * <br>- Ensures that there is an asynchronous (code splitting) provider for the injected class
 * <br>{@link #ensureProviderClass(TreeLogger, String, String, String, String, GeneratorContext)}
 * <br>- Ensures that there is a static provider of a given singleton.
 * <br>
 * <br>Note that if an async provider is accessed before there is a synchronous provider,
 * <br>then the synchronous provider will route through the async callback method,
 * <br>to prevent code splitting from being ruined.
 * <br>
 * <br>If your code accesses the synchronous provider before the async provider,
 * <br>the async provider will skip using a code split (as it wouldn't work anyway).
 * <br>To override this behaviour, set {@link SingletonOverride#forceAsync()} to true
 * <br>
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public abstract class AbstractInjectionGenerator extends IncrementalGenerator{

  /**
   * The GwtInjectionMap is a very heavyweight object to initialize,
   * but we need a different one per permutation, so we encapsulate it behind a ThreadLocal.
   *
   * This will allow a single initialization the first time it is used,
   *
   */
  private static final ThreadLocal<GwtInjectionMap> injectionThreadlocal = new ThreadLocal<GwtInjectionMap>();

  protected static GwtInjectionMap getInjectionMap(TreeLogger logger, GeneratorContext ctx){
    //we still have to synchronize because we're passing state through a field.
    GwtInjectionMap map = injectionThreadlocal.get();
    if (map == null)//only synchronize if we have to.
    synchronized (Thread.currentThread()) {//synchro on the lock the threadlocal needs
      //we have to double checked lock because we can't override ThreadLocal#initialValue,
      //as we need the generator context :/
      map = injectionThreadlocal.get();
      if (map != null)
        return map;
      map = new GwtInjectionMap(logger, ctx);
      injectionThreadlocal.set(map);
      //TODO: clean up leaky threadlocals...
    }
    return map;
  }

  @Override
  public long getVersionId() {
    return 0;//update this any time generated class structure changes
  }

  static String packageName = "xapi.inject";

  protected void tryWriteMetaInf(TreeLogger logger, Class<?> cls, JClassType impl, GeneratorContext context) {
      String serviceInterface = cls.getName();
      String serviceImplementation = impl.getQualifiedBinaryName();
      ArrayList<File> outputDirs = new ArrayList<File>();
      PropertyOracle properties = context.getPropertyOracle();
      try {
        File root = new File("");
        if (root.getAbsolutePath().endsWith("war"))
          root = new File(root.getAbsolutePath().replace(separator+"war", "")+separator);
        else
          root = new File(root.getAbsolutePath());
        ConfigurationProperty output = properties.getConfigurationProperty("xinject.output.dir");
        for (String dir : output.getValues()){
          File f = new File(root,dir);
          if (f.isDirectory()){
            outputDirs.add(f);
            f = new File(f,"META-INF"+separator+"services");
            if (!f.exists()){
              if (!f.mkdirs()){
                logger.log(Type.WARN, "Unable to create META-INF" +separator+"services " +
                		" in "+f.getAbsolutePath()+" " +
                    "Please ensure this directory exists, and is writable.");
              }
            }
          }else{
            logger.log(Type.WARN, "Missing xinject output directory: "+f.getAbsolutePath()+". " +
            		"Please set xinject.output.dir to existing source directories; current value: "+output.getValues());
          }
        }
      } catch (BadPropertyValueException e1) {
        logger.log(Type.WARN, "Unexpected propery exception for xinject.output.dir", e1);
      }
      try {
        String prefix = ".." +separator+"WEB-INF" +
        		separator+"classes" +separator+"META-INF" +
        		separator+"singletons"+separator;
        //TODO use a typed artifact to let the linker have a peak if it needs to
        OutputStream res = context.tryCreateResource(logger, prefix+serviceInterface);
        res.write(serviceImplementation.getBytes());
        context.commitResource(logger, res).setVisibility(Visibility.Public);
      } catch (UnableToCompleteException e) {
        logger.log(Type.ERROR, "Couldn't write java services to META-INF/singeltons",e);
      } catch (IOException e) {
        logger.log(Type.ERROR, "Couldn't write java services to META-INF/singletons; please ensure the war folder has full write access and the disk is not full.",e);
        e.printStackTrace();
      }

      logger.log(Type.TRACE, "Saving META-INF/singletons for "+serviceInterface+" -> "+serviceImplementation);
      exports:
        for (File output : outputDirs){
          String knownContent = null;
        //check for existing META-INF/singletons entries, so we don't clobber anything
        File existing = new File(output, "META-INF" + separator+"services"+separator+serviceInterface);
        logger.log(Type.TRACE, "Saving ServiceLoader descriptor to "+existing.getAbsolutePath());
        if (existing.isFile()){
          //need to read in existing manifest, and skip if service already exists
          BufferedReader reader=null;
          FileInputStream in = null;
          try {
            in = new FileInputStream(existing);
            reader = new BufferedReader(new InputStreamReader(in));


            String line;
            StringBuilder b = new StringBuilder();
            while ((line = reader.readLine())!=null){
              if (line.equals(serviceImplementation)){
                //the service impl already exists; skip to next output dir
                //TODO put in a flag to override top permission.
                try {
                  ConfigurationProperty prop = context.getPropertyOracle().getConfigurationProperty("xinject.overwrite.existing");
                  List<String> values = prop.getValues();
                  if (values.size()>0)
                    if (values.get(0).matches("true")){
                      //if we're supposed to overwrite the value, but it's already on top
                      if (b.length()==0){
                        continue exports;//skip the file write
                      }
                      continue;//this erases the existing value so we can put it back over top.
                      //it also skips the breaking-continue below.
                    }
                } catch (BadPropertyValueException e) {
                  logger.log(Type.TRACE, "",e);
                }
                //if we've found the service, and are not allowed to move it to top,
                continue exports;//carry on in the loop above.
              }
              b.append(line+"\n");
            }
            knownContent = b.toString().substring(0, b.length()-1);
          }catch (IOException e) {
            logger.log(Type.WARN, "Received io exception writing META-INF/service for "+existing.getAbsolutePath());
          }finally{
            try{

            if (in!=null)
              in.close();
            if (reader!=null)
              reader.close();
            }catch(IOException e){}
          }
        }
        //save a new java service descriptor
        FileWriter writer = null;
        try {
          try {
            boolean exists = existing.isFile();
            if (!exists){
              if (!existing.createNewFile()){
                logger.log(Type.WARN, "Could not create output file for "+existing);
                continue exports;
              }
            }
            writer = new FileWriter(existing,false);
            if (knownContent==null){
              writer.append(serviceImplementation);
            }else{
              writer.append(serviceImplementation);
              writer.append('\n');
              writer.append(knownContent);
            }
          }finally{
            if (writer != null){
              writer.close();
            }
          }
        } catch (IOException e) {
          logger.log(Type.WARN, "File write exception trying to save META-INF/singletons for "+existing, e);
        }
      }

    }


  protected static boolean isAsyncProvided(TreeLogger logger, String packageName,String simpleSourceName, GeneratorContext context) {
    try{
    JClassType type = context.getTypeOracle().findType(packageName+"."+InjectionUtils.generatedAsyncProviderName(simpleSourceName));
      return type != null;
    }catch(Exception e){
      return false;
    }
  }
  protected static boolean isCallbackInjected(TreeLogger logger, String packageName,String simpleSourceName, GeneratorContext context) {
    try{
      JClassType type = context.getTypeOracle().findType(packageName+"."+InjectionUtils.generatedCallbackName(simpleSourceName));
      return type != null;
    }catch(Exception e){
      return false;
    }
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    injectionThreadlocal.set(null);
  }

  public static InjectionCallbackArtifact ensureAsyncInjected(TreeLogger logger,
      String packageName,String className, String outputClass,
      GeneratorContext ctx) throws UnableToCompleteException {

    GwtInjectionMap gwtInjectionMap = getInjectionMap(logger, ctx);
    InjectionCallbackArtifact artifact = gwtInjectionMap.getOrCreateArtifact(ctx, packageName, className);
    if (artifact.isTargetUnbound()){
      artifact.bindTo(outputClass);
      ensureProviderClass(logger, packageName, artifact.getSimpleName(), artifact.getCanonicalName(), artifact.getBoundTarget(), ctx);
      logger = logger.branch(Type.TRACE, "Creating asynchronous callback for "+artifact.getCanonicalName()+" -> "+outputClass);
      String generatedName = InjectionUtils.generatedAsyncProviderName(artifact.getGeneratedName());
      String implPackage = artifact.getImplementationPackage();
      PrintWriter printWriter = ctx.tryCreate(logger, implPackage, generatedName);
      if (printWriter == null) {
        logger.log(Type.WARN, "Could not create the source writer for "+implPackage+"."+generatedName);
        return artifact;
      }
      artifact.addCallback(implPackage+"."+generatedName+".Deproxy");
      ctx.commitArtifact(logger, artifact);


      SourceFileComposerFactory composer =
          new SourceFileComposerFactory(implPackage, generatedName);
      composer.setPrivacy("public final");

      composer.addImport(com.google.gwt.core.client.GWT.class.getName());
      composer.addImport(RunAsyncCallback.class.getName());
      composer.addImport(Fifo.class.getName());
      composer.addImport(JsFifo.class.getName());
      composer.addImport(AsyncProxy.class.getName());
      composer.addImport(ApplyMethod.class.getName());
      composer.addImport(ReceivesValue.class.getName());
      composer.addImport(ReceiverAdapter.class.getCanonicalName());
      composer.addImport(artifact.getCanonicalName());


      String simpleName = artifact.getSimpleName();
      SourceWriter sw = composer.createSourceWriter(ctx, printWriter);

      sw.println();
      sw.println("static final class Callbacks implements ApplyMethod{");
      sw.indent();
      sw.println("public void apply(Object ... args){");
      sw.println("}");
      sw.outdent();
      sw.println("}");
      sw.println();


      sw.println("static final class Deproxy implements ReceivesValue<"+simpleName+">{");
      sw.indent();
      sw.println("public final void set(final "+simpleName+" value){");
      sw.indentln("getter = new ReceiverAdapter<"+simpleName+">(value);");
      sw.println("}");
      sw.outdent();
      sw.println("}");
      sw.println();


      sw.println("private static final class Proxy implements ReceivesValue<ReceivesValue<"+simpleName+">>{");
      sw.indent();
      sw.println("public final void set(final ReceivesValue<"+simpleName+"> receiver){");
      sw.indent();


      sw.print("GWT.runAsync(");
      sw.println(artifact.getCanonicalName()+".class,new Request(receiver));");

      sw.outdent();
      sw.println("}");
      sw.outdent();
      sw.println("}");
      sw.println();


      sw.println("private static final class Request");
      sw.indent();
      sw.println("extends AsyncProxy<" +simpleName+"> ");
      sw.println("implements RunAsyncCallback{");

      DefermentWriter defer = new DefermentWriter(sw);
      defer.setStrategy(DefermentStrategy.NONE);

      sw.println("private static final Fifo<ReceivesValue<" +simpleName+">> pending =");
      sw.indentln("JsFifo.newFifo();");
      sw.println();

      sw.println("protected Request(ReceivesValue<" +simpleName+"> receiver){");
      sw.indentln("accept(receiver);");
      sw.println("}");
      sw.println();

      sw.println("@Override");
      sw.println("protected final Fifo<ReceivesValue<" +simpleName+">> pending(){");
      sw.indentln("return pending;");
      sw.println("}");
      sw.println();

      sw.println("protected final void dispatch(){");
      sw.indentln("go();");
      sw.println("}");
      sw.println();

      sw.println("public final void onSuccess(){");
      sw.indent();
      defer.printStart();

      sw.println("final "+simpleName+" value = ");
      sw.print(packageName+"."+InjectionUtils.generatedProviderName(simpleName));
      sw.println(".theProvider.get();");
      sw.println();

      sw.print("final ApplyMethod callbacks = GWT.create(");
      sw.print(packageName+".impl."+generatedName+".Callbacks.class");
      sw.println(");");
      sw.println("callbacks.apply(value);");
      sw.println();

      sw.println("apply(value);");
      sw.outdent();
      sw.println("}");
      sw.outdent();
      defer.printFinish();
      sw.println("}");
      sw.println();


      sw.println("private static ReceivesValue<ReceivesValue<"+simpleName+">> getter = new Proxy();");
      sw.println();

      sw.println("static void request(final ReceivesValue<"+simpleName+"> request){");
      sw.indentln("getter.set(request);");
      sw.println("}");
      sw.println();

      sw.println("static void go(){");
      sw.indentln("request(null);");
      sw.println("}");
      sw.println();

      sw.println("private " +generatedName+"(){}");

      sw.commit(logger);

    }else{
      assert artifact.getBoundTarget().equals(outputClass) :
        "The injection target "+artifact.getCanonicalName()+" was bound " +
    		"to "+artifact.getBoundTarget()+", but you tried to bind it again, " +
				"to a different class: "+outputClass;
    }
    return artifact;
  }

  public static boolean ensureProviderClass(TreeLogger logger,
        String packageName, String simpleName0,String canonical,
        String qualifiedSourceName,
        GeneratorContext ctx){
      String simpleName = InjectionUtils.toSourceName(simpleName0);
      String generatedName = InjectionUtils.generatedProviderName(simpleName);
      String cleanedCanonical = InjectionUtils.toSourceName(canonical);
      logger.log(Type.DEBUG, "Creating provider for "+packageName+"."+generatedName);

        PrintWriter printWriter = ctx.tryCreate(logger, packageName, generatedName);
        if (printWriter == null){
          logger.log(Type.TRACE, "Already generated "+generatedName);
          return false;
        }
        logger.log(Type.TRACE, "Newly Generating provider "+generatedName+" <- "+qualifiedSourceName);

        SourceFileComposerFactory composer =
            new SourceFileComposerFactory(packageName, generatedName);
        composer.setSuperclass(SingletonInitializer.class.getName() +
        		                  "<" +simpleName0+">");
        composer.addImport(cleanedCanonical);
        composer.addImport(GWT.class.getName());
        composer.addImport(SingletonProvider.class.getName());

          SourceWriter sw = composer.createSourceWriter(ctx, printWriter);

            sw.println("@Override");
            sw.println("public "+simpleName+" initialValue(){");
            sw.indent();

            sw.print("return GWT.<" +cleanedCanonical+">create(");
            sw.print(InjectionUtils.toSourceName(qualifiedSourceName)+".class");
            sw.println(");");

            sw.outdent();
            sw.println("}");
          sw.println();
          //now, print a static final provider instance
          sw.print("public static final SingletonProvider<");
          sw.print(simpleName0+"> ");
          sw.print("theProvider = ");
          sw.println("new "+generatedName+"();");
          sw.commit(logger);
          return true;
    }
}