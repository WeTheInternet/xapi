package xapi.model.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;

import xapi.annotation.model.SerializationStrategy;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelModule;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelRating;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.tools.ModelSerializerDefault;
import xapi.time.X_Time;
import xapi.util.api.Pointer;
import xapi.util.api.SuccessHandler;

import java.util.EnumMap;

public class ModelServiceTestJre {

  @Test
  public void testKeySerialization_Empty() {
    final ModelKey key = X_Model.newKey("testkind");
    final String serialized = X_Model.keyToString(key);
    final ModelKey asKey = X_Model.keyFromString(serialized);
    Assert.assertEquals(key, asKey);
  }

  @Test
  public void testKeySerialization_Simple() {
    final ModelKey key = X_Model.newKey("ns", "testkind");
    final String serialized = X_Model.keyToString(key);
    final ModelKey asKey = X_Model.keyFromString(serialized);
    assertEquals(key, asKey);
    assertEquals("ns", key.getNamespace());
    assertEquals("testkind", key.getKind());
    assertEquals(null, key.getId());
  }

  @Test
  public void testKeySerialization_Full() {
    final ModelKey key = X_Model.newKey("ns", "testkind", "id");
    final String serialized = X_Model.keyToString(key);
    final ModelKey asKey = X_Model.keyFromString(serialized);
    assertEquals(key, asKey);
    assertEquals("ns", key.getNamespace());
    assertEquals("testkind", key.getKind());
    assertEquals("id", key.getId());
  }

  @Test
  public void testKeySerialization_WithParent() {
    ModelKey key = X_Model.newKey("ns", "testkind", "id");
    key = key.getChild("child", "123");
    final String serialized = X_Model.keyToString(key);
    final ModelKey asKey = X_Model.keyFromString(serialized);
    assertEquals(key, asKey);
    assertEquals("ns", key.getNamespace());
    assertEquals("child", key.getKind());
    assertEquals("123", key.getId());
    assertEquals("ns", key.getParent().getNamespace());
    assertEquals("testkind", key.getParent().getKind());
    assertEquals("id", key.getParent().getId());
  }

  @Test
  public void testSimpleSerialization() {
    final ModelText text = X_Model.create(ModelText.class);
    final long time = System.currentTimeMillis();
    text.setText("Hello World");
    text.setTime(time);
    final String serialized = X_Model.serialize(ModelText.class, text);
    final ModelText asModel = X_Model.deserialize(ModelText.class, serialized);
    assertEquals(text, asModel);
  }

  @Test
  public void testSimpleSerialization_WithKey() {
    final ModelText text = X_Model.create(ModelText.class);
    text.setKey(X_Model.newKey("ns", "text", "id"));
    final long time = System.currentTimeMillis();
    text.setText("Hello World");
    text.setTime(time);
    final String serialized = X_Model.serialize(ModelText.class, text);
    final ModelText asModel = X_Model.deserialize(ModelText.class, serialized);
    assertEquals(text, asModel);
    assertNotNull("Deserialized model must have a key!", asModel.getKey());
    // Because there is a key in the model, these objects should still be equal
    // if the values of the model changed.
    text.setText("different");
    assertEquals(text, asModel);
  }

  @Test
  public void testComplexSerialization() {
    final ModelContent content = X_Model.create(ModelContent.class);
    final long time = System.currentTimeMillis();
    content.setText("Hi there,");
    content.setTime(time);
    final ModelContent related = X_Model.create(ModelContent.class);
    related.setText("Hello!");
    content.related().add(related);
    final String serialized = X_Model.serialize(ModelContent.class, content);
    final ModelContent asModel = X_Model.deserialize(ModelContent.class, serialized);
    assertEquals(content, asModel);
  }

  @Test
  public void testModelEnumMap() {
    final TestModelEnumMap hasMap = X_Model.create(TestModelEnumMap.class);
    final String nullMap = X_Model.serialize(TestModelEnumMap.class, hasMap);
    final TestModelEnumMap deserializeNullMap = X_Model.deserialize(TestModelEnumMap.class, nullMap);
    assertEquals(hasMap, deserializeNullMap);

    final EnumMap<SerializationStrategy, Integer> theMap = new EnumMap<SerializationStrategy, Integer>(SerializationStrategy.class);
    hasMap.setItems(theMap);
    final String emptyMap = X_Model.serialize(TestModelEnumMap.class, hasMap);
    final TestModelEnumMap deserializeEmpty = X_Model.deserialize(TestModelEnumMap.class, emptyMap);
    assertEquals(hasMap, deserializeEmpty);

    hasMap.setItems(theMap);
    hasMap.getItems().put(SerializationStrategy.Custom, 123);
    final String fullMap = X_Model.serialize(TestModelEnumMap.class, hasMap);
    final TestModelEnumMap deserializeFull = X_Model.deserialize(TestModelEnumMap.class, fullMap);
    assertEquals(hasMap, deserializeFull);

  }

  @Test
  public void testModelPersistence() {
    final ModelContent content = X_Model.create(ModelContent.class);
    final long time = System.currentTimeMillis();
    content.setText("Hello World");
    content.setTime(time);
    final ModelContent related = X_Model.create(ModelContent.class);
    content.related().add(related);
    content.setKey(X_Model.newKey(content.getType()));
    final Pointer<Boolean> waiting = new Pointer<>(true);
    X_Model.persist(content,
    new SuccessHandler<ModelContent>() {

      @Override
      public void onSuccess(final ModelContent m) {
        X_Model.load(ModelContent.class, m.getKey(),
            new SuccessHandler<ModelContent>() {
          @Override
          public void onSuccess(final ModelContent loaded) {
            Assert.assertFalse(loaded == m);
            Assert.assertTrue(loaded.equals(m));
            Assert.assertTrue(related.equals(loaded.related().first()));
            waiting.set(false);
          }
        });
      }
    });
    final long deadline = System.currentTimeMillis()+3000;
    while (waiting.get()) {
      X_Time.trySleep(10, 0);
      Assert.assertTrue(X_Time.isFuture(deadline));
    }
  }

  @Test
  public void testModuleSerializationRoundTrip() {
    final ModelModule module = new ModelModule();
    module.addManifest(ModelUtil.createManifest(ModelContent.class));
    module.addManifest(ModelUtil.createManifest(ModelText.class));
    module.addManifest(ModelUtil.createManifest(ModelRating.class));
    final String serialized = ModelSerializerDefault.serialize(module);
    System.out.println(serialized);
    final ModelModule deserialized = ModelSerializerDefault.deserializeModule(serialized);
    Assert.assertEquals(module, deserialized);
  }

}
