package xapi.model.api;

import java.util.Objects;

/** Result of one conditional creation; the model is a detached acknowledged image, not the caller's object. */
public final class ModelCreateResult<M extends Model> {
  private final boolean created;
  private final M model;

  public ModelCreateResult(final boolean created, final M model) {
    this.created = created;
    this.model = Objects.requireNonNull(model, "model");
  }

  public boolean isCreated() { return created; }
  public M getModel() { return model; }
}
