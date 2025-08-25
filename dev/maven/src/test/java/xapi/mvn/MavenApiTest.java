package xapi.mvn;

import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.Assert;
import org.junit.Test;

import xapi.io.X_IO;

public class MavenApiTest {


  @Test
  public void testArtifactLoad() {
    if (X_IO.isOffline()) {
      return;
    }
    ArtifactResult artifact = X_Maven.loadArtifact("net.wti.core", "xapi-fu", "0.5.1");
    Assert.assertTrue(artifact.isResolved());
    Assert.assertTrue(artifact.getArtifact().getFile().exists());
  }

}
