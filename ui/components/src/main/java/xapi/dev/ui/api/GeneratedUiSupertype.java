package xapi.dev.ui.api;

import com.github.javaparser.ast.body.Parameter;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.lang.gen.GeneratedTypeParameter;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.SizedIterable;
import xapi.source.util.X_Modifier;
import xapi.source.X_Source;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiSupertype {

    private final String packageName;
    private final String className;
    private final GeneratedUiComponent owner;
    private SizedIterable<GeneratedTypeParameter> params;
    private final StringTo<Parameter[]> requiredConstructors;
    private int mode;

    public GeneratedUiSupertype(GeneratedUiComponent owner,
                                String packageName, String className,
                                GeneratedTypeParameter ... typeParams) {
        this.owner = owner;
        this.packageName = packageName;
        this.className = className;
        params = ArrayIterable.iterate(typeParams);
        requiredConstructors = X_Collect.newStringMap(Parameter[].class);
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

    public void requireConstructor(Parameter ... params) {
        StringBuilder sig = new StringBuilder();
        for (Parameter param : params) {
            sig.append(param.toSource());
        }
        requiredConstructors.put(sig.toString(), params);
    }

    public SizedIterable<Parameter[]> getRequiredConstructors() {
        return requiredConstructors.forEachValue();
    }

    public void addTypeParameter(int i, GeneratedTypeParameter apiEleParam) {
        params = params.mergeInsert(i, apiEleParam);
    }
}
