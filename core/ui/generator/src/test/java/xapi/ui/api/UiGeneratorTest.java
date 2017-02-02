package xapi.ui.api;

import org.junit.Test;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.api.PendingCompile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by james on 6/6/16.
 */
public class UiGeneratorTest {

    @Test
    public void testSimpleUiComponent() throws Throwable {
        CompilerService compiler = X_Inject.singleton(CompilerService.class);
        final PendingCompile output = compiler.startCompile(SimpleUiComponent.class);
        output.getSettings()
            .setClearGenerateDirectory(false)
            .resetGenerateDirectory();
        boolean[] success = new boolean[1];
        final int code = output.compileAndRun(
              (cl, classes) -> success[0] = true , true
        ).out1(); // only look at the success code returned

        assertThat(code).isEqualTo(0)
              .describedAs("Bad compile status code: {}", code);

    }
}
