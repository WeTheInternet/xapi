package xapi.jre.model;

import org.junit.Test;
import xapi.dev.source.CharBuffer;
import xapi.model.api.Model;
import xapi.model.api.ModelDeserializationContext;
import xapi.model.api.ModelSerializationContext;
import xapi.model.tools.ModelSerializerDefault;
import xapi.model.tools.PrimitiveSerializerDefault;
import xapi.source.lex.StringCharIterator;

import static org.junit.Assert.*;

/** Scalar target domains must reject coercion while preserving their existing wire encodings. */
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

    @Test public void narrowPrimitiveArrayLayoutRemainsUnchanged() {
        final byte[] value = new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE};
        final String wire = primitives.serializeInt(value.length) + "e4E`O";
        assertEquals(wire, codec.encode(byte[].class, value));
        assertArrayEquals(value, (byte[]) codec.decode(byte[].class, wire));
        assertInvalid("byte", () -> codec.decode(byte[].class,
                primitives.serializeInt(1) + primitives.serializeInt(256)));
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
