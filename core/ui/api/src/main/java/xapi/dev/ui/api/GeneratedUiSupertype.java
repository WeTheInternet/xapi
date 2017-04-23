package xapi.dev.ui.api;

import xapi.fu.iterate.ArrayIterable;
import xapi.fu.iterate.SizedIterable;
import xapi.source.X_Modifier;
import xapi.source.X_Source;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiSupertype {

    private final String packageName;
    private final String className;
    private final GeneratedUiComponent owner;
    private final SizedIterable<GeneratedTypeParameter> params;
    private int mode;

    public GeneratedUiSupertype(GeneratedUiComponent owner,
                                String packageName, String className,
                                GeneratedTypeParameter ... typeParams) {
        this.owner = owner;
        this.packageName = packageName;
        this.className = className;
        params = ArrayIterable.iterate(typeParams);
    }

    public GeneratedUiSupertype makeInterface() {
        mode = X_Modifier.INTERFACE;
        return this;
    }

    public String getQualifiedName() {
        return X_Source.qualifiedName(packageName,  className);
    }

    public boolean isInterface() {
        return (mode & X_Modifier.INTERFACE) == X_Modifier.INTERFACE;
    }

    public SizedIterable<GeneratedTypeParameter> getParams() {
        return params;
    }

    public String getName(GeneratedUiLayer importer) {
        return importer.getSource().addImport(X_Source.qualifiedName(packageName, className));
    }

    public GeneratedUiComponent getOwner() {
        return owner;
    }
}
