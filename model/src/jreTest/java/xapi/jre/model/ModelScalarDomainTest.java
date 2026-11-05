package xapi.jre.model;

import org.junit.Test;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringTo;
import xapi.dev.source.CharBuffer;
import xapi.fu.data.ListLike;
import xapi.fu.data.SetLike;
import xapi.fu.java.X_Jdk;
import xapi.model.api.Model;
import xapi.model.api.ModelDeserializationContext;
import xapi.model.api.ModelSerializationContext;
import xapi.model.tools.ModelSerializerDefault;
import xapi.model.tools.PrimitiveSerializerDefault;
import xapi.source.lex.StringCharIterator;

import java.util.EnumMap;

import static org.junit.Assert.*;

/** Scalar and collection domains reject invalid values while preserving valid framing. */
public class ModelScalarDomainTest {
    private final PrimitiveSerializerDefault primitives = new PrimitiveSerializerDefault();
    private final ScalarCodec codec = new ScalarCodec(primitives);

    private static final class ScalarCodec extends ModelSerializerDefault<Model> {
        private final PrimitiveSerializerDefault primitives;
        private final ModelServiceJre service = new ModelServiceJre();

        ScalarCodec(final PrimitiveSerializerDefault primitives) {
            this.primitives = primitives;
        }

        String encode(final Class<?> type, final Object value) {
            final CharBuffer out = new CharBuffer();
            final ModelSerializationContext context = new ModelSerializationContext(out, service, null);
            writeObject(out, "value", type, value, primitives, context);
            return out.toSource();
        }

        Object read(final Class<?> type, final StringCharIterator input) {
            if (type.isPrimitive()) {
                return readPrimitive(type, input, primitives);
            }
            final ModelDeserializationContext context = new ModelDeserializationContext(null, service, null);
            return readObject(type, "value", input, primitives, context);
        }

        Object decode(final Class<?> type, final String wire) {
            final StringCharIterator input = new StringCharIterator(wire);
            final Object value = read(type, input);
            assertFalse("Scalar decoder left trailing input", input.hasNext());
            return value;
        }
    }

    private void assertRoundTrip(final Class<?> type, final Object value) {
        assertEquals(value, codec.decode(type, codec.encode(type, value)));
    }

