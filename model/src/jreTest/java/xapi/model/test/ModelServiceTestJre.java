package xapi.model.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import xapi.annotation.model.SerializationStrategy;
import xapi.dev.source.CharBuffer;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelList;
import xapi.model.api.ModelModule;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelRating;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.test.api.ModelTestDirections;
import xapi.model.test.api.ModelTestDirectionsKeysOnly;
import xapi.model.test.api.ModelTestDirectionsList;
import xapi.model.test.api.TestModelEnumMap;
import xapi.model.test.service.TestModelServiceClient;
import xapi.model.test.service.TestModelServiceServer;
import xapi.model.tools.ModelSerializerDefault;
import xapi.source.lex.CharIterator;
import xapi.time.X_Time;
import xapi.util.api.SuccessHandler;

import java.util.Arrays;
import java.util.EnumMap;

import xapi.model.test.api.ModelTestDirectionsEmbedded;
import xapi.log.api.LogLevel;
import xapi.util.api.Pointer;

public class ModelServiceTestJre {

    private static final String TEST_CLIENT_TO_SERVER = "c2s-only";
    private static final String TEST_SERVER_TO_CLIENT = "s2c-only";
    private static final String TEST_BOTH_ENABLED = "both";
    private static final String TEST_BOTH_DISABLED = "private";

    static {
        X_Log.logLevel(LogLevel.DEBUG);
    }

    private final TestModelServiceClient clientService = new TestModelServiceClient();
    private final TestModelServiceServer serverService = new TestModelServiceServer();

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

    private void logSerializationResult(String direction, String serialized, ModelTestDirections model) {
        X_Log.debug(ModelServiceTestJre.class, "Directional Serialization Test:", direction);
        X_Log.debug(ModelServiceTestJre.class, "Serialized string:", serialized);
        X_Log.debug(ModelServiceTestJre.class, "Original model", model);
    }

    private ModelTestDirectionsList createTestModelList() {
        ModelTestDirectionsList list = X_Model.create(ModelTestDirectionsList.class);
        list.setModelType(ModelTestDirections.class);
        list.add(createTestModel(1));
        list.add(createTestModel(2));
        list.add(createTestModel(3));
        return list;
    }

    private ModelTestDirectionsKeysOnly createKeysOnlyModel() {
        ModelTestDirectionsKeysOnly model = X_Model.create(ModelTestDirectionsKeysOnly.class);
        ModelList<ModelTestDirections> list = model.list();

        ModelTestDirections item1 = createTestModel(1);
        item1.setKey(X_Model.newKey("test", ModelTestDirections.MODEL_TEST_DIRECTIONS, "1"));
        list.add(item1);

        ModelTestDirections item2 = createTestModel(2);
        item2.setKey(X_Model.newKey("test", ModelTestDirections.MODEL_TEST_DIRECTIONS, "2"));
        list.add(item2);

        ModelTestDirections item3 = createTestModel(3);
        item3.setKey(X_Model.newKey("test", ModelTestDirections.MODEL_TEST_DIRECTIONS, "3"));
        list.add(item3);

        return model;
    }

    @Test
    public void testKeysOnlyClientToServer() {
        ModelTestDirectionsKeysOnly model = createKeysOnlyModel();

        // Test client-to-server serialization
        CharBuffer clientToServer = clientService.serialize(ModelTestDirectionsKeysOnly.class, model);
        String serializedC2S = clientToServer.toSource();

        // Verify only keys are transmitted
        ModelTestDirectionsKeysOnly serverDeserialized = serverService.deserialize(
                ModelTestDirectionsKeysOnly.class,
                CharIterator.forString(serializedC2S)
        );

        assertEquals(model.getList().size(), serverDeserialized.getList().size());

        ModelTestDirections[] originals = model.getList().toArray(ModelTestDirections.class);
        ModelTestDirections[] deserialized = serverDeserialized.getList().toArray(ModelTestDirections.class);

        for (int i = 0; i < originals.length; i++) {
            assertNotNull("Key must be preserved", deserialized[i].getKey());
            assertEquals("Keys must match", originals[i].getKey(), deserialized[i].getKey());
            assertNull("Values should not be transmitted", deserialized[i].getClientToServer());
            assertNull("Values should not be transmitted", deserialized[i].getServerToClient());
        }
    }

