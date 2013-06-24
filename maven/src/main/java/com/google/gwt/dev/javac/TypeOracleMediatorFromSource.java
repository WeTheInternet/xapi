/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.javac;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.asm.Type;

/**
 * Creates a type oracle from JDT compiled classes.
 */
public class TypeOracleMediatorFromSource extends TypeOracleMediator {

  private Collection<CompilationUnit> units;

  /**
   * Adds new units to an existing TypeOracle.
   * 
   * @param logger logger to use
   * @param units collection of compilation units to process
   */
  public void addNewUnits(TreeLogger logger, Collection<CompilationUnit> units) {
    Collection<TypeData> classDataList = new ArrayList<TypeData>();
    this.units = units;
    // Create method args data for types to add
    MethodArgNamesLookup argsLookup = new MethodArgNamesLookup();
    for (CompilationUnit unit : units) {
      argsLookup.mergeFrom(unit.getMethodArgs());
    }

    // Create list including byte code for each type to add
    for (CompilationUnit unit : units) {
      Collection<CompiledClass> compiledClasses = unit.getCompiledClasses();
      for (CompiledClass compiledClass : compiledClasses) {
        classDataList.add(compiledClass.getTypeData());
      }
    }

    // Add the new types to the type oracle build in progress.
    addNewTypes(logger, classDataList, argsLookup);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected Class<?> rescueMissingType(TreeLogger logger, Type valueType) {
    CompiledClass clazz = null;
    for (CompilationUnit unit : units) {
      if (unit.getTypeName().equals(valueType.getClassName())) {
        for (CompiledClass cls : unit.getCompiledClasses()) {
          logger.log(TreeLogger.WARN, valueType.toString());
          clazz = cls;
        }
      }
    }
    if (clazz != null) {
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	DiagnosticCollector diagnosticsCollector = new DiagnosticCollector();
      ClassFileManager fileManager =
          new ClassFileManager(compiler.getStandardFileManager(diagnosticsCollector, null, null));
      try {
        JavaObjectFromString javaObjectFromString =
            new JavaObjectFromString(Shared.getShortName(valueType.getClassName()), clazz.getUnit()
                .getResourceLocation());
        Iterable fileObjects = Arrays.asList(javaObjectFromString);
        CompilationTask task =
            compiler.getTask(null, fileManager, diagnosticsCollector, null, null, fileObjects);
        Boolean result = task.call();
        List<Diagnostic> diagnostics = diagnosticsCollector.getDiagnostics();
        for (Diagnostic d : diagnostics) {
          System.out.println(d.getMessage(null) + " - " + d.getCode() + " : ");
          // Print all the information here.
        }
        if (result == true) {
          System.out.println("Compilation has succeeded");
          try {

            return fileManager.getClassLoader(null).loadClass(valueType.getClassName());
          } catch (Exception e) {
            System.out.println("Trying platform classpath");
            return fileManager.getClassLoader(StandardLocation.CLASS_PATH).loadClass(
                valueType.getClassName());
          }
        } else {
          System.out.println("Compilation fails.");
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    while (cl != null) {
      logger.log(TreeLogger.WARN, cl.getClass().getName() + ": " + cl + " - ");
      if (cl.getParent() == cl)
        cl = null;
      else
        cl = cl.getParent();
    }
    return null;
  }

  class JavaObjectFromString extends SimpleJavaFileObject {
    private String location;
    protected final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    @Override
    public OutputStream openOutputStream() throws IOException {
      return bos;
    }

    public byte[] getBytes() {
      return bos.toByteArray();
    }

    public JavaObjectFromString(String className, String location) throws Exception {
      super(new URI(location.replaceFirst("file:", "")), Kind.SOURCE);
      this.location = location.replaceFirst("file:", "");
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      try {
        // new FileInputStream(location).getChannel().
        String file = Shared.readContent(new FileInputStream(location));
        System.out.println("FILE: " + file);
        System.out.println("uri" + toUri());
        return file;
      } catch (IOException e) {
        e.printStackTrace();
      }
      return location;
    }
  }

  @SuppressWarnings("rawtypes")
public class ClassFileManager extends ForwardingJavaFileManager {
    /**
     * Instance of JavaClassObject that will store the compiled bytecode of our class
     */
    private JavaObjectFromString jclassObject;

    /**
     * Will initialize the manager with the specified standard java file manager
     * 
     * @param standardManger
     */
    @SuppressWarnings("unchecked")
	public ClassFileManager(StandardJavaFileManager standardManager) {
      super(standardManager);
    }

    /**
     * Will be used by us to get the class loader for our compiled class. It creates an anonymous
     * class extending the SecureClassLoader which uses the byte code created by the compiler and
     * stored in the JavaClassObject, and returns the Class for it
     */
    @Override
    public ClassLoader getClassLoader(Location location) {
      return new SecureClassLoader() {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
          byte[] b = jclassObject.getBytes();
          return super.defineClass(name, jclassObject.getBytes(), 0, b.length);
        }
      };
    }
    /**
     * Gives the compiler an instance of the JavaClassObject so that the compiler can write the byte
     * code into it.
     */
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind,
        FileObject sibling) throws IOException {
      try {
        jclassObject = new JavaObjectFromString(className, className);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return jclassObject;
    }
  }

}
