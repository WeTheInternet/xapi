package xapi.jre.model;

import org.junit.Test;
import xapi.annotation.model.IsModel;
import xapi.model.api.*;
import xapi.model.impl.ModelKeyDefault;
import xapi.source.lex.StringCharIterator;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

public class UntypedModelCursorTest {
  @IsModel(modelType = "a") public interface A extends Model { String getValue(); A setValue(String value); }
  @IsModel(modelType = "b") public interface B extends Model { String getValue(); B setValue(String value); }

  private static final class Service extends ModelServiceJre {
    final List<Runnable> reads = new ArrayList<>();
    @Override protected void dispatchFileRead(final Runnable operation) { reads.add(operation); }
    void drain() { while (!reads.isEmpty()) reads.remove(0).run(); }
  }
  private static final class Result<T> implements SuccessHandler<T>, ErrorHandler<Throwable> {
    T value; Throwable error; int callbacks;
    @Override public void onSuccess(final T value) { this.value = value; callbacks++; }
    @Override public void onError(final Throwable error) { this.error = error; callbacks++; }
    T success() { assertEquals(1, callbacks); if (error != null) throw new AssertionError(error); return value; }
  }
  private static final class Fixture implements AutoCloseable {
    final Path root = Files.createTempDirectory("xapi-untyped-cursor-");
    final FileModelTransactionStore store;
    final Service service = new Service();
    Fixture(final boolean attached) throws Exception {
      store = attached ? new FileModelTransactionStore(root) : null;
      if (attached) service.attachTransactionStore(store); else service.setRootDir(root.toFile());
      service.create(A.class); service.create(B.class);
    }
    void records(final String type, final String... ids) {
      for (final String id : ids) {
        final Model model = "a".equals(type) ? service.create(A.class) : service.create(B.class);
        model.setKey(new ModelKeyDefault("", type, id)); model.setProperty("value", id);
        final Result<Model> saved = new Result<>(); service.persist(model, saved); saved.success();
      }
    }
    Result<ModelQueryResult<Model>> page(final String cursor, final int limit) {
      final Result<ModelQueryResult<Model>> result = new Result<>();
      service.query(new ModelQuery<Model>().setCursor(cursor).setLimit(limit), result); service.drain(); return result;
    }
    List<String> complete(final int limit) {
      final List<String> result = new ArrayList<>(); final Set<String> cursors = new HashSet<>(); String cursor = null;
      for (int page = 0; page < 30; page++) {
        final ModelQueryResult<Model> batch = page(cursor, limit).success();
        assertTrue(batch.getSize() <= limit);
        for (final Model model : batch.getModelList()) result.add(model.getKey().getKind() + "/" + model.getKey().getId());
        cursor = batch.getCursor();
        if (cursor == null) return result;
        assertTrue("cursor must advance", cursors.add(cursor));
      }
      throw new AssertionError("Pagination did not terminate");
    }
    @Override public void close() throws IOException { if (store != null) store.close(); }
  }

  @Test public void laterTypeNamesBeforeAnEarlierFilenameAreNeverDropped() throws Exception {
    for (boolean attached : Arrays.asList(false, true)) try (Fixture fixture = new Fixture(attached)) {
      fixture.records("a", "x", "y", "z"); fixture.records("b", "m", "n");
      assertEquals(Arrays.asList("a/x", "a/y", "a/z", "b/m", "b/n"), fixture.complete(2));
    }
  }

  @Test public void exactTypeBoundaryResumesAtNextTypeInsteadOfRepeatingForever() throws Exception {
    for (boolean attached : Arrays.asList(false, true)) try (Fixture fixture = new Fixture(attached)) {
      fixture.records("a", "x", "y"); fixture.records("b", "m");
      final ModelQueryResult<Model> first = fixture.page(null, 2).success();
      final UntypedModelCursor cursor = UntypedModelCursor.decode(first.getCursor());
      assertEquals("b", cursor.type); assertEquals("m", cursor.record);
      assertEquals(Arrays.asList("a/x", "a/y", "b/m"), fixture.complete(2));
      final ModelQueryResult<Model> last = fixture.page(first.getCursor(), 2).success(); assertEquals(1, last.getSize()); assertNull(last.getCursor());
    }
  }

  @Test public void untypedResultWireCarriesEachTypeAndPreservesPageCursor() throws Exception {
    for (boolean attached : Arrays.asList(false, true)) try (Fixture fixture = new Fixture(attached)) {
      fixture.records("a", "same", "z"); fixture.records("b", "same", "y");
      final ModelQueryResult<Model> first = fixture.page(null, 3).success();
      assertEquals(3, first.getSize()); assertNotNull(first.getCursor());
      assertUntypedWire(fixture, first);
      final ModelQueryResult<Model> last = fixture.page(first.getCursor(), 3).success();
      assertEquals(1, last.getSize()); assertNull(last.getCursor());
      assertUntypedWire(fixture, last);
    }
  }

