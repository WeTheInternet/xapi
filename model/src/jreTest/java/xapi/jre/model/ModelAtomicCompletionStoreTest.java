package xapi.jre.model;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Tests exact-byte prerequisites, multi-record atomicity and receipts using real local files. */
public class ModelAtomicCompletionStoreTest {

  private interface Action {
    void run() throws Exception;
  }

  private static ModelStoreKey key(final String id) {
    return new ModelStoreKey(id, "models/" + id);
  }

  private static byte[] bytes(final String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static ModelTransaction tx(final String operation, final ModelMutation... mutations) {
    return new ModelTransaction("complete-test-v1", "schema-v1", "actor", operation,
        Arrays.asList(mutations));
  }

  private static ModelReadCondition condition(final FileModelTransactionStore store,
      final ModelStoreKey key) throws IOException {
    return new ModelReadCondition(key, store.read(key));
  }

  private static void legacy(final FileModelTransactionStore store, final ModelStoreKey key,
      final String value) throws Exception {
    store.legacyWriteScope(key, () -> {
      final Path path = store.directory().resolve(key.relativePath());
      Files.createDirectories(path.getParent());
      Files.write(path, bytes(value));
      return null;
    });
  }

  private static void conflict(final Action action) throws Exception {
    try {
      action.run();
      fail("Expected a conditional transaction conflict");
    } catch (final FileModelTransactionStore.Conflict expected) {
      // The rejected transaction must leave both mutations and receipt untouched.
    }
  }

  @Test
  public void revisionZeroImageChangeRejectsBothWrites() throws Exception {
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("atomic-image"))) {
      final ModelStoreKey live = key("live");
      final ModelStoreKey history = key("history");
      legacy(store, live, "ACTIVE");
      final ModelReadCondition before = condition(store, live);
      final ModelReadCondition missing = condition(store, history);

      // Ordinary legacy writes retain revision zero; byte equality must still fail.
      legacy(store, live, "OTHER-ACTIVE");
      conflict(() -> store.commitIfUnchanged(tx("a",
          new ModelMutation(live, 0, bytes("FINISHED")),
          new ModelMutation(history, -1, bytes("HISTORY"))), Arrays.asList(before, missing)));

      assertEquals("OTHER-ACTIVE", new String(store.read(live).value(), StandardCharsets.UTF_8));
      assertFalse(store.read(history).exists());
      assertNull(store.receipt("actor", "a"));
    }
  }

  @Test
  public void changedPrerequisiteOrOptionalAbsenceRejects() throws Exception {
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("atomic-prerequisite"))) {
      final ModelStoreKey live = key("live");
      final ModelStoreKey history = key("history");
      final ModelStoreKey child = key("child");
      legacy(store, live, "ACTIVE");
      final List<ModelReadCondition> conditions = Arrays.asList(
          condition(store, live), condition(store, history), condition(store, child));

      // The child is a read-only prerequisite, but a new child invalidates the batch.
      legacy(store, child, "ACTIVE");
      conflict(() -> store.commitIfUnchanged(tx("a",
          new ModelMutation(live, 0, bytes("FINISHED")),
          new ModelMutation(history, -1, bytes("HISTORY"))), conditions));

      assertEquals("ACTIVE", new String(store.read(live).value(), StandardCharsets.UTF_8));
      assertFalse(store.read(history).exists());
      assertNull(store.receipt("actor", "a"));
    }
  }

  @Test
  public void identicalReceiptWinsOverObsoleteConditionsAcrossRestart() throws Exception {
    final Path root = Files.createTempDirectory("atomic-retry");
    final ModelTransaction transaction = tx("same",
        new ModelMutation(key("live"), -1, bytes("FINISHED")),
        new ModelMutation(key("history"), -1, bytes("TIME123")));
    final ModelTransactionReceipt first;
    final List<ModelReadCondition> conditions;
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      conditions = Arrays.asList(condition(store, key("live")), condition(store, key("history")));
      first = store.commitIfUnchanged(transaction, conditions);
      assertTrue(store.isProtected(key("live")));
      conflict(() -> legacy(store, key("live"), "ACTIVE"));
    }

    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final ModelTransactionReceipt retry = store.commitIfUnchanged(transaction, conditions);
      assertEquals(first.fingerprint(), retry.fingerprint());
      assertEquals(first.revision(), retry.revision());
      conflict(() -> store.commitIfUnchanged(tx("same",
          new ModelMutation(key("live"), -1, bytes("DIFFERENT"))), conditions));
    }
  }

  @Test
  public void concurrentConditionalBundlesHaveOneWholeWinner() throws Exception {
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("atomic-concurrent"))) {
      final List<ModelReadCondition> conditions = Arrays.asList(
          condition(store, key("live")), condition(store, key("history")));
      final AtomicReference<Throwable> unexpected = new AtomicReference<>();
      final AtomicInteger wins = new AtomicInteger();
      final AtomicInteger conflicts = new AtomicInteger();
      final List<Thread> threads = new ArrayList<>();
      for (final String id : new String[]{"a", "b"}) {
        final Thread worker = new Thread(() -> {
          try {
            store.commitIfUnchanged(tx(id,
                new ModelMutation(key("live"), -1, bytes(id)),
                new ModelMutation(key("history"), -1, bytes(id))), conditions);
            wins.incrementAndGet();
          } catch (final FileModelTransactionStore.Conflict expected) {
            conflicts.incrementAndGet();
          } catch (final Throwable failure) {
            unexpected.set(failure);
          }
        });
        threads.add(worker);
        worker.start();
      }
      for (final Thread worker : threads) {
        worker.join(5000);
        assertFalse("Concurrent writer timed out", worker.isAlive());
      }

      assertNull(unexpected.get());
      assertEquals(1, wins.get());
      assertEquals(1, conflicts.get());
      assertArrayEquals(store.read(key("live")).value(), store.read(key("history")).value());
    }
  }

  @Test
  public void missingCoverageAliasesAndProtectedMissingFailClosed() throws Exception {
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("atomic-invalid"))) {
      final ModelStoreKey live = key("live");
      final ModelTransaction transaction = tx("a", new ModelMutation(live, -1, bytes("F")));
      try {
        store.commitIfUnchanged(transaction, Collections.emptyList());
        fail("Missing condition coverage must be rejected");
      } catch (final IOException expected) {
        // Every mutated record requires an exact read condition.
      }

      final ModelReadCondition absent = condition(store, live);
      final ModelStoreKey alias = new ModelStoreKey("alias", live.relativePath());
      try {
        store.commitIfUnchanged(transaction, Arrays.asList(absent,
            new ModelReadCondition(alias, store.read(live))));
        fail("Aliased filesystem identities must be rejected");
      } catch (final IOException expected) {
        // Different canonical keys cannot share one relative path.
      }

      store.commitIfUnchanged(transaction, Collections.singletonList(absent));
      Files.delete(store.directory().resolve(live.relativePath()));
      try {
        store.commitIfUnchanged(tx("b", new ModelMutation(live, 1, bytes("F"))),
            Collections.singletonList(absent));
        fail("Missing protected records must not be recreated implicitly");
      } catch (final IOException expected) {
        // A deleted protected record is a store integrity error, not absence.
      }
      assertFalse(Files.exists(store.directory().resolve(live.relativePath())));
    }
  }

  @Test
  public void concurrentIdenticalOperationReturnsSameReceipt() throws Exception {
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("atomic-same-op"))) {
      final List<ModelReadCondition> conditions = Arrays.asList(
          condition(store, key("live")), condition(store, key("history")));
      final ModelTransaction transaction = tx("same",
          new ModelMutation(key("live"), -1, bytes("FINISHED")),
          new ModelMutation(key("history"), -1, bytes("TIME123")));
      final AtomicReference<ModelTransactionReceipt> first = new AtomicReference<>();
      final AtomicReference<ModelTransactionReceipt> second = new AtomicReference<>();
      final AtomicReference<Throwable> error = new AtomicReference<>();
      final Thread a = new Thread(() -> {
        try { first.set(store.commitIfUnchanged(transaction, conditions)); }
        catch (final Throwable failure) { error.set(failure); }
      });
      final Thread b = new Thread(() -> {
        try { second.set(store.commitIfUnchanged(transaction, conditions)); }
        catch (final Throwable failure) { error.set(failure); }
      });
      a.start();
      b.start();
      a.join(5000);
      b.join(5000);

      assertFalse(a.isAlive());
      assertFalse(b.isAlive());
      assertNull(error.get());
      assertEquals(first.get().fingerprint(), second.get().fingerprint());
      assertEquals(first.get().revision(), second.get().revision());
    }
  }

  @Test
  public void publishedUncertainOutcomeRecoversOriginalReceiptWithoutNewPreparation() throws Exception {
    final Path root = Files.createTempDirectory("atomic-uncertain");
    final AtomicBoolean injected = new AtomicBoolean();
    final ModelTransaction transaction = tx("uncertain",
        new ModelMutation(key("live"), -1, bytes("FINISHED")),
        new ModelMutation(key("history"), -1, bytes("ORIGINAL-TIME")));
    final List<ModelReadCondition> conditions;

    try (FileModelTransactionStore store = new FileModelTransactionStore(root,
        (boundary, child) -> {
          if (boundary == FileModelTransactionStore.Boundary.COMMITTED
              && injected.compareAndSet(false, true)) {
            throw new IOException("injected after publication");
          }
        })) {
      conditions = Arrays.asList(condition(store, key("live")), condition(store, key("history")));
      try {
        store.commitIfUnchanged(transaction, conditions);
        fail("A fault after publication must require recovery");
      } catch (final FileModelTransactionStore.RecoveryRequired expected) {
        // The redo decision is durable, but the outcome is not yet readable.
      }
      try {
        store.read(key("live"));
        fail("Reads must remain blocked until recovery");
      } catch (final FileModelTransactionStore.RecoveryRequired expected) {
        // No other operation may run while recovery is pending.
      }
    }

    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final ModelTransactionReceipt receipt = store.commitIfUnchanged(transaction, conditions);
      assertEquals(1, receipt.revision());
      assertArrayEquals(bytes("ORIGINAL-TIME"), store.read(key("history")).value());
      assertEquals(receipt.fingerprint(), store.receipt("actor", "uncertain").fingerprint());
    }
  }

  @Test
  public void excessiveConditionCountAndAggregateImageAreRejectedBeforeCommit() throws Exception {
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("atomic-condition-limits"))) {
      final List<ModelReadCondition> tooMany = new ArrayList<>();
      for (int i = 0; i <= FileModelTransactionStore.MAX_MUTATIONS; i++) {
        tooMany.add(condition(store, key("key" + i)));
      }
      final ModelTransaction transaction = tx("bounded",
          new ModelMutation(key("key0"), -1, bytes("value")));
      try {
        store.commitIfUnchanged(transaction, tooMany);
        fail("Excess condition count must be rejected");
      } catch (final IOException expected) {
        // No receipt or mutation is permitted.
      }

      final byte[] halfPlusOne = new byte[FileModelTransactionStore.MAX_REQUEST_BYTES / 2 + 1];
      final List<ModelReadCondition> oversized = Arrays.asList(
          new ModelReadCondition(key("key0"), new ModelStoreRecord(halfPlusOne, 0)),
          new ModelReadCondition(key("extra"), new ModelStoreRecord(halfPlusOne, 0)));
      try {
        store.commitIfUnchanged(transaction, oversized);
        fail("Excess aggregate read-image bytes must be rejected");
      } catch (final IOException expected) {
        // Both condition images count against the request budget.
      }

      assertNull(store.receipt("actor", "bounded"));
      assertFalse(store.read(key("key0")).exists());
    }
  }
}
