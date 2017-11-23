package xapi.dist.impl;

import org.junit.Test;
import xapi.dist.api.DistOpts;
import xapi.reflect.X_Reflect;

import java.io.File;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class XapiDistTest {

    @Test
    public void testCoreUtil() throws Exception {
        // Performs complete transformation of the xapi-core-util project,
        // using our output folder as the location to dump output.
        DistGenerator generator = new DistGenerator();
        DistOpts opts = new DistOpts();
        String output = X_Reflect.getFileLoc(XapiDistTest.class)
            .replace("test-classes" + File.separator, "");
        opts.setOutputDir(new File(output, "dist"));
        opts.setWorkDir(new File(output, "work"));
        opts.addEntryPoint(DistEntryPoint.class.getCanonicalName());
        generator.process(opts);
    }

}
