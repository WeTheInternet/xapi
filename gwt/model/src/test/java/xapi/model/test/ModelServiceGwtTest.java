package xapi.model.test;

import static xapi.model.X_Model.newKey;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.reflect.shared.GwtReflect;

import org.junit.Test;

import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;

public class ModelServiceGwtTest extends GWTTestCase {

  @Test
  public void testKeySerialization_Empty() {
    final ModelKey key = X_Model.newKey("testkind");
    final String serialized = X_Model.keyToString(key);
    final ModelKey asKey = X_Model.keyFromString(serialized);
    assertEquals(key, asKey);
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
    content.setKey(newKey("content"));
    content.setText("Hello World");
    content.setTime(time);
    final ModelContent[] array = GwtReflect.newArray(ModelContent.class, 1);
    array[0] = X_Model.create(ModelContent.class);
    content.setRelated(array);
    final String serialized = X_Model.serialize(ModelContent.class, content);
    final ModelContent asModel = X_Model.deserialize(ModelContent.class, serialized);
    assertEquals(content, asModel);
  }


  @Test
  public void testModelPersistence() {
    final ModelText content = X_Model.create(ModelText.class);
    final long time = System.currentTimeMillis();
    content.setText("Hello World");
    content.setTime(time);
    content.setKey(newKey("content"));
    X_Log.error(getClass(), "Original: "+content);
    X_Log.error(getClass(), "Serialized: "+X_Model.serialize(ModelText.class, content));
    X_Log.error(getClass(), "Deserialized: "+X_Model.deserialize(ModelText.class, X_Model.serialize(ModelText.class, content)));
    X_Model.persist(content, m -> {
      assertNotNull(m.getKey().getId());
    });
  }

  /**
   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
   */
  @Override
  public String getModuleName() {
    return "xapi.test.ModelTest";
  }

}