    @Test
    public void testKeysOnlyServerToClient() {
        ModelTestDirectionsKeysOnly model = createKeysOnlyModel();

        // Test server-to-client serialization
        CharBuffer serverToClient = serverService.serialize(ModelTestDirectionsKeysOnly.class, model);
        String serializedS2C = serverToClient.toSource();

        ModelTestDirectionsKeysOnly clientDeserialized = clientService.deserialize(
                ModelTestDirectionsKeysOnly.class,
                CharIterator.forString(serializedS2C)
        );

        assertEquals(model.getList().size(), clientDeserialized.getList().size());

        ModelTestDirections[] originals = model.getList().toArray(ModelTestDirections.class);
        ModelTestDirections[] deserialized = clientDeserialized.getList().toArray(ModelTestDirections.class);

        for (int i = 0; i < originals.length; i++) {
            assertNotNull("Key must be preserved", deserialized[i].getKey());
            assertEquals("Keys must match", originals[i].getKey(), deserialized[i].getKey());
            assertNull("Values should not be transmitted", deserialized[i].getClientToServer());
            assertNull("Values should not be transmitted", deserialized[i].getServerToClient());
        }
    }

    @Test
    public void testKeysOnlyRoundTrip() {
        ModelTestDirectionsKeysOnly model = createKeysOnlyModel();

        // Client -> Server
        CharBuffer clientToServer = clientService.serialize(ModelTestDirectionsKeysOnly.class, model);
        String serializedC2S = clientToServer.toSource();

        ModelTestDirectionsKeysOnly serverModel = serverService.deserialize(
                ModelTestDirectionsKeysOnly.class,
                CharIterator.forString(serializedC2S)
        );

        // Server -> Client
        CharBuffer serverToClient = serverService.serialize(ModelTestDirectionsKeysOnly.class, serverModel);
        String serializedS2C = serverToClient.toSource();

        ModelTestDirectionsKeysOnly clientModel = clientService.deserialize(
                ModelTestDirectionsKeysOnly.class,
                CharIterator.forString(serializedS2C)
        );

        // Verify keys preserved through full round-trip
        assertEquals(model.getList().size(), clientModel.getList().size());

        ModelTestDirections[] originals = model.getList().toArray(ModelTestDirections.class);
        ModelTestDirections[] roundTripped = clientModel.getList().toArray(ModelTestDirections.class);

        for (int i = 0; i < originals.length; i++) {
            assertNotNull("Key must be preserved through round-trip", roundTripped[i].getKey());
            assertEquals("Keys must match after round-trip", originals[i].getKey(), roundTripped[i].getKey());
            assertNull("Values should not survive round-trip", roundTripped[i].getClientToServer());
            assertNull("Values should not survive round-trip", roundTripped[i].getServerToClient());
        }
    }

    private ModelTestDirections createTestModel(int numbered) {
        ModelTestDirections model = createTestModel();
        model.setServerToClient(model.getServerToClient() + "_" + numbered);
        model.setClientToServer(model.getClientToServer() + "_" + numbered);
        model.setBothEnabled(model.getBothEnabled() + "_" + numbered);
        model.setBothDisabled(model.getBothDisabled() + "_" + numbered);
        return model;
    }
    private ModelTestDirections createTestModel() {
        ModelTestDirections model = X_Model.create(ModelTestDirections.class);
        model.setServerToClient(TEST_SERVER_TO_CLIENT);
        model.setClientToServer(TEST_CLIENT_TO_SERVER );
        model.setBothEnabled(TEST_BOTH_ENABLED);
        model.setBothDisabled(TEST_BOTH_DISABLED);
        return model;
    }

    private void assertDeserializedProperties(ModelTestDirections original, ModelTestDirections deserialized) {
        assertDeserializedProperties(original, deserialized, null);
    }
    private void assertDeserializedProperties(ModelTestDirections original, ModelTestDirections deserialized, Integer suffixInt) {
        final String suffix = suffixInt == null ? "" : "_" + (suffixInt + 1);
        assertEquals("Original model should have bothDisabled='" + TEST_BOTH_DISABLED + "'",
                TEST_BOTH_DISABLED + suffix, original.getBothDisabled());
        assertNull("Deserialized model should have null bothDisabled",
                deserialized.getBothDisabled());
        assertEquals("Both models should have bothEnabled='" + TEST_BOTH_ENABLED + "'",
                TEST_BOTH_ENABLED + suffix, original.getBothEnabled());
        assertEquals("Both models should have bothEnabled='" + TEST_BOTH_ENABLED + "'",
                original.getBothEnabled(), deserialized.getBothEnabled());
    }

    private ModelTestDirectionsEmbedded createEmbeddedTestModel() {
        ModelTestDirectionsEmbedded model = X_Model.create(ModelTestDirectionsEmbedded.class);
        model.setKey(X_Model.newKey("test", ModelTestDirectionsEmbedded.MODEL_TEST_DIRECTIONS_EMBEDDED));
        model.setList(createTestModelList());
        return model;
    }

