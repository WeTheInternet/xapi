package xapi.dev.ui;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.api.GeneratedJavaFile;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.UiGeneratorService;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.JavacService;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.source.read.JavaModel.IsQualified;
import xapi.ui.api.PhaseMap;
import xapi.ui.api.PhaseMap.PhaseNode;
import xapi.ui.api.Ui;
import xapi.ui.api.UiPhase;
import xapi.util.X_Debug;
import xapi.util.X_String;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static xapi.fu.itr.SingletonIterator.singleItem;
import static xapi.source.X_Source.removePackage;

/**
 * Created by james on 6/6/16.
 */
@SupportedAnnotationTypes({"xapi.ui.api.*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class UiAnnotationProcessor extends AbstractProcessor {

    private JavacService service;
    private PhaseMap<String> phaseMap;
    private Messager messages;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        messages = processingEnv.getMessager();
        if ("true".equals(System.getProperty("xapi.no.javac.plugin"))) {
            messages.printMessage(Kind.NOTE, "Skipping ui annotations because system property xapi.no.javac.plugin was set to true");
            return;
        }
        service = JavacService.instanceFor(processingEnv);

        messages.printMessage(Kind.NOTE, "UiAnnotationProcessor is ready.");
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if ("true".equals(System.getProperty("xapi.no.javac.plugin"))) {
            messages.printMessage(Kind.NOTE, "Skipping ui annotations because system property xapi.no.javac.plugin was set to true");
            return true;
        }

        messages.printMessage(Kind.NOTE, "Running: Is round over? " + roundEnv.processingOver());
        if (!roundEnv.processingOver()) {
            final Types typeOracle = service.getTypes();
            final Elements elements = service.getElements();

            final In1Out1<Class<?>, DeclaredType> lookup = c->{
                final TypeElement cls = elements.getTypeElement(c.getCanonicalName());
                return typeOracle.getDeclaredType(cls);
            };



            final DeclaredType uiPhase = lookup.io(UiPhase.class);

            Set<TypeElement> types = new LinkedHashSet<>();
            Set<UiPhase> phases = new LinkedHashSet<>();
            annotations.forEach(anno->{
                final Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(anno);
                annotated.forEach(ele->{
                    UiPhase phase = ele.getAnnotation(UiPhase.class);
                    if (phase != null) {
                        phases.add(phase);
                    }
                    if (ele instanceof TypeElement) {
                        types.add((TypeElement) ele);
                    } else {
                        Element parent = ele;
                        while (!(parent instanceof TypeElement)) {
                            parent = parent.getEnclosingElement();
                        }
                        types.add((TypeElement) parent);
                    }
                });
            });

            phaseMap = PhaseMap.withDefaults(phases);
            types.forEach(this::generateControllers);
        }
        return true;
    }

    private void generateControllers(TypeElement element) {
        UiAnnotatedElements elements = findAnnotations(element);
        // Now that we have our type / method @Uis, lets create some controllers for them.
        assert elements.hasAnyAnnotations() : "Type element " + element + " has an annotation type in " +
                "xapi.ui.api package that was not handled by UiAnnotationProcessor...";
        boolean needsBinder = elements.hasBoundAnnotations();
        if (elements.hasUiAnnotations()) {
            // Anything with @Ui annotations is producing UIs
            if(generateUiFactory(element, elements)) {
                needsBinder = true;
            }
        }
        if (needsBinder) {
            // Anything with @UiField annotations is being bound to existing UIs
            generateUiBinder(element, elements);
        }
    }

    private boolean generateUiFactory(TypeElement element, UiAnnotatedElements elements) {
        String simpleName = element.getSimpleName().toString();
        String fqcn = element.getQualifiedName().toString();
        SourceBuilder<Element> builder = new SourceBuilder<>("public class " + simpleName + "UiFactory");
        final ClassBuffer out = builder.getClassBuffer();
        builder.setPackage(fqcn.replace("." + simpleName, ""));
        if (!elements.uiTypes.isEmpty()) {
          X_Log.info(getClass(), "Examining ui types for ", element, elements);
            elements.uiTypes.forEach((type, ui)->{

                final String typeName = type.getQualifiedName().toString();
                final String pkgName = builder.getPackage();
                String enclosedName = removePackage(pkgName, typeName).replace('.', '_');

                String source = X_String.join("\n", ui.value());
                UiContainerExpr container;
                try {
                    if (!source.contains("define-tag")) {
                        source = "<define-tag" +
                            " tagName=\"" +
                                (ui.type().length() == 0 ? "generated-tag" : ui.type()) +
                                "\"" +
                            " ui = " + source + "" +
                            " /define-tag>";
                    }
                    container= JavaParser.parseUiContainer(source);
                } catch (ParseException e) {
                    throw X_Debug.rethrow(e);
                }

                String ele = builder.addImport(type.getQualifiedName().toString());
                MethodBuffer factory = out.createMethod("public " + ele + " create"+enclosedName);
                factory.addParameter(simpleName, "from");

                final GeneratedJavaFile result = generateUiImplementations(builder, factory, type, ui, container);
                factory.setReturnType(result.getOwner().getApi().getWrappedName());
                if (X_Log.loggable(LogLevel.DEBUG)) {
                    X_Log.debug(UiAnnotationProcessor.class, "UiImpl", result.getSource());
                    X_Log.debug(UiAnnotationProcessor.class, "UiBase", result.getOwner().getBase().getSource());
                    X_Log.debug(UiAnnotationProcessor.class, "UiApi", result.getOwner().getApi().getSource());
                }
            });
        }
        if (!elements.uiMethods.isEmpty()) {

        }
        X_Log.trace(UiAnnotationProcessor.class, "Finished UiFactory", builder);

        return false;
    }

    private GeneratedJavaFile generateUiImplementations(SourceBuilder<Element> out, MethodBuffer factory, TypeElement type, Ui ui, UiContainerExpr container) {

        // Generate an abstract, base class that is fully generic.
        // Then, for every known type provider on the classpath,
        // also generate a platform-specific implementation of that abstract class,
        // optionally with a number of configurable interfaces added.

        // generate generic base class.
        try {

            UiGeneratorService<Element> generator = getSuperclassGenerator();
            final String pkg = service.getPackageName(type);
            IsQualified typeName = new IsQualified(pkg, type.getQualifiedName().toString().replace(pkg+".", ""));
            final ComponentBuffer component = generator.initialize(service, typeName, container);
            // Run each phase, potentially including custom phases injected by third parties
            // Note, we need to use forthcoming xapi.properties support to record annotations
            // which are annotated with @UiPhase, so a library can be compiled with a record
            // of the UiPhase added.  Currently, we do not use any custom phases.
            for (PhaseNode<String> phase : phaseMap.forEachNode()) {
                generator.runPhase(component, phase.getId());
            }
            // TODO: delay this until the annotation processor is complete,
            // so we can allow all types to run all phases before we attempt to converge to a final output
            generator.finish(singleItem(component), UiPhase.CLEANUP);
            final GeneratedJavaFile bestImpl = component.getGeneratedComponent().getBestImpl(null);
            String impl = factory.addImport(bestImpl.getWrappedName());
            factory.returnValue("new " + impl+"().io(from);");
            return bestImpl;
        } catch (Throwable t) {
            messages.printMessage(Kind.WARNING,
                "Error encountered generating ui for " + type + " from " + container + " : " + t);
            throw t;
        }

//        for (UiGeneratorService uiGeneratorService : services) {
//
//            final String pkg = getClass().getPackage().getName();
//            final String simpleName = classToEnclosedSourceName(getClass());
//            final ContainerMetadata generated = uiGeneratorService.generateComponent(
//                  pkg, simpleName, container);
//
//            final SourceBuilder<?> builder = generated.getSourceBuilder();
//            System.out.println(builder);
//        }
    }

    protected UiGeneratorService<Element> getSuperclassGenerator() {
        return X_Inject.instance(UiGeneratorService.class);
    }

    private void generateUiBinder(TypeElement element, UiAnnotatedElements elements) {

    }

    private UiAnnotatedElements findAnnotations(TypeElement element) {
        final UiAnnotatedElements elements = new UiAnnotatedElements();
        elements.maybeAddType(element);
        Map<ExecutableElement, Ui> methodUis = new LinkedHashMap<>();
        element.getEnclosedElements().forEach(ele -> {
            if (ele instanceof TypeElement) {
                // we already extracted these..
            } else if (ele instanceof ExecutableElement){
                ExecutableElement exe = (ExecutableElement) ele;
                elements.maybeAddExecutable(exe);
            } else if (ele instanceof VariableElement) {
                VariableElement var = (VariableElement) ele;
                elements.maybeAddVariable(var);
            } else {
                X_Log.warn(getClass(), "Unsupported enclosed type ", ele.getClass(), ele, "Ignoring...");
            }
        });
        return elements;
    }

}
