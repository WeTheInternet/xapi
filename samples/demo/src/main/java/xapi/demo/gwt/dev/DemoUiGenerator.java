package xapi.demo.gwt.dev;

import xapi.dev.components.GenerateWebComponents;
import xapi.dev.components.XapiWebComponentGenerator;
import xapi.dev.ui.UiGeneratorServiceDefault;
import xapi.dev.ui.api.UiGeneratorPlatform;
import xapi.dev.ui.api.UiGeneratorService;
import xapi.util.X_Properties;

import static xapi.dev.ui.api.UiGeneratorPlatform.PLATFORM_WEB_COMPONENT;
import static xapi.dev.ui.impl.ClasspathComponentGenerator.genDir;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/23/17.
 */
public class DemoUiGenerator extends GenerateWebComponents {

    public DemoUiGenerator() {
        this(genDir(DemoUiGenerator.class));
    }
    public DemoUiGenerator(String myLoc) {
        super(myLoc);
    }

    @Override
    protected String searchPackage() {
        return "xapi.demo.gwt";
    }

    public static void main(String ... args) {
        X_Properties.setProperty(UiGeneratorPlatform.SYSTEM_PROP_IGNORE_PLATFORM, UiGeneratorPlatform.PLATFORM_JAVA_FX);
        final DemoUiGenerator generator = new DemoUiGenerator();
        final UiGeneratorService<?> service = new UiGeneratorServiceDefault<>();
        generator.generateComponents(service);
    }

}
