package xapi.jre.model;

/** An immutable result captured under the store gate. Existing unversioned files have revision zero. */
public final class ModelStoreRecord {
  private final byte[] value;
  private final long revision;

  ModelStoreRecord(final byte[] value, final long revision) {
    this.value = value == null ? null : value.clone();
    this.revision = revision;
  }

  public boolean exists() { return value != null; }
  public byte[] value() { return value == null ? null : value.clone(); }
  public long revision() { return revision; }
}
