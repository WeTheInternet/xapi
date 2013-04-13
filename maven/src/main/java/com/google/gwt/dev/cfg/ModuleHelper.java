package com.google.gwt.dev.cfg;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.CompilerHelper;
import com.google.gwt.dev.javac.GeneratedUnit;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.resource.impl.ResourceOracleImpl;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

public class ModuleHelper {

  public static void addUrlToCompilation(TreeLogger log, ModuleDef mod, URL url){
    log.log(Type.TRACE, "Adding compilation source: "+url.toExternalForm());
    mod.addCompilationUnitArchiveURL(url);
    @SuppressWarnings("unused")
    ResourceLoader loader = ResourceLoaders.forClassLoader(Thread.currentThread());
//   getClass().getClassLoader().
  }
  public static void addModuleToCompilation(TreeLogger log, ModuleDef mod, String module){
    if (!module.contains(".")){
      module = "wetheinter.net."+module;
    }
    log.log(Type.TRACE, "Adding compilation module: "+module);
    mod.addInteritedModule(module);
  }
  /**
   * @throws UnableToCompleteException
   */
  public static CompilationState getCompilationState(final Log log, final AbstractTreeLogger treeLogger, ModuleDef mod) throws UnableToCompleteException {
    mod.getAllSourceFiles();//hack used to invoke private doRefresh() method of ModuleDef
    ResourceOracle oracle = mod.getResourcesOracle();
    Set<Resource> resources = oracle.getResources();
//    mod.getCompilationState(logger)
    Class<? extends ModuleDef> cls = mod.getClass();
    Field oracleField = null;
    ResourceOracleImpl source = null;
    try {
      oracleField = cls.getDeclaredField("lazySourceOracle");
      log.info("Got field "+oracleField);
      oracleField.setAccessible(true);
      log.info("Getting source "+source);
      source = (ResourceOracleImpl) oracleField.get(mod);
      log.info("Got source "+source);
    } catch (SecurityException e) {
      e.printStackTrace();
      log.error("Could not reflect upon lazySourceOracle",e);
    } catch (NoSuchFieldException e) {
      log.error("Could not find field lazySourceOracle",e);
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      log.error("Illegal argument exception in lazySourceOracle",e);
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      log.error("Not allowed to reflect upon lazySourceOracle",e);
      e.printStackTrace();
    }
    if (null!=source){
//      log.info("Got source: "+source+" - "+source.getPathNames());
//      TODO: allow pure java compiles to insert extra path prefixes
//      source.getPathPrefixes().add(new PathPrefix("", null));
      @SuppressWarnings("unused")
      ResourceOracleImpl impl = (ResourceOracleImpl)source;
      resources = source.getResources();
      ResourceOracleImpl.preload(treeLogger, Thread.currentThread().getContextClassLoader());
    }

//    for (Resource resource : oracle.getResources()){
//      if (resource.getPath().contains("com/google"))continue;
//
////      log.info("Resource "+resource.getPath());
//    }
    CompilationStateBuilder builder = CompilationStateBuilder.get();
    File cacheDir = new File("/shared/gen");
    CompilationStateBuilder.init(treeLogger, cacheDir);
    final Collection<GeneratedUnit> units = new ArrayList<GeneratedUnit>();
    final HashSet<GeneratedUnit> errored = new HashSet<GeneratedUnit>();
    try {
//      UnitCache cache = UnitCacheFactory.get(treeLogger, cacheDir);
//
//      CompilationUnit iter = cache.find("wetheinter/net/xapi/TaskManagerJava.java");
////      CompilationUnitArchive f = CompilationUnitArchive.createFromFile(new File("/shared/gen"));
//      if (iter.isError()){
////        units.add(iter.asCachedCompilationUnit());
//        log.warn("Found task manager in cache: "+iter.getTypeName());
//      }
    } catch (Exception e) {
      log.error("Couldn't read unit cache",e);
    }
    CompilationState compilationState =
        builder.doBuildFrom(treeLogger, resources
//            ,
//            new AdditionalTypeProviderDelegate() {
//              @Override
//              public GeneratedUnit doFindAdditionalType(String binaryName) {
////                treeLogger.log(Type.ERROR, "Checking type for "+binaryName);
////                if (binaryName.contains("wetheinter"))
////                log.warn("Checking type for "+binaryName);
//                return null;
//              }
//
//              @Override
//              public boolean doFindAdditionalPackage(String slashedPackageName) {
////                if (slashedPackageName.contains("wetheinter"))
////                log.warn("Checking packages for "+slashedPackageName);
//                return false;
//              }
//            }
            ,false
            );
    CompilerHelper.addCompilationUnits(treeLogger, compilationState, units,true);

    TypeOracle typeOracle = compilationState.getTypeOracle();
    if (typeOracle.findType("java.lang.Object") == null) {
      CompilationProblemReporter.logMissingTypeErrorWithHints(treeLogger, "java.lang.Object",
          compilationState);
    } else {
      TreeLogger branch = treeLogger.branch(TreeLogger.TRACE, "Finding entry point classes", null);
      String[] typeNames = mod.getEntryPointTypeNames();
      for (int i = 0; i < typeNames.length; i++) {
        String typeName = typeNames[i];
        if (typeOracle.findType(typeName) == null) {
          CompilationProblemReporter.logMissingTypeErrorWithHints(branch, typeName,
              compilationState);
        }
      }
    }

    return compilationState;//mod.getCompilationState(treeLogger, false);
  }
}
