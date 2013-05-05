package xapi.mojo.model;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import xapi.annotation.model.IsModel;
import xapi.bytecode.ClassFile;
import xapi.bytecode.annotation.Annotation;
import xapi.dev.X_Dev;
import xapi.dev.scanner.ClasspathResourceMap;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.mojo.api.AbstractXapiMojo;
import xapi.mvn.X_Maven;


/**
 * @threadsafe
 * @goal modelgen
 * @phase process-classes
 * @requiresDependencyResolution compile
 * @author <a href="mailto:james@wetheinter.net">James X. Nelson</a>
 * @version $Id$
 */
public class ModelGeneratorMojo extends AbstractXapiMojo {

  @SuppressWarnings("unchecked")
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    // Start by scanning the class folders
    MavenProject project = getSession().getCurrentProject();
    Build build = project.getBuild();
    ClasspathResourceMap classFolder // The classes we just compiled
      = X_Dev.scanFolder(build.getOutputDirectory(), true, false, false, "");
    ClasspathResourceMap environment // The complete compile-scope classpath
      = X_Maven.scanCompileScope(project, getSession());
    
    // IdentityHashMap is faster, as it uses == for comparison,
    // and we know our ClassFiles come from the same map
    IdentityHashMap<ClassFile, Integer> models =
        new IdentityHashMap<ClassFile, Integer>();

    // While we iterate the result of the classes we just compiled,
    // The full compile classpath scan is running in separate threads
    for (ClassFile model : classFolder.findClassesImplementing(Model.class)) {
      models.put(model, 1);
    }
    for (ClassFile model : classFolder.findClassAnnotatedWith(IsModel.class)) {
      if (!models.containsKey(model))
        models.put(model, 0);
    }
    
    // Build implementations for our models
    for (Iterator<Entry<ClassFile, Integer>> i = models.entrySet().iterator(); i.hasNext();) {
      Entry<ClassFile, Integer> entry = i.next();
      if (entry.getValue().equals(0)) {
        buildPojo(entry.getKey(), environment);
      } else {
        buildModel(entry.getKey(), environment);
      }
      i.remove();
    }
    
    // Run a javac for annotation processing.
  }

  private void buildModel(ClassFile key, ClasspathResourceMap classpath) {
    X_Log.debug("Building model ",key);
    Annotation modelAnno = key.getAnnotation(IsModel.class.getName());
    if (modelAnno != null) {
      X_Log.info(modelAnno);
    }
    
  }

  private void buildPojo(ClassFile key, ClasspathResourceMap classpath) {
    X_Log.debug("Building pojo ",key);
    
  }
  
}
