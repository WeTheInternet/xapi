/**
 *
 */
package xapi.test.model;

import xapi.annotation.inject.SingletonOverride;
import xapi.bytecode.impl.BytecodeAdapterService;
import xapi.fu.itr.MappedIterable;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQuery.QueryParameter;
import xapi.model.api.ModelQueryResult;
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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@SingletonOverride(priority=Integer.MIN_VALUE+2, implFor=ModelService.class)
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

  /**
   * @see xapi.model.service.ModelService#query(java.lang.Class, xapi.model.api.ModelQuery, xapi.util.api.SuccessHandler)
   */
  @Override
  public <M extends Model> void query(final Class<M> modelClass, final ModelQuery<M> query,
      final SuccessHandler<ModelQueryResult<M>> callback) {
    // The ramCache version of the service is going to be quite poor.
    final ModelQueryResult<M> result = new ModelQueryResult<>(modelClass);
    final ModelKey cursorKey = getCursorKey(query);
    for (final Entry<ModelKey, Model> entry : ramCache.entrySet()) {
      final ModelKey key = entry.getKey();
      if (query.getNamespace().equals(key.getNamespace()) && modelClass == typeNameToClass.get(key.getKind())) {
        if (doesMatch(modelClass, query, cursorKey, entry.getValue())) {
          result.addModel((M) entry.getValue());
          if (result.getSize() == query.getPageSize()) {
            break;
          }
        }
      }
    }
    if (result.getSize() > 0) {
      final List<M> list = result.getModelList();
      result.setCursor(keyToString(list.get(list.size()-1).getKey()));
    } else {
      result.setCursor(query.getCursor());
    }
    callback.onSuccess(result);
  }

  @Override
  public void query(final ModelQuery<Model> query, final SuccessHandler<ModelQueryResult<Model>> callback) {
    // The ramCache version of the service is going to be quite poor.
    final ModelQueryResult<Model> result = new ModelQueryResult<>(Model.class);
    final ModelKey cursorKey = getCursorKey(query);
    for (final Entry<ModelKey, Model> entry : ramCache.entrySet()) {
      final ModelKey key = entry.getKey();
      if (query.getNamespace().equals(key.getNamespace())) {
        if (doesMatch(null, query, cursorKey, entry.getValue())) {
          result.addModel(entry.getValue());
          if (result.getSize() == query.getPageSize()) {
            break;
          }
        }
      }
    }
    if (result.getSize() > 0) {
      final List<Model> list = result.getModelList();
      result.setCursor(keyToString(list.get(list.size()-1).getKey()));
    } else {
      result.setCursor(query.getCursor());
    }
    callback.onSuccess(result);
  }

  protected ModelKey getCursorKey(final ModelQuery<? extends Model> query) {
    if (query.getCursor() == null) {
      return null;
    } else {
      return keyFromString(query.getCursor());
    }
  }

  protected boolean doesMatch(final Class<? extends Model> modelClass, final ModelQuery<? extends Model> query, final ModelKey cursorKey, final Model model) {
    final ModelKey key = model.getKey();
    if (!query.getNamespace().equals(key.getNamespace())) {
      return false;
    }
    if (modelClass != null && !modelClass.isAssignableFrom(model.getClass())) {
      return false;
    }
    if (cursorKey != null) {
      // Make sure the model's key is greater than our cursor
      if (key.getId().compareTo(cursorKey.getId()) < 1) {
        return false;
      }
    }
    for (final QueryParameter param : query.getParameters()) {
      if (!matches(param, model)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @param param
   * @param model
   * @return
   */
  private boolean matches(final QueryParameter param, final Model model) {
    final Object value = model.getProperty(param.getParameterName());
    assert typesEqual(value, param.getFilterValue());
    switch (param.getFilterType()) {
      case EQUALS:
        return Objects.deepEquals(value, param.getFilterValue());
      case CONTAINS:
        if (value == null) {
          return param.getFilterValue() == null;
        }
        if (value instanceof String) {
          return ((String)value).contains(String.valueOf(param.getFilterValue()));
        }
        // else do array / map / container matching...
        if (value.getClass().isArray()) {
          if (param.getFilterValue() instanceof Comparable) {
            return Arrays.binarySearch((Object[])value, param.getFilterValue()) != -1;
          }
          for (int i = 0, m = Array.getLength(value); i < m; i++) {
            if (Objects.equals(Array.get(value, i), param.getFilterValue())) {
              return true;
            }
          }
        }
        // We can do lists/maps later...
        throw new UnsupportedOperationException("Contains queries not yet supported for "+value.getClass()+" types.");
      case GREATER_THAN:
        if (value == null) {
          return false;
        }
        return ((Comparable)value).compareTo(param.getFilterValue()) > 0;
      case LESS_THAN:
        if (value == null) {
          return false;
        }
        return ((Comparable)value).compareTo(param.getFilterValue()) < 0;
    }
    return false;
  }

  private boolean typesEqual(final Object value1, final Object value2) {
    if (value1 == null || value2 == null) {
      return true;
    }
    if (value1.getClass().isAssignableFrom(value2.getClass())) {
      return true;
    }
    if (value2.getClass().isAssignableFrom(value1.getClass())) {
      return true;
    }
    return false;
  }

  @Override
  protected boolean isAsync() {
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Model> T doCreate(final Class<T> key) {
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
    return super.doCreate(key);
  }

  @Override
  public <M extends Model> Class<M> typeToClass(final String kind) {
    throw new UnsupportedOperationException("Test Model Service does not support .typeToClass()");
  }

  @Override
  public MappedIterable<Method> getMethodsInDeclaredOrder(Class<?> type) {
    return BytecodeAdapterService.getMethodsInDeclaredOrder(type);
  }
}
