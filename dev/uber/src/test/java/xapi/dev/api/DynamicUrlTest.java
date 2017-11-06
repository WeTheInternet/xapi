package xapi.dev.api;

import org.junit.Test;
import xapi.dev.impl.dynamic.DynamicUrlBuilder;
import xapi.io.X_IO;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/7/17.
 */
public class DynamicUrlTest {

    @Test
    public void testUrlHandler() throws IOException {
        String id = "tid";
        new DynamicUrlBuilder()
            .setPath(id)
            .withValue("success");
        String result = X_IO.toStringUtf8(new URL("dynamic:tid").openConnection().getInputStream());
        assertEquals( "success", result);
    }
}
