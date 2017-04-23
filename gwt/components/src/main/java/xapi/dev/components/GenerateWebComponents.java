package xapi.dev.components;

import xapi.dev.ui.UiGeneratorServiceDefault;
import xapi.dev.ui.impl.ClasspathComponentGenerator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 4/1/17.
 */
public class GenerateWebComponents extends ClasspathComponentGenerator<WebComponentContext> {

    public GenerateWebComponents(String myLoc) {
        super(myLoc);
    }

    public static void main(String ... args) {
            final String myLoc = genDir(ClasspathComponentGenerator.class);
            new GenerateWebComponents(myLoc)
                .generateComponents(new UiGeneratorServiceDefault<>());
        }
}
