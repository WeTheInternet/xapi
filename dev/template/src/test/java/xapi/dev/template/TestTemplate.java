package xapi.dev.template;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Random;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.Assert;
import org.junit.Test;

import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

/**
 * A fairly simple test of our templating system;
 * this class uses itself as the generator for the template we are testing.
 *
 * The template we are using is:

//@repackaged(xapi.generated)//
package xapi.template;

//@imports()//
import java.util.Date;

//@classDefinition(public class Success)//
abstract class Success{

//@generateWith(xapi.dev.template.TestTemplate)//

  public static void main(String[] args){
    new Success().injected(args);
  }

//injected() //
abstract void injected(String ... args);

//@skipline(1)//
stuff to not compile! /
}

 *
 * This template, if successfully applied, will produce the source file:
 *
package xapi.generated;

import java.util.Date;
import org.junit.Assert;

public class Success {

  public static void main(String[] args){
    new Success().injected(args);
  }

  private void injected(String ... args){
    Assert.assertEquals(args[0], "success");
  }

}
 *
 *
 * @author "James X. Nelson (james@xapi)"
 *
 */
public class TestTemplate implements TemplateClassGenerator{


  @Override
	public void initialize(TreeLogger logger, TemplateGeneratorOptions options) {
	  logger.log(Type.INFO, "Initializing "+getClass().getName());
	}

	public void injected(TreeLogger logger, SourceBuilder<?> context, String payload){
		context
		  .setLinesToSkip(1)
		  .getImports().addImport(Assert.class.getName());
		context.getBuffer()
		  .indent()
		  .println("private void injected(String ... args){")
		  .indentln("Assert.assertEquals(args[0], \"success\");")
		  .println("}")
		  .outdent();
	}

	@Test
	public void testSimpleGeneration() throws Exception{
	  //create a temp classpath
	  File tmp = new File(System.getProperty("java.io.tmpdir","/tmp"));
	  File cp = new File(tmp, "testCompile-"+Long.toHexString(new Random().nextLong()));
	  cp.mkdirs();
	  cp.deleteOnExit();

	  //apply template
	  TemplateToJava.main(new String[] {
	    "-template",getClass().getClassLoader().getResource("xapi/template/Success.x")
	      .toExternalForm().replace("file:", ""),
	    "-output",cp.getAbsolutePath()
	  });

	  //compile the file
	  final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	  String junit = Assert.class.getProtectionDomain().getCodeSource()
	    .getLocation().toExternalForm().substring(5);//removes file: prefix

	  //our argument list
	  String[] args = new String[] {
	    "-cp", junit, "-proc:none",
	    "-d", cp.getAbsolutePath()
	    //let file normalize our / slashes
	    ,new File(cp.getAbsolutePath(),"xapi/generated/Success.java")
	      .getPath()
	  };
	  //run the compile
	  int result = compiler.run(System.in, System.out, System.err, args);
	  if (result != 0)
	    throw new RuntimeException("Java compile failed w/ status code "+result);
	  //create a thread with the required classes on the classpath
	  final URLClassLoader cl = new URLClassLoader(new URL[] {
	    new URL("file:"+junit+File.separator),
	    new URL("file:"+cp.getAbsolutePath()+File.separator)
	  });
	  Thread t = new Thread() {
	    @Override
	    public void run() {
	      //run the generated class reflectively
	      try {
  	      Class<?> cls = cl.loadClass("xapi.generated.Success");
  	      Method method = cls.getMethod("main", String[].class);
  	      method.invoke(null, (Object)(new String[] {"success"}));
	      } catch (Exception e) {
	        throw new RuntimeException(e);
	      }
	    }
	  };
	  t.setContextClassLoader(cl);
	  t.run();
	}

}