  private static void assertUntypedWire(final Fixture fixture, final ModelQueryResult<Model> result) {
    final PrimitiveSerializer primitives = fixture.service.primitiveSerializer();
    final StringCharIterator wire = new StringCharIterator(result.serialize(fixture.service, primitives));
    assertEquals(result.getCursor(), primitives.deserializeString(wire));
    assertEquals(result.getSize(), primitives.deserializeInt(wire));
    for (final Model expected : result.getModelList()) {
      final String kind = primitives.deserializeString(wire);
      assertEquals(expected.getType(), kind);
      final Model decoded = fixture.service.deserialize(fixture.service.typeToClass(kind), wire);
      assertEquals(fixture.service.keyToString(expected.getKey()), fixture.service.keyToString(decoded.getKey()));
      assertEquals((String) expected.getProperty("value"), (String) decoded.getProperty("value"));
    }
    assertFalse(wire.hasNext());
  }

  @Test public void typedResultWireStillOmitsPerRecordTypeNames() throws Exception {
    for (boolean attached : Arrays.asList(false, true)) try (Fixture fixture = new Fixture(attached)) {
      fixture.records("a", "x", "y"); fixture.records("b", "other");
      final Result<ModelQueryResult<A>> callback = new Result<>();
      fixture.service.query(A.class, new ModelQuery<A>().setPageSize(1), callback); fixture.service.drain();
      final ModelQueryResult<A> result = callback.success();
      final PrimitiveSerializer primitives = fixture.service.primitiveSerializer();
      final StringCharIterator wire = new StringCharIterator(result.serialize(fixture.service, primitives));
      assertEquals("y", primitives.deserializeString(wire));
      assertEquals(1, primitives.deserializeInt(wire));
      final A decoded = fixture.service.deserialize(A.class, wire);
      assertEquals("x", decoded.getKey().getId()); assertEquals("x", decoded.getValue());
      assertFalse(wire.hasNext());
    }
  }

  @Test public void equalIdsEmptyIntermediateTypesAndLimitOneHaveFiniteCompleteCoverage() throws Exception {
    for (boolean attached : Arrays.asList(false, true)) try (Fixture fixture = new Fixture(attached)) {
      fixture.records("a", "same", "é:[]"); fixture.records("b", "same"); Files.createDirectory(fixture.root.resolve("aa"));
      assertEquals(Arrays.asList("a/same", "a/é:[]", "b/same"), fixture.complete(1));
    }
  }

  @Test public void emptyAndSinglePageResultsHaveNullTerminalCursor() throws Exception {
    for (boolean attached : Arrays.asList(false, true)) try (Fixture fixture = new Fixture(attached)) {
      final ModelQueryResult<Model> empty = fixture.page(null, 1).success(); assertEquals(0, empty.getSize()); assertNull(empty.getCursor());
      assertUntypedWire(fixture, empty);
      fixture.records("a", "x"); final ModelQueryResult<Model> single = fixture.page(null, 1).success(); assertEquals(1, single.getSize()); assertNull(single.getCursor());
    }
  }

  @Test public void malformedAndLegacyBareCursorsFailExplicitlyRatherThanResetting() throws Exception {
    final List<String> bad = Arrays.asList("", "x", "uq0:YQ:eA", "uq1:", "uq1:YQ:", "uq1:YQ:eA:extra", "uq1:***:eA", "uq1:Li4:eA", "uq1:YQ==:eA", "uq1:_w:eA");
    for (boolean attached : Arrays.asList(false, true)) try (Fixture fixture = new Fixture(attached)) {
      fixture.records("a", "x");
      for (final String cursor : bad) {
        final Result<ModelQueryResult<Model>> result = fixture.page(cursor, 1);
        assertEquals(1, result.callbacks); assertNull(result.value); assertTrue(cursor, result.error instanceof ModelQueryFailureException);
        assertTrue(result.error.getCause() instanceof IOException);
      }
      final Result<ModelQueryResult<Model>> zero = fixture.page(null, 0); assertTrue(zero.error instanceof ModelQueryFailureException);
    }
  }

  @Test public void typedFilenameCursorSemanticsRemainUnchanged() throws Exception {
    for (boolean attached : Arrays.asList(false, true)) try (Fixture fixture = new Fixture(attached)) {
      fixture.records("a", "x", "y", "z"); final Result<ModelQueryResult<A>> result = new Result<>();
      fixture.service.query(A.class, new ModelQuery<A>().setCursor("y").setPageSize(1), result); fixture.service.drain();
      assertEquals("y", result.success().getModelList().iterator().next().getKey().getId()); assertEquals("z", result.value.getCursor());
    }
  }

  @Test public void unattachedLinuxBackslashBasenameRemainsReadableAcrossPageBoundaries() throws Exception {
    try (Fixture fixture = new Fixture(false)) {
      fixture.records("a", "x", "z\\legacy"); fixture.records("b", "m");
      final Result<A> load = new Result<>();
      fixture.service.load(A.class, new ModelKeyDefault("", "a", "z\\legacy"), load); fixture.service.drain();
      assertEquals("z\\legacy", load.success().getKey().getId());
      assertEquals(Arrays.asList("a/x", "a/z\\legacy", "b/m"), fixture.complete(1));
    }
  }
}
