/**
 *
 */
package xapi.dev.gwt.linker;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.Transferable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@Transferable
public class StrongNameArtifact extends Artifact<StrongNameArtifact> {

  private static final long serialVersionUID = 5776975700001190825L;
  private final Set<String> strongNames;

  protected StrongNameArtifact(final Class<? extends Linker> linker) {
    super(linker);
    strongNames = new LinkedHashSet<>();
  }

  public void addStrongName(final String strongName) {
    strongNames.add(strongName);
  }

  public Set<String> getStrongNames() {
    return Collections.unmodifiableSet(strongNames);
  }

  @Override
  public int hashCode() {
    return 0;// There is only on StrongNameArtifact for the whole application
  }

  @Override
  protected int compareToComparableArtifact(final StrongNameArtifact o) {
    return 0;// There is only one StrongNameArtifact for the whole application
  }

  @Override
  protected Class<StrongNameArtifact> getComparableArtifactType() {
    return StrongNameArtifact.class;
  }

}
