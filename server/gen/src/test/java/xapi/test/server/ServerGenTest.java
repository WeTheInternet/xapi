package xapi.test.server;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/10/16.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "../test/resources/xapi/test/server",
    glue = "xapi.test.server.bdd",
    format = {"pretty", "html:target/cucumber"}
)
public class ServerGenTest {
}
