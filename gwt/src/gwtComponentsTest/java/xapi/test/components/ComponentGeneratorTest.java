package xapi.test.components;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/14/16.
 */

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "../gwtComponentsTest/resources/xapi/test/components/bdd",
    glue = "xapi.test.components.bdd",
    format = {"pretty", "html:target/cucumber"}
)
public class ComponentGeneratorTest {

}
