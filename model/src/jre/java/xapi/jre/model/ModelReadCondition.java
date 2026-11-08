package xapi.jre.model;
import java.util.Objects;
/**
 * Exact immutable precondition for an owned-store transaction, including genuine
 * absence. The commit gate validates the captured revision, existence and bytes;
 * comparing only the revision would miss ordinary writes to unversioned records.
 * A condition may also cover a read-only prerequisite not present in the mutation list.
 */
public final class ModelReadCondition {
  private final ModelStoreKey key;
  private final ModelStoreRecord expected;
  public ModelReadCondition(final ModelStoreKey key, final ModelStoreRecord expected) {
    this.key = Objects.requireNonNull(key, "key");
    final ModelStoreRecord record = Objects.requireNonNull(expected, "expected");
    this.expected = new ModelStoreRecord(record.value(), record.revision());
  }
  public ModelStoreKey key() { return key; }
  public ModelStoreRecord expected() { return expected; }
}
