package wetheinter.net.mojo.model;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;



/**
 * @goal modelgen
 * @phase generate-sources
 * @requiresDependencyResolution compile
 * @author <a href="mailto:james@wetheinter.net">James X. Nelson</a>
 * @version $Id$
 */
public class ModelMojo extends AbstractXapiMojo{


  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (getPlatform().toLowerCase().contains("gwt")) {

    } else {
      // write jre META-INF values
    }


  }


}
