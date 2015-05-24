/**
 *
 */
package xapi.test.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;

import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelSerializationTest {

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
  public void testKeySerialization_WithMultipleParents() {
    ModelKey key = X_Model.newKey("ns", "testkind", "id");
    key = key.getChild("child", "123");
    key = key.getChild("grandchild", "abc");
    final String serialized = X_Model.keyToString(key);
    final ModelKey asKey = X_Model.keyFromString(serialized);
    assertEquals(key, asKey);

    assertEquals("ns", key.getNamespace());
    assertEquals("grandchild", key.getKind());
    assertEquals("abc", key.getId());

    key = key.getParent();
    assertEquals("ns", key.getNamespace());
    assertEquals("child", key.getKind());
    assertEquals("123", key.getId());

    key = key.getParent();
    assertEquals("ns", key.getNamespace());
    assertEquals("testkind", key.getKind());
    assertEquals("id", key.getId());
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
    content.setText("Hello World");
    content.setTime(time);
    content.setRelated(new ModelContent[]{X_Model.create(ModelContent.class)});
    final String serialized = X_Model.serialize(ModelContent.class, content);
    final ModelContent asModel = X_Model.deserialize(ModelContent.class, serialized);
    assertEquals(content, asModel);
  }
}
