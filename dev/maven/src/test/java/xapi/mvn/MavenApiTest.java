package xapi.mvn;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.aether.resolution.ArtifactResult;

import xapi.io.X_IO;

public class MavenApiTest {

  
  @Test
  public void testArtifactLoad() {
    if (X_IO.isOffline())
      return;
    ArtifactResult artifact = X_Maven.loadArtifact("net.wetheinter", "xapi-template", "0.2");
    Assert.assertTrue(artifact.isResolved());
  }
  
}
