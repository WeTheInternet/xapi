package xapi.dev.ui.tags.factories;

import xapi.dev.source.*;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.fu.Lazy;
import xapi.fu.lazy.ResettableLazy;
import xapi.source.X_Modifier;
import xapi.util.X_String;

/**
 * A class dedicated to generating a {@code Lazy<ElBuilder> } with an init method.
 *
 * This class handles the codegen, and holds onto the print buffer for that init method,
 * as well as the method-local names of variables (so instead of implicit `b` for an element builder,
 * you have a typesafe {@link #getVarName()} instead... Plus, we can also add
 * methods here to handle things like "attach some other lazy element to us".
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/29/18.
 */
public class LazyInitFactory implements GeneratedFactory {

    private final MethodBuffer init;
    private final String getter;
    private final boolean resettable;
    private final String fieldName;
    private LocalVariable var;
    private String varName = AssembledElement.BUILDER_VAR;
    private PrintBuffer initBuffer;

    public LazyInitFactory(
        ClassBuffer out,
        String type,
        String name,
        boolean resetable
    ) {
        String lazy = out.addImport(resetable ? ResettableLazy.class : Lazy.class);

        init = out.createMethod(
            X_Modifier.PROTECTED,
            type,
            "init" + X_String.toTitleCase(name)
        );

        final FieldBuffer refField = out.createField(
            out.parameterizedType(lazy, type),
            name
        ).setModifier(X_Modifier.PRIVATE);

        fieldName = refField.getName();
        refField.setInitializer("new " + lazy + "<>(this::" + init.getName() + ");");
        getter = refField.getName()+".out1()";
        this.resettable = resetable;
    }

    @Override
    public String getVarName() {
        return varName;
    }

    @Override
    public boolean hasVar() {
        return var != null;
    }

    public LocalVariable setVar(String type, String name, boolean reuseExisting) {
        assert initBuffer == null : "Do not double-set a generated lazy reference";
        // The user has supplied a variable to include at the head of the init method.
        var = init.newVariable(type, name, reuseExisting);
        this.varName = var.getName();

        initBuffer = new PrintBuffer(init.getIndentCount());
        init.addToEnd(initBuffer);
        init.returnValue(var.getName());
        return var;
    }

    public LocalVariable getOrCreateVar(String type, String name) {
        assert initBuffer == null : "Do not double-set a generated lazy reference";
        // The user has supplied a variable to include at the head of the init method.
        final LocalVariable var = init.newVariable(type, name, true);
        this.varName = var.getName();
        initBuffer = new PrintBuffer(init.getIndentCount());
        init.addToEnd(initBuffer);
        init.returnValue(var.getName());
        return var;

    }

    public PrintBuffer getInitBuffer() {
        return initBuffer == null ? init : initBuffer;
    }

    public PrintBuffer getInitializer() {
        return var.getInitializer();
    }

    @Override
    public LocalVariable getVar() {
        return var;
    }

    @Override
    public String getGetter() {
        return getter;
    }

    @Override
    public boolean isResettable() {
        return resettable;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}
