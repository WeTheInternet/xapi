package xapi.source;

import org.junit.Test;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.SourceBuilder;

import java.io.Serializable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/9/17.
 */
public class ImportSectionTest {
    @Test
    public void testAddInterface() {
        SourceBuilder b = new SourceBuilder("public class Testing");
        final ClassBuffer cb = b.getClassBuffer();
        cb.createMethod("public void doStuff()")
            .addParameter("Serializable", "thing");
        cb.createInnerClass("private class Serializable {}");
        cb.addInterface(Serializable.class);
        assertTrue("Must not let foreign types override reserved local names", b.toSource().contains("implements java.io.Serializable"));
        assertFalse("Should not have imported anything", b.toSource().contains("import"));

    }
}
