/**
 *
 */
package xapi.dev.gwt.linker;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;

import java.util.SortedSet;

/**
 * This linker is used to accummulate all {@link CompilationResult} strongNames
 * so that they can be accessed easily by other linkers.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@LinkerOrder(Order.PRE)
 @Shardable
public class StrongNameLinker extends Linker {

  @Override
  public ArtifactSet link(final TreeLogger logger, final LinkerContext context, ArtifactSet artifacts, final boolean onePermutation)
      throws UnableToCompleteException {
    if (onePermutation) {
      final SortedSet<StrongNameArtifact> existing = artifacts.find(StrongNameArtifact.class);
      StrongNameArtifact artifact;
      if (existing.isEmpty()) {
        artifact = new StrongNameArtifact(getClass());
        artifacts = new ArtifactSet(artifacts);
        artifacts.add(artifact);
      } else {
        artifact = existing.first();
      }
      for (final CompilationResult compilation : artifacts.find(CompilationResult.class)) {
        // pass a writable set so that other stages can use this set for temporary storage
        artifact.addStrongName(compilation.getStrongName());
      }
    }
    return artifacts;
  }

  @Override
  public String getDescription() {
    return "Accumulate permutation strongNames";
  }

}
