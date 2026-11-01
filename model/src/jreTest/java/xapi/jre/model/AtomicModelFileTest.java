package xapi.jre.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class AtomicModelFileTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private Path target() throws IOException {
    return folder.newFolder().toPath().resolve("model");
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static String read(Path path) throws IOException {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  @Test public void replacesExistingBytesAndPreservesPosixIdentityAndMode() throws Exception {
    Path path = target();
    Files.write(path, bytes("old"));
    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r-----"));
    PosixFileAttributes original = Files.readAttributes(path, PosixFileAttributes.class);
    new AtomicModelFile().replace(path, bytes("new — UTF-8"));
    PosixFileAttributes updated = Files.readAttributes(path, PosixFileAttributes.class);
    assertEquals("new — UTF-8", read(path));
    assertEquals(original.permissions(), updated.permissions());
    assertEquals(original.owner(), updated.owner());
    assertEquals(original.group(), updated.group());
    assertEquals(1, path.getParent().toFile().list().length);
  }

  @Test public void createsPrivateRecordAndDurablyCreatesNestedParents() throws Exception {
    Path path = folder.getRoot().toPath().resolve("a/b/c/model");
    AtomicReference<Boolean> replaced = new AtomicReference<>(false);
    new AtomicModelFile() {
      @Override void beforeReplace(Path temporary, Path target) throws IOException {
        assertFalse(Files.exists(target));
        replaced.set(true);
      }
    }.replace(path, bytes("new"));
    assertTrue(replaced.get());
    assertEquals("new", read(path));
    assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(path));
  }

  @Test public void partialWriteCannotDamageExistingRecord() throws Exception {
    assertPrecommitFailure(new AtomicModelFile() {
      @Override void write(FileChannel channel, byte[] serialized) throws IOException {
        super.write(channel, bytes("partial"));
        throw new IOException("injected partial write");
      }
    });
  }

  @Test public void fileForceFailureCannotDamageExistingRecord() throws Exception {
    assertPrecommitFailure(new AtomicModelFile() {
      @Override void forceFile(FileChannel channel) throws IOException {
        throw new IOException("injected file force");
      }
    });
  }

  @Test public void failureAfterCloseBeforeReplaceKeepsOldRecord() throws Exception {
    assertPrecommitFailure(new AtomicModelFile() {
      @Override void beforeReplace(Path temporary, Path target) throws IOException {
        assertEquals("new", read(temporary));
        assertEquals("old", read(target));
        assertFalse(AtomicModelFile.isModelFile(temporary.toFile()));
        throw new IOException("injected before replace");
      }
    });
  }

  @Test public void unsupportedAtomicMoveDoesNotFallBack() throws Exception {
    assertPrecommitFailure(new AtomicModelFile() {
      @Override void atomicReplace(Path temporary, Path target) throws IOException {
        throw new AtomicMoveNotSupportedException(temporary.toString(), target.toString(), "injected");
      }
    });
  }

  @Test public void failureAfterReplacementReportsUncertainWithNewBytesVisible() throws Exception {
    Path path = target();
    Files.write(path, bytes("old"));
    try {
      new AtomicModelFile() {
        @Override void atomicReplace(Path temporary, Path target) throws IOException {
          super.atomicReplace(temporary, target);
          throw new IOException("injected lost replacement acknowledgement");
        }
      }.replace(path, bytes("new"));
      fail("must report uncertain");
    } catch (ModelPersistenceException expected) {
      assertTrue(expected.isCommitUncertain());
      assertEquals("new", read(path));
    }
  }

  @Test public void finalDirectoryForceFailureReportsUncertain() throws Exception {
    Path path = target();
    Files.write(path, bytes("old"));
    try {
      new AtomicModelFile() {
        private boolean moved;
        @Override void atomicReplace(Path temporary, Path target) throws IOException {
          super.atomicReplace(temporary, target);
          moved = true;
        }
        @Override void forceDirectory(Path directory) throws IOException {
          if (moved) throw new IOException("injected directory force");
          super.forceDirectory(directory);
        }
      }.replace(path, bytes("new"));
      fail("must report uncertain");
    } catch (ModelPersistenceException expected) {
      assertEquals("new", read(path));
    }
  }

  @Test public void ancestorForceFailureBeforeReplacementKeepsOldRecord() throws Exception {
    assertPrecommitFailure(new AtomicModelFile() {
      @Override void forceDirectory(Path directory) throws IOException {
        throw new IOException("injected ancestor force");
      }
    });
  }

  @Test public void symlinkTargetIsRejectedWithoutTouchingItsReferent() throws Exception {
    Path referent = target();
    Files.write(referent, bytes("old"));
    Path link = referent.resolveSibling("link");
    Files.createSymbolicLink(link, referent);
    try {
      new AtomicModelFile().replace(link, bytes("new"));
      fail("symlink target must be rejected");
    } catch (IOException expected) {
      assertEquals("old", read(referent));
      assertTrue(Files.isSymbolicLink(link));
    }
  }

  @Test public void reservedTempsAreNotModelsOrValidWriteTargets() throws Exception {
    Path path = target().resolveSibling(AtomicModelFile.TEMP_PREFIX + "abandoned" + AtomicModelFile.TEMP_SUFFIX);
    Files.write(path, bytes("partial"));
    assertFalse(AtomicModelFile.isModelFile(path.toFile()));
    assertTrue(AtomicModelFile.isTemporary(path.toFile()));
    try {
      new AtomicModelFile().replace(path, bytes("new"));
      fail("reserved name must fail");
    } catch (IOException expected) {
      assertEquals("partial", read(path));
    }
  }

  @Test public void concurrentReadersNeverObserveHalfWrittenBytes() throws Exception {
    Path path = target();
    byte[] old = new byte[128 * 1024];
    byte[] next = new byte[old.length];
    Arrays.fill(old, (byte) 'A');
    Arrays.fill(next, (byte) 'B');
    Files.write(path, old);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread reader = new Thread(() -> {
      try {
        for (int i = 0; i < 500; i++) {
          byte[] value = Files.readAllBytes(path);
          assertTrue(Arrays.equals(old, value) || Arrays.equals(next, value));
        }
      } catch (Throwable problem) {
        failure.set(problem);
      }
    });
    reader.start();
    for (int i = 0; i < 25; i++) new AtomicModelFile().replace(path, i % 2 == 0 ? next : old);
    reader.join();
    if (failure.get() != null) throw new AssertionError(failure.get());
  }

  @Test public void processHaltBeforeRenameLeavesOldRecordAndInvisibleTemp() throws Exception {
    assertProcessHalt("before", 73, "old");
  }

  @Test public void processHaltAfterRenameLeavesCompleteNewRecord() throws Exception {
    assertProcessHalt("after", 74, "new");
  }

  private void assertProcessHalt(String phase, int exit, String value) throws Exception {
    Path path = target();
    Files.write(path, bytes("old"));
    Process child = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-cp",
        System.getProperty("java.class.path"), AtomicModelFileTest.class.getName(), phase, path.toString())
        .redirectErrorStream(true).redirectOutput(folder.newFile()).start();
    assertEquals(exit, child.waitFor());
    assertEquals(value, read(path));
    assertEquals(1, path.getParent().toFile().listFiles(AtomicModelFile::isModelFile).length);
  }

  public static void main(String[] args) throws Exception {
    new AtomicModelFile() {
      @Override void beforeReplace(Path temporary, Path target) {
        if ("before".equals(args[0])) Runtime.getRuntime().halt(73);
      }
      @Override void atomicReplace(Path temporary, Path target) throws IOException {
        super.atomicReplace(temporary, target);
        if ("after".equals(args[0])) Runtime.getRuntime().halt(74);
      }
    }.replace(java.nio.file.Paths.get(args[1]), bytes("new"));
  }

  private void assertPrecommitFailure(AtomicModelFile writer) throws Exception {
    Path path = target();
    Files.write(path, bytes("old"));
    try {
      writer.replace(path, bytes("new"));
      fail("injected operation must fail");
    } catch (IOException expected) {
      assertFalse(expected instanceof ModelPersistenceException);
      assertEquals("old", read(path));
      assertEquals(1, path.getParent().toFile().list().length);
    }
  }
}
