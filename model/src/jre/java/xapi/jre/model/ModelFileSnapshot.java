package xapi.jre.model;
import xapi.model.api.Model;
/** A detached model decoded from exactly the immutable record used by its read condition. */
public final class ModelFileSnapshot<M extends Model> {
  private final ModelStoreKey key;
  private final ModelStoreRecord record;
  private final M model;
  ModelFileSnapshot(final ModelStoreKey key, final ModelStoreRecord record, final M model) {
    this.key = key; this.record = record; this.model = model;
  }
  public ModelStoreKey storeKey() { return key; }
  public ModelStoreRecord record() { return record; }
  public M model() { return model; }
}
