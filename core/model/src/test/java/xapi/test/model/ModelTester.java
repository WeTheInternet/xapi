package xapi.test.model;

import org.junit.Assert;
import org.junit.Test;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.Key;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.dev.model.HasModelFields;
import xapi.dev.model.ModelField;
import xapi.inject.X_Inject;
import xapi.model.api.Model;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelManifest.MethodData;
import xapi.model.impl.ModelNameUtil;
import xapi.model.impl.ModelUtil;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class ModelTester {

  interface IsSerializable extends Serializable, Comparable<Double> {}

  @IsModel(key=@Key("id"), modelType="test")
  public interface TestModel <E extends Exception, I extends IntTo<E>> extends Model {
    String id();

    String getItem();
    TestModel setItem(String item);
    void deleteItem();

    <T extends Runnable> IntTo<T> getParameterizedTypeVariableWithExtendsOnMethod();
    <T extends Map> IntTo<? extends T> getParameterizedTypeVariableWithWildcardOnMethod();

    <T extends List & Set> IntTo<? extends T> getWideTypeVariablesWithCommonInterface();
    <T extends AbstractList & Set> IntTo<? extends T> getWideTypeVariablesWithConcreteType();
    <T extends Double & IsSerializable> IntTo<? extends T> getWideTypeVariablesWithMultipleMatches();

    <T extends IntTo<Callable>> T getTypeVariableWithExtendsOnMethod();
    IntTo<E> getParameterizedTypeVariableWithExtendsOnClass();
    I getTypeVariableWithExtendsOnClass();
    <T extends Iterator<E>> ObjectTo<T, I> getMapTypes();

  }

  @Test
  public void testModelManifest() {
    final ModelManifest manifest = ModelUtil.createManifest(TestModel.class);
    assertNotNull(manifest);

    MethodData method = manifest.getMethodData("getParameterizedTypeVariableWithExtendsOnMethod");
    assertArrayEquals(new Class[]{Runnable.class}, method.getTypeParams());
    assertEquals(IntTo.class, method.getType());

    method = manifest.getMethodData("getParameterizedTypeVariableWithWildcardOnMethod");
    assertArrayEquals(new Class[]{Map.class}, method.getTypeParams());
    assertEquals(IntTo.class, method.getType());

    method = manifest.getMethodData("getWideTypeVariablesWithCommonInterface");
    assertArrayEquals(new Class[]{Collection.class}, method.getTypeParams());
    assertEquals(IntTo.class, method.getType());

    method = manifest.getMethodData("getWideTypeVariablesWithConcreteType");
    assertArrayEquals(new Class[]{Collection.class}, method.getTypeParams());
    assertEquals(IntTo.class, method.getType());

    method = manifest.getMethodData("getWideTypeVariablesWithMultipleMatches");
    assertArrayEquals(new Class[]{Object.class}, method.getTypeParams());
    assertEquals(IntTo.class, method.getType());

    method = manifest.getMethodData("getTypeVariableWithExtendsOnMethod");
    assertArrayEquals(new Class[]{Callable.class}, method.getTypeParams());
    assertEquals(IntTo.class, method.getType());

    method = manifest.getMethodData("getParameterizedTypeVariableWithExtendsOnClass");
    assertArrayEquals(new Class[]{Exception.class}, method.getTypeParams());
    assertEquals(IntTo.class, method.getType());

    method = manifest.getMethodData("getTypeVariableWithExtendsOnClass");
    assertArrayEquals(new Class[]{Exception.class}, method.getTypeParams());
    assertEquals(IntTo.class, method.getType());

    method = manifest.getMethodData("getMapTypes");
    assertArrayEquals(new Class[]{Iterator.class, IntTo.class}, method.getTypeParams());
    assertEquals(ObjectTo.class, method.getType());

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
