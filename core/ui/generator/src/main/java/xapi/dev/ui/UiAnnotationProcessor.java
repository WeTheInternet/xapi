package xapi.dev.ui;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.JavacService;
import xapi.log.X_Log;
import xapi.ui.api.PhaseMap;
import xapi.ui.api.PhaseMap.PhaseNode;
import xapi.ui.api.Ui;
import xapi.ui.api.UiElement;
import xapi.ui.api.UiPhase;
import xapi.util.X_Debug;
import xapi.util.X_String;

import static xapi.source.X_Source.removePackage;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by james on 6/6/16.
 */
@SupportedAnnotationTypes({"xapi.ui.api.*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class UiAnnotationProcessor extends AbstractProcessor {

    private JavacService service;
    private PhaseMap<String> phaseMap;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        if ("true".equals(System.getProperty("xapi.no.javac.plugin"))) {
            return;
        }
        service = JavacService.instanceFor(processingEnv);

        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if ("true".equals(System.getProperty("xapi.no.javac.plugin"))) {
            return true;
        }

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

            for (Class<?> phase : UiPhase.CORE_PHASES) {
                phases.add(phase.getAnnotation(UiPhase.class));
            }

            // compute phases to run
            phaseMap = PhaseMap.toMap(phases, UiPhase::id, UiPhase::priority, UiPhase::prerequisite, UiPhase::block);

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
        SourceBuilder builder = new SourceBuilder("public class " + simpleName + "UiFactory");
        final ClassBuffer out = builder.getClassBuffer();
        builder.setPackage(fqcn.replace("." + simpleName, ""));
        String ele = builder.addImport(UiElement.class);
        if (!elements.uiTypes.isEmpty()) {
          X_Log.info(getClass(), "Examining ui types for ", element, elements);
            elements.uiTypes.forEach((type, ui)->{

                final String typeName = type.getQualifiedName().toString();
                final String pkgName = builder.getPackage();
                String enclosedName = removePackage(pkgName, typeName).replace('.', '_');

                String source = X_String.join("\n", ui.value());
                UiContainerExpr container;
                try {
                    container= JavaParser.parseUiContainer(source);
                } catch (ParseException e) {
                    throw X_Debug.rethrow(e);
                }

                MethodBuffer factory = out.createMethod("public " + ele + " create"+enclosedName);
                factory.addParameter(simpleName, "from");

                generateUiImplementations(builder, factory, type, ui, container);
            });
        }
        if (!elements.uiMethods.isEmpty()) {

        }
        System.out.println(builder);
        return false;
    }

    private void generateUiImplementations(SourceBuilder<?> out, MethodBuffer factory, TypeElement type, Ui ui, UiContainerExpr container) {

        // Generate an abstract, base class that is fully generic.
        // Then, for every known type provider on the classpath,
        // also generate a platform-specific implementation of that abstract class,
        // optionally with a number of configurable interfaces added.

        // generate generic base class.
        UiGeneratorService generator = getSuperclassGenerator();
        ComponentBuffer component = generator.initialize(service, type, ui, container);
        // Run each phase, potentially including custom phases injected by third parties
        // Note, we need to use forthcoming xapi.properties support to record annotations
        // which are annotated with @UiPhase, so a library can be compiled with a record
        // of the UiPhase added.  Currently, we do not use any custom phases.
        for (PhaseNode<String> phase : phaseMap.forEachNode()) {
            component = generator.runPhase(phase.getId(), component);
        }
        generator.finish();

//        for (UiGeneratorService uiGeneratorService : services) {
//
//            final String pkg = getClass().getPackage().getName();
//            final String simpleName = classToEnclosedSourceName(getClass());
//            final ContainerMetadata generated = uiGeneratorService.generateComponent(
//                  pkg, simpleName, container);
//
//            final SourceBuilder<?> builder = generated.getSourceBuilder();
//            System.out.println(builder);
//            String impl = factory.addImport(builder.getQualifiedName());
//            factory.returnValue("new " + impl+"().io(from);");
//        }

    }

    protected UiGeneratorService getSuperclassGenerator() {
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
