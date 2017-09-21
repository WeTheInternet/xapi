package xapi.maven.api;

import org.junit.Test;
import xapi.mvn.api.MvnCoords;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/9/17.
 */
public class MvnCoordsTest {

    @Test
    public void testRoundTrip3() {
        String coords = "group:artifact:version";
        final MvnCoords<?> model = MvnCoords.fromString(coords);
        assertEquals("group", model.getGroupId());
        assertEquals("artifact", model.getArtifactId());
        assertEquals("version", model.getVersion());
        assertNull(model.getClassifier());
        assertNull(model.getPackaging());
        final String serialized = MvnCoords.toString(model);
        assertEquals("Round trip failure", coords, serialized);
    }

    @Test
    public void testRoundTrip4() {
        String coords = "group:artifact:type:version";
        final MvnCoords<?> model = MvnCoords.fromString(coords);
        assertEquals("group", model.getGroupId());
        assertEquals("artifact", model.getArtifactId());
        assertEquals("version", model.getVersion());
        assertEquals("type", model.getPackaging());
        assertNull(model.getClassifier());
        final String serialized = MvnCoords.toString(model);
        assertEquals("Round trip failure", coords, serialized);
    }

    @Test
    public void testRoundTrip5() {
        String coords = "group:artifact:type:classifier:version";
        final MvnCoords<?> model = MvnCoords.fromString(coords);
        assertEquals("group", model.getGroupId());
        assertEquals("artifact", model.getArtifactId());
        assertEquals("version", model.getVersion());
        assertEquals("type", model.getPackaging());
        assertEquals("classifier", model.getClassifier());
        final String serialized = MvnCoords.toString(model);
        assertEquals("Round trip failure", coords, serialized);
    }
}
