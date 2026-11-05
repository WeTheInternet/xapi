package xapi.jre.model;

import org.junit.Test;
import xapi.model.api.Model;
import xapi.model.api.ModelCreateResult;
import xapi.model.api.ModelManifest;
import xapi.model.content.ModelText;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** Deferred root-declaration capture; this does not freeze referenced model registries. */
public class ModelCapturedPropertyTypeTest {
    private static final class Service extends ModelServiceJre {
        final List<Runnable> pending = new ArrayList<>();
        int ordinaryWrites;

        @Override protected void dispatchFileOperation(final Runnable operation) {
            pending.add(operation);
        }

        @Override protected <M extends Model> void doPersist(final String type, final M model,
                                                            final SuccessHandler<M> callback) {
            ordinaryWrites++;
            super.doPersist(type, model, callback);
        }

        void drain() {
            while (!pending.isEmpty()) pending.remove(0).run();
        }
    }

    private static final class Result<T> implements SuccessHandler<T>, ErrorHandler<Throwable> {
        T value;
        Throwable failure;
        int calls;

        @Override public void onSuccess(final T value) { this.value = value; calls++; }
        @Override public void onError(final Throwable failure) { this.failure = failure; calls++; }

        T success() {
            assertEquals(1, calls);
            if (failure != null) throw new AssertionError(failure);
            assertNotNull(value);
            return value;
        }
    }

    private static ModelText text(final Service service, final String value, final double time) {
        final ModelText model = service.create(ModelText.class);
        model.setKey(service.newKey("owner", "text", "progress"));
        model.setText(value);
        model.setTime(time);
        return model;
    }

    @Test public void pendingCreationDecodesAcknowledgmentWithCapturedRootPropertyType() throws Exception {
        assertCapturedRootType(false);
    }

    @Test public void pendingExistingResultUsesCapturedRootTypeWithoutReplacingStoredImage() throws Exception {
        assertCapturedRootType(true);
    }

    private static void assertCapturedRootType(final boolean existing) throws Exception {
        final Path root = Files.createTempDirectory("xapi-captured-property-type-");
        try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
            final Service service = new Service();
            service.attachTransactionStore(store);
            final Path target = root.resolve("owner/text/progress");
            final String storedText = "stored root text \u65e5\u672c";
            final String proposedText = "captured root text \u65e5\u672c \ud83c\udf31";
            if (existing) {
                final Result<ModelText> saved = new Result<>();
                service.persist(text(service, storedText, 73.5), saved);
                saved.success();
            }
            final ModelText proposed = text(service, proposedText, 42.25);
            final ModelManifest declaration = service.findManifest(ModelText.class);
            final ModelManifest.MethodData textProperty = declaration.getMethodData("text");
            final Class<?> originalType = textProperty.getType();
            assertSame(String.class, originalType);
            final byte[] expectedBytes = existing ? Files.readAllBytes(target)
                    : service.serialize(declaration, proposed).toSource().getBytes(StandardCharsets.UTF_8);
            final Result<ModelCreateResult<ModelText>> outcome = new Result<>();
            service.createIfAbsent(declaration, proposed, outcome);
            assertEquals(0, outcome.calls);
            assertEquals(1, service.pending.size());
            assertEquals(existing, Files.exists(target));

            // Capture has finished, but the file operation and its acknowledgment are pending.
            // The proxy factory still uses this registered declaration when decoding later.
            textProperty.setType(double.class);
            try {
                service.drain();
                final ModelCreateResult<ModelText> result = outcome.success();
                final ModelText acknowledged = result.getModel();
                assertEquals(!existing, result.isCreated());
                assertNotSame(proposed, acknowledged);
                assertEquals(existing ? storedText : proposedText, acknowledged.getText());
                assertEquals(existing ? 73.5 : 42.25, acknowledged.getTime(), 0);
                assertEquals(service.keyToString(proposed.getKey()), service.keyToString(acknowledged.getKey()));
                assertEquals(proposedText, proposed.getText());
                assertArrayEquals(expectedBytes, Files.readAllBytes(target));
                assertSame(double.class, textProperty.getType());
                assertEquals(existing ? 1 : 0, service.ordinaryWrites);
                assertTrue(service.pending.isEmpty());
            } finally {
                textProperty.setType(originalType);
            }

            // The chosen image remains readable by the ordinary provider after restoring
            // its live schema; the operation did not rewrite the stored field representation.
            final Result<ModelText> loaded = new Result<>();
            service.load(ModelText.class, proposed.getKey(), loaded);
            service.drain();
            assertEquals(existing ? storedText : proposedText, loaded.success().getText());
            assertArrayEquals(expectedBytes, Files.readAllBytes(target));
        }
    }
}
