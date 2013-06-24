package xapi.mvn;

import org.apache.maven.model.Model;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import xapi.dev.X_Dev;
import xapi.dev.scanner.StringDataResource;

public class MavenAdapterTest {

  private static Model coreTestModel;

  @BeforeClass
  public static void findOwnPom () {
    ClassLoader cl = MavenAdapterTest.class.getClassLoader();
    Iterable<StringDataResource> poms = X_Dev.findPoms(cl);
    for (StringDataResource pom : poms) {
      if (pom.getResourceName().contains("xapi-core-test")) {
        coreTestModel = X_Maven.loadPomString(pom.readAll());
        return;
      }
    }
    Assert.assertNotNull(coreTestModel);
  }
  
  
  @Test
  public void testBytecodeAdapter() throws Exception{
//    MavenProject project = new MavenProject(model);
//    ClassWorld classWorld = new ClassWorld("test", getClass().getClassLoader());
//    ContainerConfiguration configuration = new DefaultContainerConfiguration();
//    configuration.setClassWorld(classWorld);
//    PlexusContainer container = new DefaultPlexusContainer(configuration );
//    MavenExecutionRequest repositorySession = new DefaultMavenExecutionRequest();
//    repositorySession.setPom(model.getPomFile());
//    MavenExecutionResult request = new DefaultMavenExecutionResult();
//    request.setProject(project);
//    ProjectBuildingRequest build = new DefaultProjectBuildingRequest();
//    RepositorySystemSession repoSession = X_Maven.getMavenService().getRepoSession();
//    build.setRepositorySession(repoSession);
//    project.setProjectBuildingRequest(build);
//    project.getParent();
//    MavenSession session = new MavenSession(container, repositorySession, request, project);
//    BytecodeAdapterService adapter = X_Maven.compileScopeAdapter(project, session);
//    IsClass self = adapter.toClass(getClass().getName());
  }
  
}
