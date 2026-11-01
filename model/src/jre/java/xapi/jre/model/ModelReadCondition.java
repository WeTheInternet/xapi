package xapi.jre.model;
import java.util.Objects;
/** Exact immutable image used by an adapter's preparation, including genuine absence. */
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