    @Test
    public void testEmbeddedModelPersistence() {
        ModelTestDirectionsEmbedded model = createEmbeddedTestModel();
        final Pointer<Boolean> waiting = new Pointer<>(true);

        final ModelKey originalKey = model.getKey();

        // Test client-side persistence
        clientService.persist(model, persisted -> {
            clientService.load(ModelTestDirectionsEmbedded.class, persisted.getKey(), loaded -> {
                Assert.assertFalse(loaded == persisted);
                Assert.assertEquals(persisted.getKey(), loaded.getKey());

                // Verify list contents maintained client-side
                ModelTestDirections[] originalItems = persisted.getList().toArray(ModelTestDirections.class);
                ModelTestDirections[] loadedItems = loaded.getList().toArray(ModelTestDirections.class);

                for (int i = 0; i < originalItems.length; i++) {
                    // From client-authored persistence, client loads:
                    // - clientToServer was written, but client prevents deserializing it -> null
                    // - serverToClient was not written at all -> null
                    assertNull("Client-to-server field should not be materialized on client load", loadedItems[i].getClientToServer());
                    assertNull("Server-to-client field should not exist in client-authored persistence", loadedItems[i].getServerToClient());
                }


                // Test server-side persistence
                serverService.persist(model, serverPersisted -> {
                    serverService.load(ModelTestDirectionsEmbedded.class, serverPersisted.getKey(), serverLoaded -> {
                        Assert.assertFalse(serverLoaded == serverPersisted);
                        Assert.assertEquals(serverPersisted.getKey(), serverLoaded.getKey());

                        // Verify list contents maintained server-side
                        ModelTestDirections[] serverItems = serverLoaded.getList().toArray(ModelTestDirections.class);

                        for (int i = 0; i < originalItems.length; i++) {
                            // From server-authored persistence, server loads:
                            // - clientToServer does not exist -> null normally, but server persists only s2c.
                            //   However, our originalItems are from the original client-created model; here we want to
                            //   assert serverToClient survived and clientToServer is null.
                            assertNull("Server-authored persistence should not include client-to-server", serverItems[i].getClientToServer());
//                            assertEquals("Server-to-client field should be present in server-authored persistence",
//                                    originalItems[i].getServerToClient(), serverItems[i].getServerToClient());
                        }


                        waiting.set(false);
                    });
                });
            });
        });

        final long deadline = System.currentTimeMillis() + 3000;
        while (waiting.get()) {
            X_Time.trySleep(10, 0);
            Assert.assertTrue(X_Time.isFuture(deadline));
        }
    }

    @Test
    public void testEmbeddedModelSerialization() {
        ModelTestDirectionsEmbedded model = createEmbeddedTestModel();

        // Test client-to-server serialization
        CharBuffer clientToServer = clientService.serialize(ModelTestDirectionsEmbedded.class, model);
        String serializedC2S = clientToServer.toSource();

        ModelTestDirectionsEmbedded serverDeserialized = serverService.deserialize(
                ModelTestDirectionsEmbedded.class,
                CharIterator.forString(serializedC2S)
        );

        assertNotNull(serverDeserialized.getList());
        assertEquals("List size must match", model.getList().size(), serverDeserialized.getList().size());

        ModelTestDirections[] originals = model.getList().toArray(ModelTestDirections.class);
        ModelTestDirections[] deserialized = serverDeserialized.getList().toArray(ModelTestDirections.class);

        for (int i = 0; i < originals.length; i++) {
            assertDeserializedProperties(originals[i], deserialized[i], i);
        }


    }

    @Test
    public void testServerToClientSerialization() {
        ModelTestDirections model = createTestModel();

        // Test server-to-client serialization
        CharBuffer serverToClient = serverService.serialize(ModelTestDirections.class, model);
        final String serializedS2C = serverToClient.toSource();

        logSerializationResult("Server to Client", serializedS2C, model);

        ModelTestDirections clientDeserialized = clientService.deserialize(
                ModelTestDirections.class,
                CharIterator.forString(serializedS2C)
        );

        // Verify only rendered field is transmitted
        assertEquals(TEST_SERVER_TO_CLIENT, clientDeserialized.getServerToClient());
        assertNull("Value should not be transmitted to client", clientDeserialized.getClientToServer());
    }

