package xapi.test.elemental.bdd;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/9/16.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/java/xapi/test/elemental/bdd",
    glue = "xapi.test.elemental.bdd",
    format = {"pretty", "html:target/cucumber"}
)
public class TemplateGeneratorTest {

}
