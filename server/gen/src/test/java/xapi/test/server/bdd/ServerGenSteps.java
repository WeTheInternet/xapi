package xapi.test.server.bdd;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.server.api.WebApp;
import xapi.util.X_String;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static xapi.fu.MappedIterable.adaptIterable;
import static xapi.fu.iterate.ArrayIterable.iterate;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/10/16.
 */
public class ServerGenSteps implements ServerTestHelper<TestSocketServer> {

    static {
        X_Log.logLevel(LogLevel.INFO);
    }

    @After
    public void cleanup() {
        ServerTestHelper.super.cleanup();
    }

    @Override
    public TestSocketServer newServer(String name, WebApp classpath) {
        return new TestSocketServer(classpath);
    }

    @Given("^Generate web app named (.+):$")
    public void generateWebApp(String name, List<String> source) throws ParseException, ClassNotFoundException {
        String src = X_String.join("\n", source);
        final UiContainerExpr app = JavaParser.parseUiContainer(src);
        createWebApp(name, app);
    }

    @Then("^Expect web app named (\\S+) to have source:$")
    public void expectNamedWebAppToHaveSource(String name, List<String> lines) throws Throwable {
        String expected = X_String.join("\n",
            adaptIterable(lines, String::trim)
            .filter(s->!s.isEmpty())
        );
        final WebApp app = webApps.get(name);
        assertNotNull("No app named " + name, app);
        String src = app.getBaseSource();
        src = X_String.join("\n", adaptIterable(
            iterate(src.split("\n")), String::trim
            ).filter(s->!s.isEmpty())
        );
        assertEquals(expected, src);

    }
}
