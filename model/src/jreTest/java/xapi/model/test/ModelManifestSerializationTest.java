package xapi.model.test;

import org.junit.Test;
import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.IsModel;
import xapi.annotation.model.KeyOnly;
import xapi.annotation.model.Serializable;
import xapi.dev.source.CharBuffer;
import xapi.jre.model.ModelServiceJre;
import xapi.model.api.*;
import xapi.model.content.HasAuthor;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.test.api.ModelTestDirections;
import xapi.model.test.api.ModelTestDirectionsEmbedded;
import xapi.model.tools.ModelSerializerDefault;
import xapi.model.tools.PrimitiveSerializerDefault;
import xapi.source.lex.StringCharIterator;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class ModelManifestSerializationTest {

    // Captured from the pre-versioned writer at dc259a6347603e73b60d758ce3b74c50c14d988f.
    // This fixture really lacks generic metadata; reading it must not reconstruct a new schema.
    private static final String LEGACY_MODULE = "Plegacy-generics-v0EFlegacy-genericsKLtestDirModhTxapi.model.test.api.ModelTestDirectionsWserverToClientWclientToServerCbothEnabledUbothDisabledGjava.lang.StringHcontent\\xapi.model.content.ModelContentItextItimeHupvotesDdownvotesSauthorHrelatedRchildrenDpermaLinkSdoubletT[Lxapi.model.content.ModelRating;Zxapi.model.user.ModelUserQxapi.model.api.ModelListAETIAOINASNATEEAEAE1T1OSNTEEEAEAE1T1ISNOTEEAEAE1T1NSNEEEEAEAE1T1HRRDLCUMWFGDSNOTEE1EAE1T1LYNOTEE1EAE1T1CPNOTEE1EAE1T1UPNOTEE1EAE1T1MBNOTEE1EAE1T1WVNOTEE1EAE1T1FVNOTEE1EAE1T1GSNOTEE1EAE1T1";
    private static final String LEGACY_TEXT_MANIFEST = "Itext[xapi.model.content.ModelTextAItextItimeItextGjava.lang.StringNOTEE1EAE1T1ItimeSdoubleNOTEE1EAE1T1";

    private final PrimitiveSerializer primitives = new PrimitiveSerializerDefault();

    private static class ImportedService extends ModelServiceJre {
        private final ModelModule module;

        ImportedService(final String artifact) {
            module = ModelSerializerDefault.deserializeModule(artifact);
            for (final ModelManifest manifest : module.getManifests()) {
                register(manifest.getModelType());
                registerManifest(manifest);
            }
        }

        @Override
        public ModelModule getModelModule() {
            return module;
        }
    }

    private ModelModule module() {
        final ModelModule module = new ModelModule();
        module.setModuleName("generic-manifest-test");
        module.setUuid("generic-manifest-test-v1");
        module.addManifest(ModelUtil.createManifest(ModelContent.class));
        module.addManifest(ModelUtil.createManifest(ModelText.class));
        module.addManifest(ModelUtil.createManifest(ModelList.class));
        module.addManifest(ModelUtil.createManifest(ModelTestDirections.class));
        // Prove import uses the exported order, not another reflection pass.
        Collections.reverse(Arrays.asList(module.getManifest("content").getPropertyNames()));
        return module;
    }

    private String serialize(final ModelManifest manifest) {
        return ModelManifest.serialize(new CharBuffer(), manifest, primitives).toSource();
    }

    private ModelManifest deserialize(final String serialized) {
        final StringCharIterator input = new StringCharIterator(serialized);
        final ModelManifest manifest = ModelManifest.deserialize(input, primitives);
        assertFalse("Manifest decoder must consume exactly the manifest", input.hasNext());
        return manifest;
    }

    private int formatMarker(final String serialized) {
        final StringCharIterator input = new StringCharIterator(serialized);
        primitives.deserializeString(input);
        primitives.deserializeClass(input);
        return primitives.deserializeInt(input);
    }

    @Test
    public void readsFixedLegacyModuleWithoutInventingGenericMetadata() {
        final StringCharIterator input = new StringCharIterator(LEGACY_MODULE);
        final ModelModule loaded = ModelSerializerDefault.deserialize(input, primitives);
        assertFalse(input.hasNext());
        assertEquals("legacy-generics", loaded.getModuleName());
        final ModelManifest content = loaded.getManifest("content");
        assertArrayEquals(new String[]{"text", "time", "upvotes", "downvotes", "author", "related", "children", "permaLink"},
                content.getPropertyNames());
        assertEquals(ModelList.class, content.getMethodData("children").getType());
        assertNull(content.getMethodData("children").getTypeParams());
        assertFalse(content.isKeyOnly("author"));
        assertFalse(content.isKeyOnly("related"));
        assertFalse(content.isAutoSave("related"));
        assertDirections(loaded.getManifest(ModelTestDirections.MODEL_TEST_DIRECTIONS));
        assertEquals(LEGACY_MODULE, ModelSerializerDefault.serialize(loaded));
    }

    @Test
    public void keepsGenericArtifactsInExistingFormatWithoutReferencePolicies() {
        final ModelManifest authored = ModelUtil.createManifest(ModelTestDirectionsEmbedded.class);
        assertFalse(authored.isKeyOnly("list"));
        assertFalse(authored.isAutoSave("list"));
        final String encoded = serialize(authored);
        assertEquals(-1, formatMarker(encoded));
        final ModelManifest imported = deserialize(encoded);
        assertArrayEquals(new Class<?>[]{ModelTestDirections.class},
                imported.getMethodData("list").getTypeParams());
        assertTrue(imported.isClientToServerEncrypted("list"));
        assertTrue(imported.isServerToClientEncrypted("list"));
        assertFalse(imported.isKeyOnly("list"));
        assertFalse(imported.isAutoSave("list"));
        assertEquals(encoded, serialize(imported));
    }

    private interface NongenericReferences extends Model, HasAuthor {
        @xapi.annotation.model.KeyOnly(autoSave = true)
        ModelText getContent();
        ModelText getInline();
    }

    @Test
    public void referencePolicyVersionPreservesNongenericFlagsAndParameterStates() {
        final ModelManifest authored = ModelUtil.createManifest(NongenericReferences.class);
        authored.getMethodData("author").setTypeParams((Class<?>[]) null);
        authored.getMethodData("content").setTypeParams(new Class<?>[0]);
        final String encoded = serialize(authored);
        assertEquals(-2, formatMarker(encoded));
        final ModelManifest imported = deserialize(encoded);
        assertTrue(imported.isKeyOnly("author"));
        assertFalse(imported.isAutoSave("author"));
        assertTrue(imported.isKeyOnly("content"));
        assertTrue(imported.isAutoSave("content"));
        assertFalse(imported.isKeyOnly("inline"));
        assertFalse(imported.isAutoSave("inline"));
        assertNull(imported.getMethodData("author").getTypeParams());
        assertArrayEquals(new Class<?>[0], imported.getMethodData("content").getTypeParams());
        assertEquals(encoded, serialize(imported));
    }

    @Test
    public void keepsNongenericArtifactsByteCompatible() {
        final ModelManifest manifest = ModelUtil.createManifest(ModelText.class);
        assertEquals(LEGACY_TEXT_MANIFEST, serialize(manifest));
        assertEquals(LEGACY_TEXT_MANIFEST, serialize(deserialize(LEGACY_TEXT_MANIFEST)));
    }

    @Test
    public void modulePreservesConcreteParametersOrderAndPolicies() throws Exception {
        final ModelModule authored = module();
        // Exercise differing policies inside the versioned manifest itself.
        authored.getManifest("content").getMethodData("children").addAnnotations(
                ModelTestDirections.class.getMethod("getServerToClient").getAnnotations());
        authored.getManifest("content").getMethodData("related").addAnnotations(
                ModelTestDirections.class.getMethod("getClientToServer").getAnnotations());
        final ModelModule imported = ModelSerializerDefault.deserializeModule(ModelSerializerDefault.serialize(authored));
        for (final ModelManifest expected : authored.getManifests()) {
            final ModelManifest actual = imported.getManifest(expected.getType());
            assertArrayEquals(expected.getPropertyNames(), actual.getPropertyNames());
            for (final String property : expected.getPropertyNames()) {
                final Class<?>[] parameters = expected.getMethodData(property).getTypeParams();
                // Legacy encoding intentionally keeps nongeneric manifests unchanged.
                if (parameters != null && parameters.length > 0) {
                    assertArrayEquals(property, parameters, actual.getMethodData(property).getTypeParams());
                }
                assertEquals(expected.getMethodData(property), actual.getMethodData(property));
                assertEquals(expected.isKeyOnly(property), actual.isKeyOnly(property));
                assertEquals(expected.isAutoSave(property), actual.isAutoSave(property));
                assertEquals(expected.getClientToServerSerializationStrategy(property), actual.getClientToServerSerializationStrategy(property));
                assertEquals(expected.getServerToClientSerializationStrategy(property), actual.getServerToClientSerializationStrategy(property));
            }
        }
        assertArrayEquals(new Class<?>[]{ModelContent.class}, imported.getManifest("content").getMethodData("children").getTypeParams());
        assertTrue(imported.getManifest("content").isServerToClientEnabled("children"));
        assertFalse(imported.getManifest("content").isClientToServerEnabled("children"));
        assertFalse(imported.getManifest("content").isServerToClientEnabled("related"));
        assertTrue(imported.getManifest("content").isClientToServerEnabled("related"));
        assertArrayEquals(new Class<?>[]{ModelKey.class, Model.class}, imported.getManifest("list").getMethodData("models").getTypeParams());
        assertDirections(imported.getManifest(ModelTestDirections.MODEL_TEST_DIRECTIONS));
    }

    @Test
    public void copiesReferencePolicyAndIndependentDeclarationsWithoutWireEncoding() {
        final ModelManifest authored = ModelUtil.createManifest(ModelContent.class);
        final ModelManifest.MethodData related = authored.getMethodData("related");
        related.setIdField("related");
        final String[] order = authored.getPropertyNames().clone();
        final ModelManifest copy = authored.copy();
        final ModelManifest.MethodData copiedRelated = copy.getMethodData("related");

        assertTrue(copy.isKeyOnly("related"));
        assertTrue(copy.isAutoSave("related"));
        assertNotSame(related, copiedRelated);
        assertSame(copiedRelated, copy.getMethodData("getRelated"));
        assertSame(copiedRelated, copy.getMethodData("setRelated"));
        assertNotSame(authored.getPropertyNames(), copy.getPropertyNames());
        assertNotSame(related.getTypeParams(), copiedRelated.getTypeParams());
        assertArrayEquals(new Class<?>[]{ModelContent.class}, copiedRelated.getTypeParams());

        authored.getPropertyNames()[0] = "changed";
        related.getTypeParams()[0] = ModelText.class;
        related.setType(ModelText.class);
        related.setIdField("changed");
        assertArrayEquals(order, copy.getPropertyNames());
        assertEquals(ModelList.class, copiedRelated.getType());
        assertArrayEquals(new Class<?>[]{ModelContent.class}, copiedRelated.getTypeParams());
        // The copied custom id-field rule and both method indexes remain independent.
        copy.addProperty("related", "related", null, null, null);
        assertEquals(ModelMethodType.ID, copy.getMethodType("related"));
        assertTrue(copy.hasSeenMethod("related"));
        assertFalse(authored.hasSeenMethod("related"));
        assertNull(authored.getMethodType("related"));
        assertSame(copiedRelated, copy.getMethodData("related"));
    }

    @KeyOnly
    @IsModel(modelType = "copy-defaults",
        serializable = @Serializable(clientToServer = @ClientToServer(enabled = false)))
    private interface CopyDefaults extends Model {}

    @Test
    public void copiesRootPoliciesAndDefaultsForNewProperties() {
        final ModelManifest authored = new ModelManifest(CopyDefaults.class);
        final ModelManifest copy = authored.copy();
        assertEquals("copy-defaults", copy.getType());
        assertEquals(CopyDefaults.class, copy.getModelType());
        assertTrue(copy.isKeyOnly());
        copy.addProperty("getValue", "id", null, null, null).setType(String.class);
        assertFalse(copy.isClientToServerEnabled("value"));
        assertTrue(copy.isServerToClientEnabled("value"));
        assertTrue(copy.hasSeenMethod("getValue"));
        assertFalse(authored.hasSeenMethod("getValue"));
        assertArrayEquals(new String[]{"value"}, copy.getPropertyNames());
        authored.addProperty("getOriginal", "id", null, null, null).setType(String.class);
        assertArrayEquals(new String[]{"original"}, authored.getPropertyNames());
        assertArrayEquals(new String[]{"value"}, copy.getPropertyNames());
    }

    private void assertDirections(final ModelManifest manifest) {
        assertTrue(manifest.isServerToClientEnabled("serverToClient"));
        assertFalse(manifest.isClientToServerEnabled("serverToClient"));
        assertFalse(manifest.isServerToClientEnabled("clientToServer"));
        assertTrue(manifest.isClientToServerEnabled("clientToServer"));
        assertTrue(manifest.isClientToServerEnabled("bothEnabled"));
        assertTrue(manifest.isServerToClientEnabled("bothEnabled"));
        assertFalse(manifest.isClientToServerEnabled("bothDisabled"));
        assertFalse(manifest.isServerToClientEnabled("bothDisabled"));
    }

    @Test
    public void versionedManifestPreservesNullAndEmptyParameterArrays() {
        final ModelManifest authored = ModelUtil.createManifest(ModelContent.class);
        authored.getMethodData("text").setTypeParams((Class<?>[]) null);
        authored.getMethodData("time").setTypeParams(new Class<?>[0]);
        final String encoded = serialize(authored);
        assertEquals(-2, formatMarker(encoded));
        final ModelManifest imported = deserialize(encoded);
        assertTrue(imported.isKeyOnly("related"));
        assertTrue(imported.isAutoSave("related"));
        assertNull(imported.getMethodData("text").getTypeParams());
        assertArrayEquals(new Class<?>[0], imported.getMethodData("time").getTypeParams());
        assertArrayEquals(new Class<?>[]{ModelContent.class}, imported.getMethodData("children").getTypeParams());
    }

    @Test
    public void importedModuleDecodesRealModelListChildren() {
        final String artifact = ModelSerializerDefault.serialize(module());
        final ImportedService sender = new ImportedService(artifact);
        final ImportedService receiver = new ImportedService(artifact);
        final ModelContent parent = sender.create(ModelContent.class);
        parent.setKey(sender.newKey("family", "content", "parent"));
        parent.setText("Packing \u2713\r\n");
        final ModelContent child = sender.create(ModelContent.class);
        child.setKey(parent.getKey().getChild("content", "child"));
        child.setText("Toothbrush \u00e9");
        final ModelList<ModelContent> children = sender.create(ModelList.class);
        children.setModelType(ModelContent.class);
        children.setKey(parent.getKey().getChild("list", "children"));
        children.add(child);
        parent.setChildren(children);

        final String serialized = sender.serialize(ModelContent.class, parent).toSource();
        final StringCharIterator input = new StringCharIterator(serialized);
        final ModelContent loaded = receiver.deserialize(ModelContent.class, input);
        assertFalse(input.hasNext());
        assertEquals(parent.getText(), loaded.getText());
        assertEquals(1, loaded.getChildren().size());
        assertEquals(ModelContent.class, loaded.getChildren().getModelType());
        assertEquals(child.getText(), loaded.getChildren().first().getText());
        assertEquals(sender.keyToString(child.getKey()), receiver.keyToString(loaded.getChildren().first().getKey()));
        assertEquals(sender.keyToString(parent.getKey()), receiver.keyToString(loaded.getKey()));
        assertEquals(serialized, receiver.serialize(ModelContent.class, loaded).toSource());
    }

    @Test
    public void rejectsUnknownVersionsAndInvalidCounts() {
        final String header = primitives.serializeString("content") + primitives.serializeClass(ModelContent.class);
        assertMalformed(header + primitives.serializeInt(-3), "Unsupported model manifest format");
        for (final int version : new int[]{-1, -2}) {
            assertMalformed(header + primitives.serializeInt(version) + primitives.serializeInt(-1),
                    "Invalid model property count");
            final String property = header + primitives.serializeInt(version) + primitives.serializeInt(1)
                    + primitives.serializeString("children") + primitives.serializeString("children")
                    + primitives.serializeClass(ModelList.class);
            assertMalformed(property + primitives.serializeInt(-2), "Invalid model type parameter count");
        }
    }

    @Test
    public void rejectsInvalidReferencePolicyMasksAndFlagCounts() {
        final String property = primitives.serializeString("content") + primitives.serializeClass(ModelContent.class)
                + primitives.serializeInt(-2) + primitives.serializeInt(1)
                + primitives.serializeString("children") + primitives.serializeString("children")
                + primitives.serializeClass(ModelList.class) + primitives.serializeInt(0);
        final String policies = primitives.serializeBooleanArray(true, true, false, false, false);
        for (final int invalid : new int[]{-1, 4}) {
            assertMalformed(property + policies + primitives.serializeInt(invalid),
                    "Invalid model reference policy flags");
        }
        assertMalformed(property + primitives.serializeBooleanArray(true, true, false, false),
                "Invalid model policy flag count");
    }

    private void assertMalformed(final String serialized, final String message) {
        try {
            deserialize(serialized);
            fail("Malformed manifest was accepted");
        } catch (final IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith(message));
        }
    }
}
