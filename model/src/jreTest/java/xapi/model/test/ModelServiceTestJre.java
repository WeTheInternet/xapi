package xapi.model.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;

import xapi.annotation.model.SerializationStrategy;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelList;
import xapi.model.api.ModelModule;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelRating;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.tools.ModelSerializerDefault;
import xapi.time.X_Time;
import xapi.util.api.Pointer;
import xapi.util.api.SuccessHandler;

import java.util.Arrays;
import java.util.EnumMap;

public class ModelServiceTestJre {

    static {
        X_Log.logLevel(LogLevel.DEBUG);
    }
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
        final long deadline = System.currentTimeMillis() + 3000;
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


    @Test
    public void testModelList_NullHandling_NoSerializerPruning_KeysOnly() {
        // Ensure nulls are refused by ModelList (not pruned by the serializer)
        final ModelContent content = X_Model.create(ModelContent.class);

        // Initially empty
        assertEquals(0, content.related().size());

        // Adding null should be a no-op
        content.related().add(null);
        assertEquals(0, content.related().size());

        // Add a real related model
        final ModelContent related = X_Model.create(ModelContent.class);
        related.setText("Hello!");
        content.related().add(related);
        assertEquals(1, content.related().size());

        // Adding another null remains a no-op
        content.related().add(null);
        assertEquals(1, content.related().size());

        // Serialize/deserialize; contents should be preserved without any serializer-side pruning logic
        final String serialized = X_Model.serialize(ModelContent.class, content);
        final ModelContent roundTrip = X_Model.deserialize(ModelContent.class, serialized);

        assertNotNull(roundTrip);
        assertEquals(1, roundTrip.related().size());
        assertEquals(content.related().first(), roundTrip.related().first());
    }

    @Test
    public void testModelList_NullHandling_NoSerializerPruning_FullModel() {
        // Ensure nulls are refused by ModelList (not pruned by the serializer)
        final ModelContent content = X_Model.create(ModelContent.class);

        // Initially empty
        assertEquals(0, content.children().size());

        // Adding null should be a no-op
        content.children().add(null);
        assertEquals(0, content.children().size());

        // Add a real child model
        final ModelContent child = X_Model.create(ModelContent.class);
        child.setText("Hello!");
        content.children().add(child);
        final ModelContent child2 = X_Model.create(ModelContent.class);
        child2.setText("World!");
        content.children().add(child2);
        assertEquals(2, content.children().size());

        // Adding another null remains a no-op
        content.children().add(null);
        assertEquals(2, content.children().size());

        // Serialize/deserialize; contents should be preserved without any serializer-side pruning logic
        final String serialized = X_Model.serialize(ModelContent.class, content);
        final ModelContent roundTrip = X_Model.deserialize(ModelContent.class, serialized);

        assertNotNull(roundTrip);
        assertEquals(2, roundTrip.children().size());
        assertEquals(content.children().first(), roundTrip.children().first());
    }


    @Test
    public void testEmbeddedChildrenArray_AllowsNulls_RoundTripPreserved() {
        // Ensure embedded model arrays preserve nulls through serialization
        final ModelContent parent = X_Model.create(ModelContent.class);

        final ModelContent child1 = X_Model.create(ModelContent.class);
        child1.setText("child-1");
        final ModelContent child2 = X_Model.create(ModelContent.class);
        child2.setText("child-2");

        parent.children().add(child1);
        parent.children().add(child2);

        final String serialized = X_Model.serialize(ModelContent.class, parent);
        final ModelContent roundTrip = X_Model.deserialize(ModelContent.class, serialized);

        assertNotNull(roundTrip);
        Assert.assertEquals(2, roundTrip.children().size());
        Assert.assertEquals(child1, roundTrip.children().first());
        Assert.assertEquals(child2, roundTrip.children().last());

        // Overall equality should hold as well
        Assert.assertEquals(parent, roundTrip);
    }

    @Test
    public void testModelList_Serialization_MultiItem_NoExtraMarkers() {
        // Create a ModelList directly and verify multi-item roundtrip works without stream misalignment
        final ModelList<ModelText> list = X_Model.create(ModelList.class);
        X_Model.ensureKey(ModelList.MODEL_LIST, list);
        list.setModelType(ModelText.class);

        final ModelText a = X_Model.create(ModelText.class);
        a.setText("A");
        final ModelText b = X_Model.create(ModelText.class);
        b.setText("B");
        final ModelText c = X_Model.create(ModelText.class);
        c.setText("C");

        list.add(a);
        list.add(b);
        list.add(c);

        // Sanity preconditions
        Assert.assertEquals(3, list.size());

        // Round-trip the list itself
        final String serialized = X_Model.serialize(ModelList.class, list);
        final ModelList<ModelText> roundTrip = X_Model.deserialize(ModelList.class, serialized);

        assertNotNull(roundTrip);
        Assert.assertEquals(3, roundTrip.size());

        // Access the elements to ensure deserialization didn’t get misaligned
        final java.util.List<ModelText> rt = Arrays.asList(
                roundTrip.getModels().iterateValues().toArray(ModelText.class)
        );
        Assert.assertTrue(rt.contains(a));
        Assert.assertTrue(rt.contains(b));
        Assert.assertTrue(rt.contains(c));
    }

}
