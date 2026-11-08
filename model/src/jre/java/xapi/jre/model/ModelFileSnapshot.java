package xapi.jre.model;

import xapi.model.api.Model;

/**
 * The result of one owned-store read, pairing the exact stored image and revision
 * with a decoded model view. The record and store key describe the same instant;
 * use both when constructing a {@link ModelReadCondition} for a later conditional
 * transaction. An absent record has no model and still supplies a read condition.
 *
 * <p>The decoded model is detached from persistence but remains mutable. Mutating
 * it does not change the captured record bytes or guarantee that the store has
 * not changed since this read. The store validates those bytes again at commit.
 */
public final class ModelFileSnapshot<M extends Model> {
  private final ModelStoreKey key;
  private final ModelStoreRecord record;
  private final M model;

  ModelFileSnapshot(final ModelStoreKey key, final ModelStoreRecord record, final M model) {
    this.key = key;
    this.record = record;
    this.model = model;
  }

  /** Immutable canonical identity and relative storage location for this read. */
  public ModelStoreKey storeKey() { return key; }

  /** Original existence, revision and defensively copied image, including absence. */
  public ModelStoreRecord record() { return record; }

  /** Detached decoded view, or null if the record did not exist at read time. */
  public M model() { return model; }
}
