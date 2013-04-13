package wetheinter.net.gen;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SyntheticArtifact;

@Shardable
@LinkerOrder(Order.POST)
public class InstantiableLinkerOld extends AbstractLinker{

  
  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context, ArtifactSet artifacts,
      boolean onePermutation) throws UnableToCompleteException {
    if (onePermutation){
      SyntheticArtifact gen = emitString(logger, "stuff", "shared/gen/Injector.java");
      gen.setVisibility(Visibility.LegacyDeploy);
      ArtifactSet set = new ArtifactSet(artifacts);
      set.add(gen);
      artifacts = set;
      System.err.println(gen);
      
    }
    return artifacts;
  }
  
  
  @Override
  public String getDescription() {
    return "Generates rebind helper for java platforms";
  }

}
