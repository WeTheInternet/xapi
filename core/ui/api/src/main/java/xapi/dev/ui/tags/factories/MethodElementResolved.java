package xapi.dev.ui.tags.factories;

import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.ui.api.GeneratedUiBase;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.UiNamespace;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.dev.ui.tags.assembler.AssemblyIf;
import xapi.fu.Lazy;
import xapi.source.X_Modifier;

import static xapi.dev.ui.api.ReservedUiMethods.ROOT_INJECTOR_VAR;
import static xapi.dev.ui.api.UiNamespace.VAR_ELEMENT;

/**
 * Encapsulates generator-time info for the elementResolved method of a generated ui component.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/26/18.
 */
public class MethodElementResolved {
    private final Lazy<MethodBuffer> beforeResolved, afterResolved;
    private final PrintBuffer insertions;
    private final MethodBuffer method;
    private final GeneratedUiComponent ui;
    private final UiNamespace namespace;

    public MethodElementResolved(GeneratedUiComponent ui, UiNamespace namespace) {
        this.ui = ui;
        this.namespace = namespace;
        final GeneratedUiBase base = ui.getBase();
        String elType = base.getElementType(namespace);

        beforeResolved = Lazy.deferred1(()->base.getOrCreateMethod(X_Modifier.PROTECTED, "void", "beforeResolved")
            .addParameter(elType, VAR_ELEMENT));
        afterResolved = Lazy.deferred1(()->base.getOrCreateMethod(X_Modifier.PROTECTED, "void", "afterResolved")
            .addParameter(elType, VAR_ELEMENT));
        insertions = new PrintBuffer(2);
        method = base.getOrCreateMethod(X_Modifier.PROTECTED, "void", "onElementResolved", init -> {
            String builderType = ui.getMethods().getBaseTypeInjector(namespace);
            init.addParameter(elType, VAR_ELEMENT);

            init.patternln("$1 $2 = $3($4);",
                    builderType, ROOT_INJECTOR_VAR, ui.getElementInjectorConstructor(namespace), VAR_ELEMENT)
                .addToEnd(insertions);
            init
                .println("super.onElementResolved(el);");
        });

    }

    public PrintBuffer beforeResolved() {
        return beforeResolved.out1();
    }

    public PrintBuffer afterResolved() {
        return afterResolved.out1();
    }

    public String getMethodName() {
        return method.getName();
    }

    public void append(AssembledElement e) {
        if (e instanceof AssemblyIf && ((AssemblyIf) e).canBeEmpty()) {
            insertions.patternln("$1(el);", ((AssemblyIf)e).getRedrawMethod());
        } else {
            insertions.println(ROOT_INJECTOR_VAR + ".appendChild(" +
                e.requireRef() + ".out1().getElement()" +
                ");");
        }
    }
}
