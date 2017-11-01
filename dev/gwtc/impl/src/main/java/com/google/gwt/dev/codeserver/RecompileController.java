package com.google.gwt.dev.codeserver;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import xapi.dev.gwtc.api.GwtcJobState;
import xapi.fu.Do;
import xapi.fu.In2;
import xapi.fu.Lazy;
import xapi.fu.Out3;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.IsRecompiler;
import xapi.inject.impl.LazyPojo;
import xapi.log.X_Log;
import xapi.util.X_Util;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.dev.codeserver.CompileStrategy;
import com.google.gwt.dev.codeserver.JobEvent.Status;
import com.google.gwt.dev.javac.StaleJarError;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecompileController implements IsRecompiler {

  private final RecompileRunner runner;
  private Logger log = Logger.getLogger(getClass().getSimpleName());
  private ResourceLoader loader;

  private Lazy<CompiledDirectory> compileDir = resetCompilation();
  private Job job;

  private final Recompiler recompiler;
  private PrintWriterTreeLogger logger;
  private CompileDir lastCompile;

  public RecompileController(Recompiler compiler, RecompileRunner runner) {
    this.recompiler = compiler;
    this.runner = runner;
  }

  private Lazy<CompiledDirectory> resetCompilation() {
    return
        Lazy.deferred1(()->{
          final Out3<Result, CompileStrategy, ResourceLoader> results = initialize();
          if (results.out2() == CompileStrategy.SKIPPED) {
            loader = results.out3();
          } else {
            loader = recompiler.getResourceLoader();
          }
          final Result res = results.out1();
          lastCompile = res.outputDir;

          // Try to look up the permutation map from war directory
          File war = lastCompile.getWarDir();

          File mappings = new File(war,getModuleName()+File.separator+"compilation-mappings.txt");
          Map<String, String> permutations = new HashMap<>();
          if (mappings.exists()) {
            try (
                BufferedReader read = new BufferedReader(new FileReader(mappings));
            ){
              String line, strongName=null;
              while ((line = read.readLine())!=null) {
                if (line.startsWith("user.agent")) {
                  if (strongName != null) {
                    if (line.contains("safari")) {
                      permutations.put("safari", strongName);
                    }
                    else if (line.contains("gecko1_8")) {
                      permutations.put("gecko1_8", strongName);
                    }
                    else if (line.contains("gecko")) {
                      permutations.put("gecko", strongName);
                    }
                    else if (line.contains("ie6")) {
                      permutations.put("ie6", strongName);
                    }
                    else if (line.contains("ie8")) {
                      permutations.put("ie8", strongName);
                    }
                    else if (line.contains("ie9")) {
                      permutations.put("ie9", strongName);
                    }
                    else if (line.contains("opera")) {
                      permutations.put("opera", strongName);
                    }
                  }
                } else {
                  int ind = line.indexOf(".cache.js");
                  if (ind > -1) {
                    strongName = line.substring(0, ind);
                  }
                }
              }
            }catch (Exception e) {
              e.printStackTrace();
            }
          }

          final File symbolDir = lastCompile.findSymbolMapDir(getModuleName());
          CompiledDirectory compiled = new CompiledDirectory()
              .setDeployDir(lastCompile.getDeployDir().getAbsolutePath())
              .setExtraDir(lastCompile.getExtraDir().getAbsolutePath())
              .setGenDir(lastCompile.getGenDir().getAbsolutePath())
              .setLogFile(lastCompile.getLogFile().getAbsolutePath())
              .setWarDir(lastCompile.getWarDir().getAbsolutePath())
              .setWorkDir(lastCompile.getWorkDir().getAbsolutePath())
              .setStrategy(results.out2())
              .setSourceMapDir(symbolDir == null ? null : symbolDir.getAbsolutePath())
              .setUri(getModuleName())
              .setUserAgentMap(permutations)
              ;
          return compiled;
        });
  }

  public byte[] recompileSerialized(){
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    try (
        ObjectOutputStream out = new ObjectOutputStream(bout)
    ) {
      final CompiledDirectory compiled = recompile();
      out.writeObject(compiled);
      return bout.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public CompiledDirectory getOrCompile() {
    if (compileDir.isResolved()) {
      final RecompileRunner runningJob = getRunner();
      while (getRunner().isRunning()) {
        try {
          runningJob.waitForStateChange();
        } catch (InterruptedException e) {
          throw X_Util.rethrow(e);
        }
      }
      return compileDir.out1();
    }
    return recompile();
  }

  @Override
  public CompiledDirectory recompile(){
    CompiledDirectory toDestroy = null;
    if (compileDir.isResolved()) {
      toDestroy = compileDir.out1();
      // check for changes...
      compileDir = resetCompilation();
    }
    CompiledDirectory result = null;
    try {
      result = compileDir.out1();
      return result;
    } catch (Throwable t) {
      compileDir = resetCompilation();
      if (t.getCause() instanceof NullPointerException) {
        if (t.getCause().getMessage().startsWith("entry")) {
          // A jar we were using was removed (rebuilt most likely)
          // we are going to have to ditch this classloader and restart...
          final CompiledDirectory toReturn = toDestroy;
          compileDir = Lazy.immutable1(toReturn);
          toDestroy = null;
          X_Log.trace(getClass(), "Resetting GWT recompiler due to broken classpath item", t);
          return toReturn;

        }
      }
      X_Log.warn(getClass(), "Resetting GWT recompiler", t);
      throw t;
    } finally {
      if (toDestroy != null && result != null && toDestroy != result && result.getStrategy() != CompileStrategy.SKIPPED) {
        destroy(toDestroy, 10000);
      }
    }
  }

  /**
   * Kill previous compile in a separate thread, to avoid wasting wall-time.

   * @param dir - The draft compile to destroy.
   */
  private void destroy(final CompiledDirectory dir, final int delay) {
    final File deployDir = new File(dir.getDeployDir());
    final File extraDir = new File(dir.getExtraDir());
    final File genDir = new File(dir.getGenDir());
    final File logDir = new File(dir.getLogFile());
    final File mapDir = new File(dir.getSourceMapDir());
    final File warDir = new File(dir.getWarDir());
    final File workDir = new File(dir.getWorkDir());
    Thread cleanup = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // wait thirty seconds before taking out the old compile
          Thread.sleep(delay);
          destroy(deployDir);
          int perDir = delay/30+1;
          Thread.sleep(perDir);
          destroy(extraDir);
          Thread.sleep(perDir);
          destroy(genDir);
          Thread.sleep(perDir);
          destroy(mapDir);
          Thread.sleep(perDir);
          destroy(warDir);
          Thread.sleep(perDir);
          destroy(workDir);
          Thread.sleep(perDir);
        } catch (InterruptedException e) {Thread.currentThread().interrupt();}
      }

      private void destroy(File file) {
        try {
          FileUtils.deleteDirectory(file);
        } catch (IOException e) {
          System.err.println("Error destroying "+file+" ; ");
          System.err.println("Check if directory is empty? "+file.exists());
        }
      }
    });
    cleanup.start();
  }

  public String getModuleName(){
    return recompiler.getOutputModuleName();
  }

  protected ResourceLoader getResourceLoader(){
    return loader == null ? recompiler.getResourceLoader() : loader;
  }

  public URL getResource(String name) {
    return getResourceLoader().getResource(name);
  }

  protected Out3<Result, CompileStrategy, ResourceLoader> initialize(){
    final ResourceLoader currentLoader = recompiler.getResourceLoader();
    Map<String, String> defaultProps = new HashMap<String, String>();
    defaultProps.put("user.agent", "safari,gecko1_8");
    defaultProps.put("locale", "en");
    defaultProps.put("compiler.useSourceMaps", "true");
    Result dir = null;
      try{
        final Options opts = recompiler.getOptions();
        logger = new PrintWriterTreeLogger();
        logger.setMaxDetail(opts.getLogLevel());
        String moduleName = runner.getModuleName();

        boolean firstRun = job == null;
        final Outbox box;
        if (firstRun) {
          // Creating an outbox will immediately compile if the opts didn't disable precompile
          box = new Outbox(moduleName, recompiler, opts, logger);
        } else {
          // reuse old outbox...
          box = job.getOutbox();
        }
        job = new Job(box, defaultProps, logger, opts);

        if (!firstRun || opts.getNoPrecompile()) {
          // If the recompiler wasn't configured for precompile,
          // or if we are on second+ run, we have to manually run the recompile...
          runner.submit(job);
        }
        dir = job.waitForResult();
        final CompileStrategy strategy = runner.getTable().getPublishedEvent(job).getCompileStrategy();
        return Out3.out3(dir, strategy, currentLoader);
      }catch (Throwable e) {
        if (e instanceof StaleJarError) {
          compileDir = resetCompilation();
          log.log(Level.WARNING, "Stale jars detected; reinitializing GWT compiler");
        }
        e.printStackTrace();
        log.log(Level.SEVERE, "Unable to compile module.", e);
        throw new RuntimeException(e);
      }
    }

  public void cleanup() {
    if (compileDir.isResolved()) {
      if (!job.waitForResult().isOk()) {
        final CompiledDirectory worker = compileDir.out1();
        destroy(worker, 1);
        compileDir = resetCompilation();
      }
    }
  }

  public RecompileRunner getRunner() {
    return runner;
  }

  @Override
  public void checkFreshness(Do ifFresh, Do ifStale) {
      if (lastCompile == null || job == null) {
          ifStale.done();
          return;
      }
      recompiler.checkCompileFreshness(ifFresh.toRunnable(), ifStale.toRunnable());

  }

}
