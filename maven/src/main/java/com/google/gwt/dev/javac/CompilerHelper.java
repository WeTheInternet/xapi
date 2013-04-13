package com.google.gwt.dev.javac;

import java.lang.reflect.Field;
import java.util.Collection;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.javac.CompilationStateBuilder.CompileMoreLater;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

public class CompilerHelper {

  public static void addCompilationUnits(TreeLogger logger,
      CompilationState compilationState, Collection<GeneratedUnit> units,boolean allowInvalid){
    if (allowInvalid)
      filthyHack(logger, compilationState, units);
    else
      compilationState.addGeneratedCompilationUnits(logger, units);
  }
  private static void filthyHack(TreeLogger logger,
      CompilationState compilationState, Collection<GeneratedUnit> generatedUnits){
    Event generatedUnitsAddEvent = SpeedTracerLogger.start(
        DevModeEventType.COMP_STATE_ADD_GENERATED_UNITS);
    try {
      CompileMoreLater compileMoreLater = getFieldUnsafe(logger,"compileMoreLater",compilationState);
      logger = logger.branch(TreeLogger.DEBUG, "Adding '"
          + generatedUnits.size() + "' new generated units");
      generatedUnitsAddEvent.addData("# new generated units", "" + generatedUnits.size());
      Collection<CompilationUnit> newUnits = compileMoreLater.addGeneratedTypes(
          logger, generatedUnits);
      assimilateUnits(logger, compilationState, newUnits,compileMoreLater);
    } finally {
      generatedUnitsAddEvent.end();
    }
    /*
         for (CompilationUnit unit : units) {
      unitMap.put(unit.getTypeName(), unit);
      for (CompiledClass compiledClass : unit.getCompiledClasses()) {
        classFileMap.put(compiledClass.getInternalName(), compiledClass);
        classFileMapBySource.put(compiledClass.getSourceName(), compiledClass);
      }
    }
    CompilationUnitInvalidator.retainValidUnits(logger, units,
        compileMoreLater.getValidClasses());
    mediator.addNewUnits(logger, units);
     */
  }
  @SuppressWarnings("unchecked")
  private static <T> T getFieldUnsafe(TreeLogger logger, String field, CompilationState compilationState) {
    try{
      Field compiler = compilationState.getClass().getDeclaredField(field);
      compiler.setAccessible(true);
      return (T) compiler.get(compilationState);
    }catch (Exception e) {
      RuntimeException error = new RuntimeException("Could not retrieve private field "+field,e);
      logger.log(Type.ERROR, "Could not retrieve private field "+field,e);
      throw error;
    }
  }
  private static void assimilateUnits(TreeLogger logger, CompilationState compilationState, Collection<CompilationUnit> units, CompileMoreLater compileMoreLater) {
    for (CompilationUnit unit : units) {
      compilationState.unitMap.put(unit.getTypeName(), unit);
      for (CompiledClass compiledClass : unit.getCompiledClasses()) {
        compilationState.classFileMap.put(compiledClass.getInternalName(), compiledClass);
        compilationState.classFileMapBySource.put(compiledClass.getSourceName(), compiledClass);
      }
    }
    logger.log(Type.WARN, "Assimilated "+units);
    //do not remove invalid units
    CompilationUnitInvalidator.retainValidUnits(logger, units,
        compileMoreLater.getValidClasses());
    ((TypeOracleMediatorFromSource)compilationState.getMediator()).addNewUnits(logger, units);
  }
  
}
