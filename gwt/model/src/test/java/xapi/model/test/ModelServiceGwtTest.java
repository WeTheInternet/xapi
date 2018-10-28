package xapi.model.test;

import org.junit.Assert;
import org.junit.Test;
import xapi.collect.api.IntTo;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelList;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;
import xapi.model.user.ModelUser;
import xapi.time.X_Time;

import java.util.Arrays;

import static xapi.model.X_Model.newKey;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.reflect.shared.GwtReflect;

public class ModelServiceGwtTest
    extends GWTTestCase
{

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
    final ModelContent content = X_Model.create(ModelContent.class);
    final long time = System.currentTimeMillis();
    content.setText("Hello World");
    content.setTime(time);
    content.setKey(newKey("content"));
    X_Log.error(getClass(), "Original: ",content);
    final String serialized = X_Model.serialize(ModelContent.class, content);
    X_Log.error(getClass(), "Serialized: ",serialized);
    final ModelContent deserialized = X_Model.deserialize(ModelContent.class, serialized);
    X_Log.error(getClass(), "Deserialized: ",deserialized);
    X_Model.persist(content, received -> {
          try {
            X_Log.error("Received: ", received);
            X_Log.error("Sent: ", content);
            assertNotNull(received.getKey().getId());

            content.setKey(received.getKey());

            assertArrayEquals(content.getChildren(), received.getChildren());
            assertArrayEquals(content.getRelated(), received.getRelated());
            assertArrayEquals(content.getUpvotes(), received.getUpvotes());
            assertArrayEquals(content.getDownvotes(), received.getDownvotes());

            assertEquals(content.getPermaLink(), received.getPermaLink());
            assertEquals(0, content.getTime(), received.getTime());
            assertEquals(content.getText(), received.getText());
            assertEquals(content.getAuthor(), received.getAuthor());

            final String reserialized = X_Model.serialize(ModelContent.class, received);
            final String withKey = X_Model.serialize(ModelContent.class, content);
            assertEquals(withKey, reserialized);

          } finally {
//            finishTest();
          }
    });
    X_Time.trySleep(2000, 0);
//    delayTestFinish(5000);
  }

  private <T> void assertArrayEquals(T[] expected, T[] received) {
    assertEquals(Arrays.asList(expected), Arrays.asList(received));
  }

  @Test
  public void testModelList() {
    final ModelList<ModelUser> model = X_Model.create(ModelList.class);
    model.setModelType(ModelUser.class);
    final ModelUser user = X_Model.create(ModelUser.class);
    model.add(user);
  }

  @Test
  public void testModelWithEverything() {
    final ModelWithEverything model = X_Model.create(ModelWithEverything.class);
    assertNotNull(model.getModelArray());
    assertNotNull(model.getIntTo());
    final IntTo intTo = model.getIntTo();
    // Lets make sure the IntTo generated has the correct component type.
    intTo.add("String");
    try {
      intTo.add(1);
      Assert.fail("Expected assertion error; either you are not running with assertions enabled; -ea, " +
              "or the IntTo created for this model does not have the correct component type");
    } catch (AssertionError expected){

    }

//    for (String property : model.getPropertyNames()) {
//      X_Log.error(getClass(), property, model.getProperty(property));
//    }
  }

  public String getModuleName() {
    return "xapi.test.ModelTest";
  }

  //
//  /**
//   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
//   */
//  @Override
//  public String getModuleName() {
//    return "xapi.test.ModelTest";
//  }

}