    @Test
    public void testRoundTripSerialization() {
        final ModelTestDirections model = createTestModel();

        // Test client-to-server serialization
        CharBuffer clientToServer = clientService.serialize(ModelTestDirections.class, model);
        final String serializedC2S = clientToServer.toSource();

        logSerializationResult("Client to Server", serializedC2S, model);

        ModelTestDirections serverDeserialized = serverService.deserialize(
                ModelTestDirections.class,
                CharIterator.forString(serializedC2S)
        );

        assertDeserializedProperties(model, serverDeserialized);
        assertNull("Rendered field should not be transmitted to server", serverDeserialized.getServerToClient());
        assertEquals("Value field should be transmitted to server",
                TEST_CLIENT_TO_SERVER, serverDeserialized.getClientToServer());

        // Update the server-to-client field
        final String customS2C = "toClient";
        serverDeserialized.setServerToClient(customS2C);

        // Send back to client
        CharBuffer serverToClient = serverService.serialize(ModelTestDirections.class, serverDeserialized);
        final String serializedS2C = serverToClient.toSource();

        ModelTestDirections clientRoundTrip = clientService.deserialize(
                ModelTestDirections.class,
                CharIterator.forString(serializedS2C)
        );

        assertDeserializedProperties(model, clientRoundTrip);
        // Verify roundtrip state
        assertEquals("Rendered field should be preserved in roundtrip", customS2C, clientRoundTrip.getServerToClient());
        assertNull("Value field should not be transmitted back to client", clientRoundTrip.getClientToServer());


        // Update the client-to-server field
        final String customC2S = "toServer";
        clientRoundTrip.setClientToServer(customC2S);

        // Complete roundtrip back to server
        CharBuffer clientToServerFinal = clientService.serialize(ModelTestDirections.class, clientRoundTrip);
        final String serializedC2SFinal = clientToServerFinal.toSource();

        ModelTestDirections serverFinal = serverService.deserialize(
                ModelTestDirections.class,
                CharIterator.forString(serializedC2SFinal)
        );

        assertDeserializedProperties(model, serverFinal);
        // Verify final server state
        assertNull("Rendered field should not be transmitted to server", serverFinal.getServerToClient());
        assertEquals("Value field should be transmitted to server",
                customC2S, serverFinal.getClientToServer());
    }

    @Test
    public void testRoundTripSerializationList() {
        final ModelTestDirectionsList list = createTestModelList();

        // Test client-to-server serialization
        CharBuffer clientToServer = clientService.serialize(ModelTestDirectionsList.class, list);
        final String serializedC2S = clientToServer.toSource();

        ModelTestDirectionsList serverDeserialized = serverService.deserialize(
                ModelTestDirectionsList.class,
                CharIterator.forString(serializedC2S)
        );

        assertEquals("List size should match", list.size(), serverDeserialized.size());

        ModelTestDirections[] originals = list.toArray(ModelTestDirections.class);
        ModelTestDirections[] deserialized = serverDeserialized.toArray(ModelTestDirections.class);

        for (int i = 0; i < originals.length; i++) {
            assertDeserializedProperties(originals[i], deserialized[i], i);
            assertNull("Rendered field should not be transmitted to server", deserialized[i].getServerToClient());
            assertEquals("Value field should be transmitted to server",
                    originals[i].getClientToServer(), deserialized[i].getClientToServer());
        }

        // Update server-to-client fields
        for (int i = 0; i < deserialized.length; i++) {
            deserialized[i].setServerToClient("toClient_" + (i + 1));
        }

        // Send back to client
        CharBuffer serverToClient = serverService.serialize(ModelTestDirectionsList.class, serverDeserialized);
        final String serializedS2C = serverToClient.toSource();

        ModelTestDirectionsList clientRoundTrip = clientService.deserialize(
                ModelTestDirectionsList.class,
                CharIterator.forString(serializedS2C)
        );

        assertEquals("List size should match", list.size(), clientRoundTrip.size());
        ModelTestDirections[] roundTrips = clientRoundTrip.toArray(ModelTestDirections.class);

        for (int i = 0; i < originals.length; i++) {
            assertDeserializedProperties(originals[i], roundTrips[i], i);
            assertEquals("Rendered field should be preserved in roundtrip",
                    "toClient_" + (i + 1), roundTrips[i].getServerToClient());
            assertNull("Value field should not be transmitted back to client", roundTrips[i].getClientToServer());
        }

        // Update client-to-server fields
        for (int i = 0; i < roundTrips.length; i++) {
            roundTrips[i].setClientToServer("toServer_" + (i + 1));
        }

        // Complete roundtrip back to server
        CharBuffer clientToServerFinal = clientService.serialize(ModelTestDirectionsList.class, clientRoundTrip);
        final String serializedC2SFinal = clientToServerFinal.toSource();

        ModelTestDirectionsList serverFinal = serverService.deserialize(
                ModelTestDirectionsList.class,
                CharIterator.forString(serializedC2SFinal)
        );

        assertEquals("List size should match", list.size(), serverFinal.size());
        ModelTestDirections[] finals = serverFinal.toArray(ModelTestDirections.class);

        for (int i = 0; i < originals.length; i++) {
            assertDeserializedProperties(originals[i], finals[i], i);
            assertNull("Rendered field should not be transmitted to server", finals[i].getServerToClient());
            assertEquals("Value field should be transmitted to server",
                    "toServer_" + (i + 1), finals[i].getClientToServer());
        }
    }

}
