package xapi.dev.ui.api;

import com.github.javaparser.ast.type.ReferenceType;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.Immutable;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;
import xapi.source.X_Modifier;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.api.component.ModelComponentOptions;

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
        final MethodBuffer overload1 = cb.createMethod(methodSig)
            .makeFinal()
            .returnValue(
                buildName + "(" +
                    currentScope
                    + ")"
            );
        // Add a method which takes a scope, and wraps it in an immutable provider
        // (we use Out1<Scope> as only overridable method, so no-arg method does not have to find / create a scope)
        final MethodBuffer overload2 = cb.createMethod(methodSig)
            .makeFinal()
            .addParameter(scope, "scope")
            .returnValue(
                buildName + "(" +
                    cb.addImportStatic(Immutable.class, "immutable1") + "(scope)"
                    + ")"
            );

        // We need to know all the base api type parameters, so we can construct a generic builder
        // which can be statically created from each implementation class... but we don't want
        // to do that until everybody who wants something generated in this factory can get it.
        // TODO: if a new component wants missing features on a previously-generated component,
        // introduce overrides using X_Inject or similar.
        onSave = service -> {
            StringBuilder namesOnly = new StringBuilder();
            String elType = null;
            String componentType = null;
            for (GeneratedTypeParameter param : api.getTypeParameters()) {
                if (!param.isExposed()) {
                    continue;
                }
                cb.addGenerics(param.getType().toSource());
                if (UiNamespace.TYPE_ELEMENT.equals(param.getSystemName())) {
                    elType = param.getTypeName();
                }
                if (UiNamespace.TYPE_SELF.equals(param.getSystemName())) {
                    componentType = param.getTypeName();
                }
                namesOnly.append(namesOnly.length() == 0 ? "<" : ", ")
                    .append(param.getTypeName());

            }
            if (elType == null) {
                elType = api.getGenericInfo().findUnused(ImplLayer.Api, true, "E", "El", "Ele", "ElementType", "ET", "Element");
                namesOnly.append(namesOnly.length() == 0 ? "<" : ", ")
                    .append(elType);
                cb.addGenerics(elType);
            }
            if (namesOnly.length() > 0) {
                namesOnly.append(">");
                String generics = namesOnly.toString();
                builderMethod.getReturnType().generics = generics;
                overload1.getReturnType().generics = generics;
                overload2.getReturnType().generics = generics;
            }
            if (componentType == null) {
                componentType = api.getGenericInfo().findUnused(ImplLayer.Api, true, "C", "Comp", "Api", "A", "AC");
            }
            // Now, add one more for the component type itself.
            final String qualifiedType = componentType + " extends " + apiName + namesOnly;
            cb.addGenerics(qualifiedType);

            final FieldBuffer creator = cb
                .createField(ComponentConstructor.class, "creator", elType, componentType)
                .makePrivate().makeFinal();

            final FieldBuffer extractor = cb
                .createField(In1Out1.class, "extractor", elType, componentType)
                .makePrivate().makeFinal();

            boolean isModel = owner.hasPublicModel();

            // Compute the ComponentOptions type to use (we may add more special-cases here later...)
            String optsType;
            if (isModel) {
                // something with public state
                optsType = cb.addImport(ModelComponentOptions.class);
                final String modelType = owner.getPublicModel().getWrappedName();
                optsType += "<" + elType + ", " + modelType + ", " + componentType + ">";
            } else {
                // non-model "pure" component.
                optsType = cb.addImport(ComponentOptions.class) + namesOnly;
                optsType += "<" + elType + ", " + componentType + ">";
            }
            final FieldBuffer optsField = cb.createField(optsType, "opts")
                .makePrivate().makeFinal();

            cb.createConstructor(X_Modifier.PUBLIC, creator, extractor)
                .patternln("this.opts = new $1<>();", optsField.getRawType());

            if (isModel) {
                cb.createMethod(X_Modifier.PUBLIC, cb.getSimpleName(), "withModel")
                    .addParameter(In1.class, "callback", owner.getPublicModel().getWrappedName())
                    .println("opts.addModelListener(callback);")
                    .returnValue("this");
            }

            final String scopeService = cb.addImportStatic(X_Scope.class, "service");
            builderMethod
                .patternln("final $1[] component = {null};", apiName)
                .patternln("$1().runInScopeNoRelease(scope.out1(), s->{", scopeService)
                .indentln("component[0] = creator.constructComponent(opts, extractor);")
                .println("});")
            ;
            builderMethod.returnValue("component[0]");
        };

    }

    private In1<UiGeneratorTools<?>> onSave;

    @Override
    protected void prepareToSave(UiGeneratorTools<?> tools) {
        onSave.in(tools);
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
//        getNodeType(namespace);
        return super.getElementType(namespace);
    }

    @Override
    public void addLocalDefinition(String sysName, ReferenceType param) {
        super.addLocalDefinition(sysName, param);
    }
}
