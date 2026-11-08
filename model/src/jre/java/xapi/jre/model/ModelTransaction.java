package xapi.jre.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable, schema-identified operation. The store computes its fingerprint internally. */
public final class ModelTransaction {
  private final String protocol;
  private final String manifest;
  private final String actorScope;
  private final String operationId;
  private final List<ModelMutation> mutations;

  public ModelTransaction(final String protocol, final String manifest, final String actorScope,
                          final String operationId, final List<ModelMutation> mutations) {
    this.protocol = ModelStoreKey.text(protocol, 256, "protocol");
    this.manifest = ModelStoreKey.text(manifest, 256, "manifest");
    this.actorScope = ModelStoreKey.text(actorScope, 1024, "actor scope");
    this.operationId = ModelStoreKey.text(operationId, 256, "operation ID");
    Objects.requireNonNull(mutations, "mutations");
    if (mutations.isEmpty() || mutations.size() > FileModelTransactionStore.MAX_MUTATIONS) {
      throw new IllegalArgumentException("Invalid transaction child count");
    }
    final List<ModelMutation> copy = new ArrayList<>(mutations);
    for (final ModelMutation mutation : copy) Objects.requireNonNull(mutation, "mutation");
    this.mutations = Collections.unmodifiableList(copy);
  }

  public String protocol() { return protocol; }
  public String manifest() { return manifest; }
  public String actorScope() { return actorScope; }
  public String operationId() { return operationId; }
  public List<ModelMutation> mutations() { return mutations; }
}
