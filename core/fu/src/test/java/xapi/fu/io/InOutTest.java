package xapi.fu.io;

import org.junit.Test;
import xapi.fu.In1Out1;

import static org.junit.Assert.assertEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/6/16.
 */
public class InOutTest {

    @Test
    public void mappedArrayTest() {
        In1Out1<Object, String> mapper = Object::toString;
        final Object o = new Object();
        final String[] result = In1Out1.mapArray(new String[3], mapper, o, 1, true);
        assertEquals("", o.toString(), result[0]);
        assertEquals("", "1", result[1]);
        assertEquals("", "true", result[2]);
    }

}
