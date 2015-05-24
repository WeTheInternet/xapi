/**
 *
 */
package xapi.test.model;

import java.util.HashMap;

import xapi.annotation.inject.SingletonOverride;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.content.ModelComment;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelRating;
import xapi.model.content.ModelText;
import xapi.model.impl.AbstractModelService;
import xapi.model.service.ModelService;
import xapi.model.user.ModelUser;
import xapi.test.model.content.ModelCommentTest;
import xapi.test.model.content.ModelContentTest;
import xapi.test.model.content.ModelRatingTest;
import xapi.test.model.content.ModelTextTest;
import xapi.test.model.content.ModelUserTest;
import xapi.util.api.SuccessHandler;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@SingletonOverride(priority=Integer.MIN_VALUE, implFor=ModelService.class)
public class ModelServiceTestImpl extends AbstractModelService {

  private final HashMap<ModelKey, Model> ramCache = new HashMap<>();

  @Override
  protected <M extends Model> void doPersist(final String type, final M model, final SuccessHandler<M> callback) {
    X_Log.info(getClass(), "Persist",type,"as\n",model);
    ModelKey key = model.getKey();
    if (key == null) {
      key = newKey(null, type);
      model.setKey(key);
    }
    ramCache.put(key, model);
    callback.onSuccess(model);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <M extends Model> void load(final Class<M> modelClass,
      final ModelKey modelKey, final SuccessHandler<M> callback) {
    callback.onSuccess((M) ramCache.get(modelKey));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Model> T create(final Class<T> key) {
    if (key == ModelText.class) {
      return (T) new ModelTextTest();
    }
    if (key == ModelContent.class) {
      return (T) new ModelContentTest();
    }
    if (key == ModelRating.class) {
      return (T) new ModelRatingTest();
    }
    if (key == ModelUser.class) {
      return (T) new ModelUserTest();
    }
    if (key == ModelComment.class) {
      return (T) new ModelCommentTest();
    }
    return super.create(key);
  }

}
