package xapi.test.server.bdd;

import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import xapi.fu.in.ReadAllInputStream;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.model.X_Model;
import xapi.server.gen.VertxWebAppGenerator;
import xapi.server.api.WebApp;
import xapi.server.vertx.XapiVertxServer;
import xapi.util.X_String;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/10/16.
 */
public class VertxServerSteps implements ServerTestHelper<XapiVertxServer> {

    static {
        X_Log.logLevel(LogLevel.INFO);
    }

    private XapiVertxServer newestServer;

    @After
    @Override
    public void cleanup() {
        ServerTestHelper.super.cleanup();
    }

    @Override
    public XapiVertxServer newServer(String name, WebApp classpath) {
        final XapiVertxServer server = new XapiVertxServer(classpath);
        server.getWebApp().setKey(X_Model.newKey("", "web-app", name));
        return server;
    }

    @And("^Run web app named (.+)$")
    public void runWebAppNamedHelloWorld(String name) throws Throwable {
        initializeServer(name, server->{
            newestServer = server;
            newestServer.start();
        });
    }

    @Then("^Expect url (\\S+) to return:$")
    public void expectUrlToReturn(String url, List<String> lines) throws Throwable {
        if (url.startsWith("/")) {
            url = "http://127.0.0.1:" + newestServer.getWebApp().getPort() + url;
        }
        else if (url.indexOf(':') == -1) {
            url = "http://127.0.0.1:" + newestServer.getWebApp().getPort() + "/" + url;
        }

        ReadAllInputStream all = ReadAllInputStream.read(new URL(url).openStream());
        String expected = X_String.join("\n", lines);
        String actual = new String(all.all());
        assertEquals(expected, actual);
    }

    @Given("^Use vert[.]x generator$")
    public void useVertXGenerator() throws Throwable {
        generatorFactory.in(VertxWebAppGenerator::new);
    }
}
