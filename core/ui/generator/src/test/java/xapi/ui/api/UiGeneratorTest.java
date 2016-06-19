package xapi.ui.api;

import org.junit.Test;
import xapi.fu.Out2;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.model.CompilerSettings;

import java.io.File;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by james on 6/6/16.
 */
public class UiGeneratorTest {

    @Test
    public void testSimpleUiComponent() throws Throwable {
        CompilerService compiler = X_Inject.singleton(CompilerService.class);
        final CompilerSettings settings = new CompilerSettings();
        settings.setSourceDirectory(new File(".", "src/test/java").getCanonicalPath());
        settings.setTest(true);
        final Out2<Integer, URL> output = compiler.compileClasses(settings, SimpleUiComponent.class);
        assertThat(output.out1()).isEqualTo(0);

    }
}
