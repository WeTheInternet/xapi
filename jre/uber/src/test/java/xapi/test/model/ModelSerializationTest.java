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
import xapi.model.api.ModelList;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;
import xapi.util.api.SuccessHandler;

import java.util.concurrent.atomic.AtomicReference;

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
  public void testI18nSerialization() throws Throwable {
    final ModelText text = X_Model.create(ModelText.class);
    final long time = System.currentTimeMillis();
    text.setKey(X_Model.newKey("", "txt", "いちにさん"));
    text.setText("妖異達の通り雨");
    text.setTime(time);
    final String serialized = X_Model.serialize(ModelText.class, text);
    final ModelText asModel = X_Model.deserialize(ModelText.class, serialized);
    assertEquals(text, asModel);
    Exception success = new Exception();
    AtomicReference<Throwable> result = new AtomicReference<>(null);
    X_Model.persist(asModel, SuccessHandler.handler(saved->{
      // now that we've persisted... verify we are the same
      try {
        String savedTxt = X_Model.serialize(ModelText.class, saved);
        assertEquals(serialized, savedTxt);
        // now that we've gotten this far... load it back and verify again
        X_Model.load(ModelText.class, saved.key(), SuccessHandler.handler(loaded->{
          try {
            String loadedTxt = X_Model.serialize(ModelText.class, saved);
            assertEquals(serialized, loadedTxt);
            // HERE is the only way to escape this mess with success
            result.set(success);
          } catch (Throwable failed) {
            result.set(failed);
          }
          synchronized (result) {
            result.notifyAll();
          }
        }, failedLoad -> {
          result.set(failedLoad);
          synchronized (result) {
            result.notifyAll();
          }
        }));
      } catch (Throwable failed) {
        result.set(failed);
        synchronized (result) {
          result.notifyAll();
        }
      }
    }, failedSave->{
      result.set(failedSave);
      synchronized (result) {
        result.notifyAll();
      }
    }));
    synchronized (result) {
      result.wait(5_000);
    }
    if (result.get() != success) {
      throw result.get();
    }
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
    final ModelContent related = X_Model.create(ModelContent.class);
    related.key().setId("child");
    related.setText("related");
    final ModelList<ModelContent> list = content.related();
    list.add(related);
    X_Model.persist(related, SuccessHandler.noop());
    final String serialized = X_Model.serialize(ModelContent.class, content);
    final ModelContent asModel = X_Model.deserialize(ModelContent.class, serialized);
    assertEquals(content, asModel);
  }
}
