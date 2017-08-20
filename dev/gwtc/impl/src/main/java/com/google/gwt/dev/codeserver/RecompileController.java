package com.google.gwt.dev.codeserver;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import xapi.fu.Out3;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.IsRecompiler;
import xapi.inject.impl.LazyPojo;
import xapi.log.X_Log;

import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.dev.codeserver.CompileStrategy;
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

  private final JobRunner runner;
  private Logger log = Logger.getLogger(getClass().getSimpleName());
  private ResourceLoader loader;

  private LazyPojo<CompiledDirectory> compileDir = resetCompilation();

  private LazyPojo<CompiledDirectory> resetCompilation() {
    return
        new LazyPojo<CompiledDirectory>(){
          @Override
          protected CompiledDirectory initialValue() {
            final Out3<Result, CompileStrategy, ResourceLoader> results = initialize();
            if (results.out2() == CompileStrategy.SKIPPED) {
              loader = results.out3();
            } else {
              loader = recompiler.getResourceLoader();
            }
            final Result res = results.out1();
            final CompileDir dir = res.outputDir;

            if (dir == null) {

            }
            // Try to look up the permutation map from war directory
            File warp = dir.getWarDir();

            File war = new File(warp,getModuleName()+File.separator+"compilation-mappings.txt");
            Map<String, String> permutations = new HashMap<>();
            if (war.exists()) {
              try (
                  BufferedReader read = new BufferedReader(new FileReader(war));
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

            final File symbolDir = dir.findSymbolMapDir(getModuleName());
            CompiledDirectory compiled = new CompiledDirectory()
                .setDeployDir(dir.getDeployDir().getAbsolutePath())
                .setExtraDir(dir.getExtraDir().getAbsolutePath())
                .setGenDir(dir.getGenDir().getAbsolutePath())
                .setLogFile(dir.getLogFile().getAbsolutePath())
                .setWarDir(dir.getWarDir().getAbsolutePath())
                .setWorkDir(dir.getWorkDir().getAbsolutePath())
                .setStrategy(results.out2())
                .setSourceMapDir(symbolDir == null ? null : symbolDir.getAbsolutePath())
                .setUri(getModuleName())
                .setUserAgentMap(permutations)
                ;
            return compiled;
          }
        };
  }

  private final Recompiler recompiler;
  private PrintWriterTreeLogger logger;

  public RecompileController(Recompiler compiler, JobRunner runner) {
    this.recompiler = compiler;
    this.runner = runner;
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
    if (compileDir.isSet()) {
      return compileDir.get();
    }
    return recompile();
  }

  @Override
  public CompiledDirectory recompile(){
    CompiledDirectory toDestroy = null;
    if (compileDir.isSet()) {
      toDestroy = compileDir.get();
    }
    compileDir.reset();
    CompiledDirectory result = null;
    try {
      result = compileDir.get();
      return result;
    } catch (Throwable t) {
      compileDir = resetCompilation();
      if (t.getCause() instanceof NullPointerException) {
        if (t.getCause().getMessage().startsWith("entry")) {
          // A jar we were using was removed (rebuilt most likely)
          // we are going to have to ditch this classloader and restart...
          final CompiledDirectory toReturn = toDestroy;
          compileDir.set(toReturn);
          toDestroy = null;
          X_Log.trace(getClass(), "Resetting GWT recompiler due to broken classpath item", t);
          return toReturn;

        }
      }
      X_Log.warn(getClass(), "Resetting GWT recompiler", t);
      throw t;
    } finally {
      if (toDestroy != null && result != null && result.getStrategy() != CompileStrategy.SKIPPED) {
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

  public ResourceLoader getResourceLoader(){
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
        final Outbox box = new Outbox("id", recompiler, opts, logger);
        final Job job = new Job(box, defaultProps, logger, opts);
        // TODO actually manage the job table sanely
        final JobEventTable table = new JobEventTable();
        job.onSubmitted(table);
        dir = recompiler.recompile(job);
        final CompileStrategy strategy = table.getPublishedEvent(job).getCompileStrategy();
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
    if (compileDir.isSet()) {
      destroy(compileDir.get(), 1);
      compileDir.reset();
    }
  }

}
