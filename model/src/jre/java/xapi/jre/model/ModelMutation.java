package xapi.jre.model;

import java.util.Objects;

/** Immutable conditional replacement; -1 means must not exist, not revision zero. */
public final class ModelMutation {
  public static final long MUST_NOT_EXIST = -1;
  private final ModelStoreKey key;
  private final long expectedRevision;
  private final byte[] value;

  public ModelMutation(final ModelStoreKey key, final long expectedRevision, final byte[] value) {
    this.key = Objects.requireNonNull(key, "key");
    if (expectedRevision < MUST_NOT_EXIST) throw new IllegalArgumentException("Invalid expected revision");
    this.expectedRevision = expectedRevision;
    Objects.requireNonNull(value, "value");
    if (value.length > FileModelTransactionStore.MAX_REQUEST_BYTES) throw new IllegalArgumentException("Model image too large");
    this.value = value.clone();
  }

  public ModelStoreKey key() { return key; }
  public long expectedRevision() { return expectedRevision; }
  public byte[] value() { return value.clone(); }
}
