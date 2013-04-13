package com.google.gwt.dev;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.ModuleMetricsArtifact;
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.util.CollapsedPropertyKey;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

public class CompileHelper {

  
  public static Precompilation precompile(TreeLogger logger, JJSOptions jjsOptions,
      ModuleDef module, File genDir) {
    PropertyPermutations allPermutations =
        new PropertyPermutations(module.getProperties(), module.getActiveLinkerNames());
    return precompile(logger, jjsOptions, module, 0, allPermutations, genDir);
  }
  
  static Precompilation precompile(TreeLogger logger, JJSOptions jjsOptions, ModuleDef module,
      int permutationBase, PropertyPermutations allPermutations, File genDir) {
    return precompile(logger, jjsOptions, module, permutationBase, allPermutations, genDir,
        ManagementFactory.getRuntimeMXBean().getStartTime());
  }

  static Precompilation precompile(TreeLogger logger, JJSOptions jjsOptions, ModuleDef module,
      int permutationBase, PropertyPermutations allPermutations, File genDir,
      long startTimeMilliseconds) {

    Event precompileEvent = SpeedTracerLogger.start(CompilerEventType.PRECOMPILE);

    // This initializes the Java2D library in a thread so that the main program
    // doesn't block when the library is accessed for the first time.
    new GraphicsInitThread().start();

    ArchivePreloader.preloadArchives(logger, module);

    try {
      CompilationState compilationState =
          module.getCompilationState(logger, !jjsOptions.isStrict());
      if (jjsOptions.isStrict() && compilationState.hasErrors()) {
        abortDueToStrictMode(logger);
      }

      List<String> initialTypeOracleTypes = new ArrayList<String>();
      if (jjsOptions.isCompilerMetricsEnabled()) {
        for (JClassType type : compilationState.getTypeOracle().getTypes()) {
          initialTypeOracleTypes.add(type.getPackage().getName() + "." + type.getName());
        }
      }

      // Track information about the module load including initial type
      // oracle build for diagnostic purposes.
      long moduleLoadFinished = System.currentTimeMillis();

      String[] declEntryPts = module.getEntryPointTypeNames();
      if (declEntryPts.length == 0) {
        logger.log(TreeLogger.ERROR, "Module has no entry points defined", null);
        throw new UnableToCompleteException();
      }

      ArtifactSet generatedArtifacts = new ArtifactSet();
      DistillerRebindPermutationOracle rpo =
          new DistillerRebindPermutationOracle(module, compilationState, generatedArtifacts,
              allPermutations, genDir);
      // Allow GC later.
      compilationState = null;
      PrecompilationMetricsArtifact precompilationMetrics =
          jjsOptions.isCompilerMetricsEnabled()
              ? new PrecompilationMetricsArtifact(permutationBase) : null;
      UnifiedAst unifiedAst =
          Precompile.getCompiler(module).precompile(logger, module, rpo, declEntryPts, null, jjsOptions,
              rpo.getPermuationCount() == 1, precompilationMetrics);

      if (jjsOptions.isCompilerMetricsEnabled()) {
        ModuleMetricsArtifact moduleMetrics = new ModuleMetricsArtifact();
        moduleMetrics.setSourceFiles(module.getAllSourceFiles());
        // The initial type list has to be gathered before the call to
        // precompile().
        moduleMetrics.setInitialTypes(initialTypeOracleTypes);
        // The elapsed time in ModuleMetricsArtifact represents time
        // which could be done once for all permutations.
        moduleMetrics.setElapsedMilliseconds(moduleLoadFinished - startTimeMilliseconds);
        unifiedAst.setModuleMetrics(moduleMetrics);
      }

      // Merge all identical permutations together.
      List<Permutation> permutations =
          new ArrayList<Permutation>(Arrays.asList(rpo.getPermutations()));

      mergeCollapsedPermutations(permutations);

      // Sort the permutations by an ordered key to ensure determinism.
      SortedMap<RebindAnswersPermutationKey, Permutation> merged =
          new TreeMap<RebindAnswersPermutationKey, Permutation>();
      SortedSet<String> liveRebindRequests = unifiedAst.getRebindRequests();
      for (Permutation permutation : permutations) {
        // Construct a key for the live rebind answers.
        RebindAnswersPermutationKey key =
            new RebindAnswersPermutationKey(permutation, liveRebindRequests);
        if (merged.containsKey(key)) {
          Permutation existing = merged.get(key);
          existing.mergeFrom(permutation, liveRebindRequests);
        } else {
          merged.put(key, permutation);
        }
      }
      if (jjsOptions.isCompilerMetricsEnabled()) {
        int[] ids = new int[allPermutations.size()];
        for (int i = 0; i < allPermutations.size(); i++) {
          ids[i] = permutationBase + i;
        }
        precompilationMetrics.setPermuationIds(ids);
        // TODO(zundel): Right now this double counts module load and
        // precompile time. It correctly counts the amount of time spent
        // in this process. The elapsed time in ModuleMetricsArtifact
        // represents time which could be done once for all permutations.
        precompilationMetrics.setElapsedMilliseconds(System.currentTimeMillis()
            - startTimeMilliseconds);
        unifiedAst.setPrecompilationMetrics(precompilationMetrics);
      }
      return new Precompilation(unifiedAst, merged.values(), permutationBase, generatedArtifacts);
    } catch (UnableToCompleteException e) {
      e.printStackTrace();
      // We intentionally don't pass in the exception here since the real
      // cause has been logged.
      return null;
    } finally {
      precompileEvent.end();
    }
  }

  private static void abortDueToStrictMode(TreeLogger logger) throws UnableToCompleteException {
    logger.log(TreeLogger.ERROR, "Aborting compile due to errors in some input files");
    throw new UnableToCompleteException();
  }

  /**
   * This merges Permutations that can be considered equivalent by considering
   * their collapsed properties. The list passed into this method may have
   * elements removed from it.
   */
  private static void mergeCollapsedPermutations(List<Permutation> permutations) {
    if (permutations.size() < 2) {
      return;
    }

    // See the doc for CollapsedPropertyKey
    SortedMap<CollapsedPropertyKey, List<Permutation>> mergedByCollapsedProperties =
        new TreeMap<CollapsedPropertyKey, List<Permutation>>();

    // This loop creates the equivalence sets
    for (Iterator<Permutation> it = permutations.iterator(); it.hasNext();) {
      Permutation entry = it.next();
      CollapsedPropertyKey key = new CollapsedPropertyKey(entry);

      List<Permutation> equivalenceSet = mergedByCollapsedProperties.get(key);
      if (equivalenceSet == null) {
        equivalenceSet = Lists.create();
      } else {
        // Mutate list
        it.remove();
        equivalenceSet = Lists.add(equivalenceSet, entry);
      }
      mergedByCollapsedProperties.put(key, equivalenceSet);
    }

    // This loop merges the Permutations together
    for (Map.Entry<CollapsedPropertyKey, List<Permutation>> entry : mergedByCollapsedProperties
        .entrySet()) {
      Permutation mergeInto = entry.getKey().getPermutation();

      /*
       * Merge the deferred-binding properties once we no longer need the
       * PropertyOracle data from the extra permutations.
       */
      for (Permutation mergeFrom : entry.getValue()) {
        mergeInto.mergeRebindsFromCollapsed(mergeFrom);
      }
    }

    // Renumber the Permutations
    for (int i = 0, j = permutations.size(); i < j; i++) {
      permutations.set(i, new Permutation(i, permutations.get(i)));
    }
  }


  
}
