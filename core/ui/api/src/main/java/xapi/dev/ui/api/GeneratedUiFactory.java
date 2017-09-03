package xapi.dev.ui.api;

import com.github.javaparser.ast.type.ReferenceType;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.Immutable;
import xapi.fu.Out1;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;
import xapi.source.read.JavaModel.IsTypeDefinition;

/**
 * Used to collect up necessary methods on our generated component builder.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiFactory extends GeneratedUiLayer {

    private final String apiName;
    private final StringTo<GeneratedUiMethod> parameters;
    private final MethodBuffer builderMethod;

    public GeneratedUiFactory(GeneratedUiComponent owner, GeneratedUiApi api) {
        super(api, api.getPackageName(), api.getTypeName(), ImplLayer.Base, owner);
        this.apiName = api.getWrappedName();
        parameters = X_Collect.newStringMapInsertionOrdered(GeneratedUiMethod.class);

        // add our build method(s)
        final String buildName = newMethodName(getBuildMethodName());
        final ClassBuffer cb = getSource().getClassBuffer();
        final String methodSig = "public " + apiName + " " + buildName+"()";

        final String out1 = cb.addImport(Out1.class);
        final String scope = cb.addImport(Scope.class);

        builderMethod = cb.createMethod(methodSig)
                          .addParameter(out1 + "<" + scope + ">", "scope");

        final String currentScope = cb.addImport(X_Scope.class) + "::currentScope";

        // Create a no-arg method which just defers to supplying the current scope.
        cb.createMethod(methodSig)
            .makeFinal()
            .returnValue(
                buildName+"("+
                    currentScope
                +")"
            );
        // Add a method which takes a scope, and wraps it in an immutable provider
        // (we use Out1<Scope> as only overridable method, so no-arg method does not have to find / create a scope)
        cb.createMethod(methodSig)
            .makeFinal()
            .addParameter(scope, "scope")
            .returnValue(
                buildName+"("+
                    cb.addImportStatic(Immutable.class, "immutable1") + "(scope)"
                +")"
            );
    }

    @Override
    protected void prepareToSave(UiGeneratorTools<?> tools) {
        builderMethod.returnValue("null");
    }

    protected String getBuildMethodName() {
        return "build";
    }

    public String getApiName() {
        return apiName;
    }

    @Override
    protected String wrapName(String className) {
        return "Build" + apiName;
    }

    @Override
    public SourceBuilder<GeneratedJavaFile> getSource() {
        return super.getSource();
    }

    @Override
    protected IsTypeDefinition definition() {
        return IsTypeDefinition.newClass(getPackageName(), getWrappedName());
    }

    @Override
    public String getElementType(UiNamespace namespace) {
        // We want to force the base node type to come before the element type
        getNodeType(namespace);
        return super.getElementType(namespace);
    }

    @Override
    public void addLocalDefinition(String sysName, ReferenceType param) {
        super.addLocalDefinition(sysName, param);
    }
}
