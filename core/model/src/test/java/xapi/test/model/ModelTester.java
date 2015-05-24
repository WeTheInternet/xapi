package xapi.test.model;

import org.junit.Assert;
import org.junit.Test;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.Key;
import xapi.dev.model.HasModelFields;
import xapi.dev.model.ModelField;
import xapi.inject.X_Inject;
import xapi.model.api.Model;
import xapi.model.impl.ModelNameUtil;

public class ModelTester {

  @IsModel(key=@Key("id"), modelType="test")
  public interface TestModel extends Model {
    String id();

    Object getItem();
    TestModel setItem(Object item);
    void deleteItem();

  }

  @Test
  public void testStringManipulation() throws Exception {
    String item = ModelNameUtil.stripGetter("getItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelNameUtil.stripGetter("item");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelNameUtil.stripGetter("isItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelNameUtil.stripGetter("hasItem");
    Assert.assertTrue(item, "item".equals(item));

    item = ModelNameUtil.stripSetter("setItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelNameUtil.stripSetter("putItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelNameUtil.stripSetter("putAllItem");
    Assert.assertTrue(item, "item".equals(item));
    item = ModelNameUtil.stripSetter("setAllItem");
    Assert.assertTrue(item, "item".equals(item));
  }
  @Test
  public void testModel() throws Exception {
    final HasModelFields model = generateModel();

    System.out.println(model);
  }

  private HasModelFields generateModel() throws Exception {
    final HasModelFields fields = X_Inject.instance(HasModelFields.class);
    final ModelField id = fields.getOrMakeField("id");
    id.setKey(TestModel.class.getMethod("id").getAnnotation(Key.class));

    return fields;
  }

}
