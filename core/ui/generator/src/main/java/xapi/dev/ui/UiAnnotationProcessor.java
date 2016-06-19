package xapi.dev.ui;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.JavacService;
import xapi.log.X_Log;
import xapi.ui.api.Ui;
import xapi.ui.api.UiBuilder;
import xapi.ui.api.UiElement;
import xapi.util.X_Debug;
import xapi.util.X_String;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by james on 6/6/16.
 */
@SupportedAnnotationTypes({"xapi.ui.api.*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class UiAnnotationProcessor extends AbstractProcessor {

    private JavacService service;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        service = JavacService.instanceFor(processingEnv);
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<TypeElement> types = new HashSet<>();
            annotations.forEach(anno->{
                final Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(anno);
                annotated.forEach(ele->{
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
            elements.uiTypes.forEach((type, ui)->{
                String enclosedName = type.getQualifiedName().toString().replace(builder.getPackage()+".", "").replace('.', '_');
                MethodBuffer factory = out.createMethod("public " + ele + " create"+enclosedName);
                factory.addParameter(simpleName, "from");
                String source = X_String.join("\n", ui.value());
                try {
                    final UiContainerExpr container = JavaParser.parseUiContainer(source);
                    generateFactoryMethod(builder, factory, type, ui, container);
                } catch (ParseException e) {
                    throw X_Debug.rethrow(e);
                }
            });
        }
        if (!elements.uiMethods.isEmpty()) {

        }
        System.out.println(builder);
        return false;
    }

    private void generateFactoryMethod(SourceBuilder<?> out, MethodBuffer factory, TypeElement type, Ui ui, UiContainerExpr container) {
        String inject = out.addImport(X_Inject.class);
        String builder = out.addImport(UiBuilder.class);
        factory.println(builder + " b = " + inject+".instance(" + builder + ".class);");
        StringTo<UiContainerExpr> refMap = RefCollectorVisitor.collectRefs(container);

        container.accept(new VoidVisitorAdapter<PrintBuffer>() {
            @Override
            public void visit(UiContainerExpr n, PrintBuffer arg) {
                arg.println("b.setType(\"" + n.getName() + "\");");
                super.visit(n, arg);
            }
        }, factory);

        factory.returnValue("b.build();");
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
