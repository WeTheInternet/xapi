package xapi.settings.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import org.junit.Before;
import org.junit.Test;
import xapi.io.X_IO;
import xapi.settings.api.SettingsManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/17.
 */
public class SettingsManagerTest {

    class TestSettingsManager extends SettingsManagerDefault {
        @Override
        public String settingsLocation() {
            File loc = new File(System.getProperty("java.io.tmpdir", "/tmp") , "settingsTest" + System.identityHashCode(this)+".xapi");
            if (loc.exists()) {
                for (File file : loc.listFiles()) {
                    file.delete();
                }
                loc.delete();
            }
            loc.mkdirs();
            loc.deleteOnExit();
            return loc.getAbsolutePath();
        }
    }

    TestSettingsManager manager;

    @Before
    public void before() {
        manager = new TestSettingsManager();
    }

    @Test
    public void testSerialization() throws IOException, ParseException {
        manager.getSettings()
            .addAttribute("hello", StringLiteralExpr.stringLiteral("world"));
        manager.saveSettings();
        final String location = manager.getFileLocation();
        final File file = new File(location, "settings.xapi");
        String in = X_IO.toStringUtf8(new FileInputStream(file));
        final UiContainerExpr parsed = JavaParser.parseUiContainer(location, in);
        assertEquals("Imperfect deserialization:", in, parsed.toSource());
        assertTrue(parsed.getAttribute("hello").isPresent());
        assertEquals("\"world\"", parsed.getAttributeNotNull("hello").getExpression().toSource());
    }

    @Test
    public void testSubApp() throws Throwable {
        final UiContainerExpr testApp = new UiContainerExpr("settings");
        testApp.addAttribute("test", StringLiteralExpr.stringLiteral("success"));

        manager.getSettings()
            .addAttribute("app", JsonContainerExpr.jsonObject(
                NameExpr.of("testApp"), testApp
            ));

        final SettingsManager subApp = manager.getApp("testApp");
        assertEquals("\"success\"", subApp.getSettings().getAttributeNotNull("test").getExpression().toSource());
    }
}
