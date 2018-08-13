package xapi.dev.ui.tags.factories;

import xapi.dev.source.LocalVariable;
import xapi.dev.source.PrintBuffer;

/**
 * This is the interface exposed by {@link LazyInitFactory},
 * and is likely to change as it will apply more generally to
 * "a field / factory / lazy to be added to a component's class".
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/29/18.
 */
public interface GeneratedFactory {

    PrintBuffer getInitBuffer();

    PrintBuffer getInitializer();

    LocalVariable getVar();

    LocalVariable setVar(String type, String name, boolean reuseExisting);

    String getGetter();

    String getVarName();

    boolean hasVar();

    default boolean isResettable() {
        return false;
    }

    String getFieldName();
}
