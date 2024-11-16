package xapi.dev.ui.api;

import xapi.dev.source.ClassBuffer;
import xapi.dev.ui.tags.factories.MethodElementResolved;
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

    public static final String ROOT_INJECTOR_VAR = "inj";
    private final GeneratedUiComponent ui;

    private String nameNewBuilder;
    private String nameNewInjector;
    private String baseTypeInjector;
    private MethodElementResolved elementResolved;

    public ReservedUiMethods(GeneratedUiComponent ui) {
        this.ui = ui;
    }

    public String newBuilder(UiNamespace namespace) {
        if (nameNewBuilder == null) {
            final GeneratedUiBase baseClass = ui.getBase();
//            nameNewBuilder = baseClass.getElementBuilderType(namespace) + "()";
            final String builderName = baseClass.newMethodName(UiNamespace.METHOD_NEW_BUILDER);
            baseClass.getSource().getClassBuffer().makeAbstract();
            String builderType = baseClass.getElementBuilderType(namespace);
            // When in api/base layer, we will create an abstract method that impls must fill;
            baseClass.getSource().getClassBuffer().createMethod("public abstract " +builderType +" " + builderName + "()")
                .addParameter(boolean.class, "searchable");
            baseClass.getSource().getClassBuffer().createMethod("public " +builderType +" " + builderName + "()")
                .returnValue(builderName + "(false)");
            ui.getImpls().forAll(impl -> {
                final UiNamespace ns = impl.reduceNamespace(namespace);
                String type = impl.getElementBuilderType(ns);
                impl.getSource()
                    .getClassBuffer().createMethod("public " + type +" " + builderName + "(boolean searchable)")
                    .returnValue("new " + type + "(searchable)");
            });
            nameNewBuilder = builderName + "()";
        }
        // Use the concrete type
        return nameNewBuilder;
    }

    public String getBaseTypeInjector(UiNamespace namespace) {
        newInjector(namespace);
        return baseTypeInjector;
    }

    public String newInjector(UiNamespace namespace) {
        if (nameNewInjector == null) {

            final GeneratedUiBase baseClass = ui.getBase();
            final String factoryName = baseClass.reserveMethodName("newInjector");
            final ClassBuffer src = baseClass.getSource().getClassBuffer();
            src.makeAbstract();
            final String elType = baseClass.getElementType(namespace);
            baseTypeInjector = src.addImport(ElementInjector.class);
            baseTypeInjector += "<? super " + elType + ">";

            // When in api/base layer, we will create an abstract method that impls must fill;
            src.createMethod("public abstract " + baseTypeInjector +" " + factoryName + "()")
                .addParameter(elType, "el");
            ui.getImpls().forAll(impl -> {
                final UiNamespace ns = impl.reduceNamespace(namespace);
                String type = ns.getElementInjectorType(impl.getSource());
                final ClassBuffer cb = impl.getSource().getClassBuffer();
                String el = ns.getElementType(cb);
                cb.createMethod("public " + type +" " + factoryName + "()")
                    .addParameter(el, "el")
                    .returnValue("new " + type + "(el)");
            });
            nameNewInjector = factoryName;
        }
        // Use the concrete type
        return nameNewInjector;
    }

    public MethodElementResolved elementResolved(UiNamespace namespace) {
        if (elementResolved == null) {
            elementResolved = new MethodElementResolved(ui, namespace);
        }
        return elementResolved;

    }
}
