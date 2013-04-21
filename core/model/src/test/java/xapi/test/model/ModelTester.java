package xapi.test.model;

import org.junit.Assert;
import org.junit.Test;

import xapi.annotation.model.Key;
import xapi.annotation.model.Serializable;
import xapi.dev.model.HasModelFields;
import xapi.dev.model.ModelField;
import xapi.dev.model.ModelUtil;
import xapi.inject.X_Inject;
import xapi.model.api.Model;

public class ModelTester {

  @Serializable
  public interface TestModel extends Model {
    @Key
    String id();

    Object getItem();
    TestModel setItem(Object item);
    void deleteItem();

  }

  @Test
  public void testStringManipulation() throws Exception {
    String item = ModelUtil.stripGetter("getItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelUtil.stripGetter("item");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelUtil.stripGetter("isItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelUtil.stripGetter("hasItem");
    Assert.assertTrue(item, "item".equals(item));

    item = ModelUtil.stripSetter("setItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelUtil.stripSetter("putItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelUtil.stripSetter("putAllItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelUtil.stripSetter("setAllItem");
    Assert.assertTrue(item, "item".equals(item));
  }
  @Test
  public void testModel() throws Exception {
    HasModelFields model = generateModel();

    System.out.println(model);
  }

  private HasModelFields generateModel() throws Exception {
    HasModelFields fields = X_Inject.instance(HasModelFields.class);
    ModelField id = fields.getOrMakeField("id");
    id.setKey(TestModel.class.getMethod("id").getAnnotation(Key.class));

    return fields;
  }

}