    private static void assertInvalid(final String type, final Runnable decode) {
        try {
            decode.run();
            fail("Out-of-domain " + type + " was accepted");
        } catch (final IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().startsWith("Invalid encoded " + type + ":"));
        }
    }

    @Test public void primitiveNarrowBoundariesKeepTheirIntEncoding() {
        for (final byte value : new byte[]{Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE}) {
            assertEquals(primitives.serializeByte(value), codec.encode(byte.class, value));
            assertRoundTrip(byte.class, value);
        }
        for (final short value : new short[]{Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE}) {
            assertEquals(primitives.serializeShort(value), codec.encode(short.class, value));
            assertRoundTrip(short.class, value);
        }
        for (final char value : new char[]{Character.MIN_VALUE, '\t', 'A', 0x8000, Character.MAX_VALUE}) {
            assertEquals(primitives.serializeChar(value), codec.encode(char.class, value));
            assertRoundTrip(char.class, value);
        }
        assertEquals("e4", codec.encode(byte.class, Byte.MIN_VALUE));
        assertEquals("eee1", codec.encode(short.class, Short.MIN_VALUE));
        assertEquals("E", codec.encode(char.class, Character.MIN_VALUE));
    }

    @Test public void primitiveNarrowingRejectsValuesOutsideTheTargetDomain() {
        for (final int value : new int[]{Byte.MIN_VALUE - 1, Byte.MAX_VALUE + 1, 256,
                Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertInvalid("byte", () -> codec.decode(byte.class, primitives.serializeInt(value)));
        }
        for (final int value : new int[]{Short.MIN_VALUE - 1, Short.MAX_VALUE + 1, 65536,
                Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertInvalid("short", () -> codec.decode(short.class, primitives.serializeInt(value)));
        }
        for (final int value : new int[]{-1, Character.MAX_VALUE + 1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertInvalid("char", () -> codec.decode(char.class, primitives.serializeInt(value)));
        }
    }

    @Test public void boxedIntegralBoundariesKeepTheirLongEncoding() {
        for (final int value : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            assertEquals(primitives.serializeLong(value), codec.encode(Integer.class, value));
            assertRoundTrip(Integer.class, value);
        }
        for (final short value : new short[]{Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE}) {
            assertEquals(primitives.serializeLong(value), codec.encode(Short.class, value));
            assertRoundTrip(Short.class, value);
        }
        for (final byte value : new byte[]{Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE}) {
            assertEquals(primitives.serializeLong(value), codec.encode(Byte.class, value));
            assertRoundTrip(Byte.class, value);
        }
        // Boxed Integer.MIN_VALUE uses a long magnitude, not the primitive int NUL sentinel.
        assertEquals("eeeeee2", codec.encode(Integer.class, Integer.MIN_VALUE));
        assertEquals("\0", codec.encode(int.class, Integer.MIN_VALUE));
        assertEquals(Integer.valueOf(0), codec.decode(Integer.class, codec.encode(Integer.class, null)));
        assertEquals(Short.valueOf((short) 0), codec.decode(Short.class, codec.encode(Short.class, null)));
        assertEquals(Byte.valueOf((byte) 0), codec.decode(Byte.class, codec.encode(Byte.class, null)));
    }

    @Test public void boxedIntegralNarrowingRejectsValuesOutsideTheTargetDomain() {
        for (final long value : new long[]{(long) Integer.MIN_VALUE - 1, (long) Integer.MAX_VALUE + 1,
                1L << 32, Long.MIN_VALUE, Long.MAX_VALUE}) {
            assertInvalid("Integer", () -> codec.decode(Integer.class, primitives.serializeLong(value)));
        }
        for (final long value : new long[]{Short.MIN_VALUE - 1L, Short.MAX_VALUE + 1L,
                65536, Long.MIN_VALUE, Long.MAX_VALUE}) {
            assertInvalid("Short", () -> codec.decode(Short.class, primitives.serializeLong(value)));
        }
        for (final long value : new long[]{Byte.MIN_VALUE - 1L, Byte.MAX_VALUE + 1L,
                256, Long.MIN_VALUE, Long.MAX_VALUE}) {
            assertInvalid("Byte", () -> codec.decode(Byte.class, primitives.serializeLong(value)));
        }
    }

    @Test public void booleansKeepDistinctPrimitiveAndNullableBoxedEncodings() {
        assertEquals("0", codec.encode(boolean.class, false));
        assertEquals("1", codec.encode(boolean.class, true));
        assertRoundTrip(boolean.class, false);
        assertRoundTrip(boolean.class, true);
        assertEquals("1", codec.encode(Boolean.class, null));
        assertEquals("E", codec.encode(Boolean.class, false));
        assertEquals("T", codec.encode(Boolean.class, true));
        assertRoundTrip(Boolean.class, null);
        assertRoundTrip(Boolean.class, false);
        assertRoundTrip(Boolean.class, true);
    }

    @Test public void booleansRejectOtherLiteralsAndNumericValues() {
        for (final String wire : new String[]{" ", "\n", "\0", "E", "T", "2", "x"}) {
            assertInvalid("boolean", () -> codec.decode(boolean.class, wire));
        }
        for (final int value : new int[]{Integer.MIN_VALUE, -2, 2, 42, Integer.MAX_VALUE}) {
            assertInvalid("Boolean", () -> codec.decode(Boolean.class, primitives.serializeInt(value)));
        }
    }

    @Test public void packedBooleanArrayGoldenFramesConsumeTheirWholePayload() {
        // The redundant inner size and terminal chunk are existing writer bytes.
        assertBooleanArrayFrame(null, "1");
        assertBooleanArrayFrame(new boolean[0], "EEE");
        assertBooleanArrayFrame(new boolean[]{false}, "TTE");
        assertBooleanArrayFrame(new boolean[]{true}, "TTT");
        assertBooleanArrayFrame(new boolean[]{true, false}, "AAT");
        assertBooleanArrayFrame(new boolean[]{true, false, true}, "OON");
        assertBooleanArrayFrame(new boolean[]{true, false, true, false}, "IIN");
        assertBooleanArrayFrame(new boolean[]{true, false, true, false, true}, "NNKL");
    }

    @Test public void largerPackedBooleanArrayGoldenFramesRetainEveryValue() {
        assertBooleanArrayFrame(new boolean[]{true, false, true, false, true, true}, "SSKT");
        assertBooleanArrayFrame(new boolean[]{true, false, true, false, true,
                false, true, false, true, false}, "LLKLE");
        assertBooleanArrayFrame(new boolean[]{true, false, true, false, true,
                false, true, false, true, false, true}, "CCKLT");
        assertBooleanArrayFrame(new boolean[]{true, false, true, false, true,
                false, true, false, true, false, true, false, true, false, false, true}, "GGKLNT");
    }

    private void assertBooleanArrayFrame(final boolean[] value, final String golden) {
        assertEquals(golden, codec.encode(boolean[].class, value));
        final StringCharIterator input = new StringCharIterator(golden + "T");
        assertArrayEquals(value, (boolean[]) codec.read(boolean[].class, input));
        assertEquals("The following scalar must remain framed", 1, primitives.deserializeInt(input));
        assertFalse(input.hasNext());
    }

    @Test public void packedBooleanArrayRejectsMismatchedLengths() {
        // Outer one/inner empty; outer empty/inner one; outer two/inner one.
        for (final String wire : new String[]{"TEE", "ETE", "ATE"}) {
            assertInvalid("boolean array", () -> codec.decode(boolean[].class, wire));
        }
    }

    @Test public void packedBooleanArrayRejectsMasksOutsideFiveBits() {
        // Each pair is the primitive frame and the same frame with the model's outer size.
        // Check data chunks, the empty array's chunk, and consumed five/ten-value padding.
        final String[][] malformed = {
                {"T1", "TT1"}, {"TeT", "TTeT"},
                {"E1", "EE1"}, {"EeT", "EEeT"},
                {"NK1", "NNK1"}, {"NKeT", "NNKeT"},
                {"LKL1", "LLKL1"}, {"LKLeT", "LLKLeT"}
        };
        for (final String[] frame : malformed) {
            assertInvalid("boolean array", () ->
                    primitives.deserializeBooleanArray(new StringCharIterator(frame[0])));
            assertInvalid("boolean array", () -> codec.decode(boolean[].class, frame[1]));
        }
    }

    @Test public void packedBooleanArrayRetainsValidUnusedBitsAndPadding() {
        // Mask 31 is valid even when some or all of its bits are unused.
        assertArrayEquals(new boolean[0],
                primitives.deserializeBooleanArray(new StringCharIterator("E\\")));
        assertArrayEquals(new boolean[0], (boolean[]) codec.decode(boolean[].class, "EE\\"));
        assertArrayEquals(new boolean[]{true},
                primitives.deserializeBooleanArray(new StringCharIterator("T\\")));
        assertArrayEquals(new boolean[]{true}, (boolean[]) codec.decode(boolean[].class, "TT\\"));
        final boolean[] allTrue = {true, true, true, true, true};
        final StringCharIterator input = new StringCharIterator("N\\\\T");
        assertArrayEquals(allTrue, primitives.deserializeBooleanArray(input));
        assertEquals("The following scalar must remain framed", 1, primitives.deserializeInt(input));
        assertFalse(input.hasNext());
        assertArrayEquals(allTrue, (boolean[]) codec.decode(boolean[].class, "NN\\\\"));
    }

    @Test public void narrowPrimitiveArrayLayoutRemainsUnchanged() {
        final byte[] value = new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE};
        final String wire = primitives.serializeInt(value.length) + "e4E`O";
        assertEquals(wire, codec.encode(byte[].class, value));
        assertArrayEquals(value, (byte[]) codec.decode(byte[].class, wire));
        assertInvalid("byte", () -> codec.decode(byte[].class,
                primitives.serializeInt(1) + primitives.serializeInt(256)));
    }

    private enum CollectionKey { VALUE }

    private Object readCollectionFrame(final Class<?> type, final String wire) {
        final StringCharIterator input = new StringCharIterator(wire + "T");
        final Object value = codec.read(type, input);
        assertEquals("The following scalar must remain framed", 1, primitives.deserializeInt(input));
        assertFalse(input.hasNext());
        return value;
    }

    @Test public void collectionsRejectCountsBelowTheirNullSentinel() {
        final String stringClass = primitives.serializeClass(String.class);
        final Class<?>[] types = {ObjectTo.class, SetLike.class, StringTo.class, EnumMap.class};
        final String[] domains = {"iterable count", "sized iterable count", "string map count", "enum map count"};
        final String[] prefixes = {"", "", stringClass, ""};
        final String[] suffixes = {stringClass + stringClass, "", "", primitives.serializeClass(CollectionKey.class)};
        for (int index = 0; index < types.length; index++) {
            final int i = index;
            for (final int count : new int[]{-2, -31, Integer.MIN_VALUE}) {
                final StringCharIterator input = new StringCharIterator(
                        prefixes[i] + primitives.serializeInt(count) + suffixes[i] + "T");
                assertInvalid(domains[i], () -> codec.read(types[i], input));
                assertEquals("Reject at the count without consuming later fields", suffixes[i] + "T", input.consumeAll().toString());
            }
        }
    }

    @Test public void collectionCountsPreserveNullEmptyAndFollowingFields() {
        final String property = "xapiModelNullForEmpty";
        final String previous = System.getProperty(property);
        try {
            System.setProperty(property, "false");
            assertNull(readCollectionFrame(ObjectTo.class, "1"));
            assertNull(readCollectionFrame(SetLike.class, "1"));
            assertNull(readCollectionFrame(EnumMap.class, "1"));
            assertNull(readCollectionFrame(StringTo.class, primitives.serializeClass(void.class)));
            // Preserve the reader's existing typed-null form as well as the writer's void marker.
            final String stringClass = primitives.serializeClass(String.class);
            assertNull(readCollectionFrame(StringTo.class, stringClass + "1"));
            assertEquals(0, ((ObjectTo<?, ?>) readCollectionFrame(ObjectTo.class,
                    "E" + stringClass + stringClass)).size());
            assertEquals(0, ((SetLike<?>) readCollectionFrame(SetLike.class, "E")).size());
            assertEquals(0, ((StringTo<?>) readCollectionFrame(StringTo.class, stringClass + "E")).size());
            assertEquals(0, ((EnumMap<?, ?>) readCollectionFrame(EnumMap.class,
                    "E" + primitives.serializeClass(CollectionKey.class))).size());
            assertEquals("1", codec.encode(SetLike.class, null));
            assertEquals("E", codec.encode(SetLike.class, X_Jdk.setLinked()));
            System.setProperty(property, "true");
            assertEquals("1", codec.encode(SetLike.class, X_Jdk.setLinked()));
            assertNull(readCollectionFrame(SetLike.class, "E"));
            assertNull(readCollectionFrame(ObjectTo.class, "E"));
        } finally {
            if (previous == null) System.clearProperty(property);
            else System.setProperty(property, previous);
        }
    }

    @Test public void collectionEntriesPreserveTheirTypeFrames() {
        final String stringClass = primitives.serializeClass(String.class);
        final String key = primitives.serializeString("key");
        final String value = primitives.serializeString("value");
        final ObjectTo<?, ?> objects = (ObjectTo<?, ?>) readCollectionFrame(ObjectTo.class,
                "T" + stringClass + stringClass + key + value);
        assertEquals(1, objects.size());
        assertEquals("value", objects.get("key"));
        final StringTo<?> strings = (StringTo<?>) readCollectionFrame(StringTo.class,
                stringClass + "T" + key + value);
        assertEquals(1, strings.size());
        assertEquals("value", strings.get("key"));
        final EnumMap<?, ?> enums = (EnumMap<?, ?>) readCollectionFrame(EnumMap.class,
                "T" + primitives.serializeClass(CollectionKey.class) + "E" + stringClass + value);
        assertEquals(1, enums.size());
        assertEquals("value", enums.get(CollectionKey.VALUE));
    }

    @Test public void sizedIterableWriterRoundTripsTypedAndNullItems() {
        final String stringClass = primitives.serializeClass(String.class);
        final SetLike<String> set = X_Jdk.setLinked();
        set.add("first");
        set.add("second");
        final String setWire = codec.encode(SetLike.class, set);
        assertEquals("A" + stringClass + primitives.serializeString("first")
                + stringClass + primitives.serializeString("second"), setWire);
        final SetLike<?> readSet = (SetLike<?>) readCollectionFrame(SetLike.class, setWire);
        final java.util.Iterator<?> values = readSet.iterator();
        assertEquals("first", values.next());
        assertEquals("second", values.next());
        assertFalse(values.hasNext());
        assertEquals(2, readSet.size());

        // SetLike deliberately drops null members; ListLike preserves the existing null-item marker.
        final ListLike<Object> list = X_Jdk.listArray();
        list.add(null).add("value").add(Integer.valueOf(7)).add(null);
        final String listWire = codec.encode(ListLike.class, list);
        assertEquals("I1" + stringClass + primitives.serializeString("value")
                + primitives.serializeClass(Integer.class) + primitives.serializeLong(7) + "1", listWire);
        final ListLike<?> readList = (ListLike<?>) readCollectionFrame(ListLike.class, listWire);
        assertEquals(4, readList.size());
        assertNull(readList.get(0));
        assertEquals("value", readList.get(1));
        assertEquals(Integer.valueOf(7), readList.get(2));
        assertNull(readList.get(3));
    }

    @Test public void floatingPointDomainsAndWireRepresentationsRemainUnchanged() {
        for (final float value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                -0.0f, 0.0f, Float.MIN_VALUE, Float.MAX_VALUE, 1.25f}) {
            final String primitive = codec.encode(float.class, value);
            final String boxed = codec.encode(Float.class, value);
            assertEquals(primitives.serializeFloat(value), primitive);
            assertEquals(primitives.serializeDouble((double) value), boxed);
            assertEquals(Float.floatToIntBits(value), Float.floatToIntBits((Float) codec.decode(float.class, primitive)));
            assertEquals(Float.floatToIntBits(value), Float.floatToIntBits((Float) codec.decode(Float.class, boxed)));
        }
        for (final double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                -0.0, 0.0, Double.MIN_VALUE, Double.MAX_VALUE, 1.25}) {
            final String primitive = codec.encode(double.class, value);
            final String boxed = codec.encode(Double.class, value);
            assertEquals(primitives.serializeDouble(value), primitive);
            assertEquals(primitive, boxed);
            assertEquals(Double.doubleToLongBits(value), Double.doubleToLongBits((Double) codec.decode(double.class, primitive)));
            assertEquals(Double.doubleToLongBits(value), Double.doubleToLongBits((Double) codec.decode(Double.class, boxed)));
        }
    }
}
