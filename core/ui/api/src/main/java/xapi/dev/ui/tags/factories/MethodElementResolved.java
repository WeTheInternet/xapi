package xapi.dev.ui.tags.factories;

import xapi.dev.source.*;
import xapi.dev.ui.api.GeneratedUiBase;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.ReservedUiMethods;
import xapi.dev.ui.api.UiNamespace;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.source.X_Modifier;

/**
 * Encapsulates generator-time info for the elementResolved method of a generated ui component.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/26/18.
 */
public class MethodElementResolved {
    private final MethodBuffer beforeResolved, afterResolved;
    private final PrintBuffer insertions;
    private final MethodBuffer method;

    public MethodElementResolved(GeneratedUiComponent ui, UiNamespace namespace) {
        final GeneratedUiBase base = ui.getBase();
        ClassBuffer output = base.getSource().getClassBuffer();
        final String elType = base.getElementType(namespace);

        beforeResolved = base.getOrCreateMethod(X_Modifier.PROTECTED, "void", "beforeResolved")
            .addParameter(elType, "el");
        afterResolved = base.getOrCreateMethod(X_Modifier.PROTECTED, "void", "afterResolved")
            .addParameter(elType, "el");
        insertions = new PrintBuffer(2);
        method = base.getOrCreateMethod(X_Modifier.PROTECTED, "void", "elementResolved", init -> {
            String builderType = ui.getMethods().getBaseTypeInjector(namespace);
            init.addParameter(elType, "el");

            init.println(beforeResolved.getName() +"(el);")
                .println(builderType + " " + ReservedUiMethods.ROOT_INJECTOR_VAR + " = " +
                    ui.getElementInjectorConstructor(namespace) + "(el);")
                .addToEnd(insertions);
            init
                .println("super.elementResolved(el);")
                .println(afterResolved.getName() + "(el);");
        });

    }

    public PrintBuffer beforeResolved() {
        return beforeResolved;
    }

    public PrintBuffer afterResolved() {
        return afterResolved;
    }

    public String getMethodName() {
        return method.getName();
    }

    public void append(AssembledElement e) {
        insertions.println(ReservedUiMethods.ROOT_INJECTOR_VAR + ".appendChild(" +
            e.requireRef() + ".out1().getElement()" +
            ");");
    }
}
