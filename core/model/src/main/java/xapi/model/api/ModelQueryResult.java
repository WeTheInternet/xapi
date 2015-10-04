/**
 *
 */
package xapi.model.api;

import static xapi.collect.X_Collect.newList;

import java.util.List;

import xapi.collect.api.IntTo;
import xapi.model.service.ModelService;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelQueryResult <M extends Model> {

  private String cursor;
  private final IntTo<M> models;
  private final Class<M> modelClass;

  public ModelQueryResult(final Class<M> modelClass) {
    models = newList(modelClass);
    this.modelClass = modelClass;
  }

  public ModelQueryResult<M> addModel(final M model) {
    models.add(model);
    return this;
  }

  /**
   * @return -> models
   */
  public Iterable<M> getModels() {
    return models.forEach();
  }

  public List<M> getModelList() {
    return models.asList();
  }

  /**
   * @return -> cursor
   */
  public String getCursor() {
    return cursor;
  }

  /**
   * @param cursor -> set cursor
   * @return
   */
  public ModelQueryResult<M> setCursor(final String cursor) {
    this.cursor = cursor;
    return this;
  }

  /**
   * @return
   */
  public int getSize() {
    return models.size();
  }

  public String serialize(final ModelService service, final PrimitiveSerializer primitives) {
    final StringBuilder b = new StringBuilder();
    b.append(primitives.serializeString(getCursor()));
    b.append(primitives.serializeInt(getSize()));
    if (modelClass == Model.class) {
      // Model type was unknown, we need to serialize all type names
      for (final M model : models.forEach()) {
        b.append(primitives.serializeString(model.getType()));
        b.append(service.serialize(service.typeToClass(model.getType()), model));
      }
    } else {
      // Model type was known, just serialize the models.
      for (final M model : models.forEach()) {
        b.append(service.serialize(modelClass, model));
      }
    }
    return b.toString();
  }


}
