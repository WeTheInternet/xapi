package xapi.dev.ui.api;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.source.X_Modifier;
import xapi.ui.api.ElementInjector;

/**
 * A container for various "standard" generated ui methods.
 *
 * By querying for the name of the method, you will trigger it's inclusion,
 * so do try to defer getting information from this object until you're already
 * sure that you need to actually use the generated method in question.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/24/18.
 */
public class ReservedUiMethods {

    private final GeneratedUiComponent ui;

    private String nameNewBuilder;
    private String nameNewInjector;

    public ReservedUiMethods(GeneratedUiComponent ui) {
        this.ui = ui;
    }

    public String newBuilder(UiNamespace namespace) {
        if (nameNewBuilder == null) {
            final GeneratedUiBase baseClass = ui.getBase();
            final String builderName = baseClass.newMethodName("newBuilder");
            baseClass.getSource().getClassBuffer().makeAbstract();
            String builderType = baseClass.getElementBuilderType(namespace);
            // When in api/base layer, we will create an abstract method that impls must fill;
            baseClass.getSource().getClassBuffer().createMethod("public abstract " +builderType +" " + builderName + "()");
            ui.getImpls().forAll(impl -> {
                final UiNamespace ns = impl.reduceNamespace(namespace);
                String type = impl.getElementBuilderType(ns);
                impl.getSource()
                    .getClassBuffer().createMethod("public " + type +" " + builderName + "()")
                    .returnValue("new " + type + "()");
            });
            nameNewBuilder = builderName + "()";
        }
        // Use the concrete type
        return nameNewBuilder;
    }

    public String newInjector(UiNamespace namespace) {
        if (nameNewInjector == null) {
            final GeneratedUiBase baseClass = ui.getBase();
            final String factoryName = baseClass.newMethodName("newInjector");
            final ClassBuffer src = baseClass.getSource().getClassBuffer();
            src.makeAbstract();
            String baseType = src.addImport(ElementInjector.class);
            final String elType = baseClass.getElementType(namespace);
            baseType += "<? super " + elType + ">";

            // When in api/base layer, we will create an abstract method that impls must fill;
            src.createMethod("public abstract " + baseType +" " + factoryName + "()")
                .addParameter(elType, "el");
            ui.getImpls().forAll(impl -> {
                final UiNamespace ns = impl.reduceNamespace(namespace);
                String type = ns.getElementInjectorType(impl.getSource());
                String el = impl.getElementType(ns);
                impl.getSource()
                    .getClassBuffer().createMethod("public " + type +" " + factoryName + "()")
                    .addParameter(el, "el")
                    .returnValue("new " + type + "(el)");
            });
            nameNewInjector = factoryName;
        }
        // Use the concrete type
        return nameNewInjector;
    }

    public MethodBuffer elementResolved(UiNamespace namespace) {
        final GeneratedUiBase base = ui.getBase();
        return base.getOrCreateMethod(X_Modifier.PROTECTED,  "void", "elementResolved", init->{
            ClassBuffer output = base.getSource().getClassBuffer();
            init.setName(base.newMethodName("elementResolved"));
            String builderType = namespace.getElementInjectorType(output);
            init.addParameter(base.getElementType(namespace), "el");
            init.println(builderType + " e = " + ui.getElementInjectorConstructor(namespace) + "(el);");
        });

    }
}
