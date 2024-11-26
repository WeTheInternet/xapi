package xapi.jre.ui.impl;

import javafx.scene.Parent;
import xapi.fu.Pointer;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.CompilerService;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public interface SelfCompilingJavaFxApp {
    default Parent compileAndGetJavaFxUi() {
        CompilerService compiler = X_Inject.singleton(CompilerService.class);
        Pointer<Parent> value = Pointer.pointer();
        final Class<?> self = getClass();
        final String generatedName = self.getPackage().getName() + ".JavaFx" + self.getSimpleName() + "Component";
        compiler.startCompile(self)
            .withSettings(s->
                s.setClearGenerateDirectory(false)
                    .resetGenerateDirectory()
            )
            .compileAndRun((cl, cls) -> {
                    final Class<?> generated = cl.loadClass(generatedName);
                    Object o = generated.newInstance();
                    Object test = generated
                        .getMethod("io", self)
                        .invoke(o, SelfCompilingJavaFxApp.this);
                    value.in((Parent)test);
                }
            );
        return value.out1();
    }
}
