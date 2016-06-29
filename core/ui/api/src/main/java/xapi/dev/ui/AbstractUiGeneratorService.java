package xapi.dev.ui;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import xapi.dev.ui.ContainerMetadata.MetadataRoot;
import xapi.log.X_Log;
import xapi.util.X_Debug;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by james on 6/17/16.
 */
public abstract class AbstractUiGeneratorService extends UiGeneratorTools implements UiGeneratorService {

    protected String phase;

    public AbstractUiGeneratorService() {
    }

    @Override
    public UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata) {
        final UiComponentGenerator generator = componentGenerators.get(container.getName());
        Objects.requireNonNull(generator, "Null component for " + container.getName());
        return generator;
    }

    @Override
    public UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        final UiFeatureGenerator generator = featureGenerators.get(container.getNameString());
        Objects.requireNonNull(generator, "Null feature for " + container.getNameString());
        return generator;
    }

    @Override
    public ContainerMetadata createMetadata(MetadataRoot root, UiContainerExpr n) {
        final ContainerMetadata component = new ContainerMetadata(n);
        component.setRoot(root == null ? new MetadataRoot() : root);
        return component;
    }

    @Override
    public ComponentBuffer runPhase(String id, ComponentBuffer component) {
        this.phase = id;
        return component;
    }

    protected UiContainerExpr resolveImports(Elements elements, JavaFileManager filer, TypeElement element, UiContainerExpr container) {
        return (UiContainerExpr) new ModifierVisitorAdapter<Object>(){
            @Override
            public Node visit(
                  UiContainerExpr n, Object arg
            ) {
                if ("import".equals(n.getName())) {
                    final Optional<UiAttrExpr> file = n.getAttribute("file");
                    if (!file.isPresent()) {
                        throw new IllegalArgumentException("import tags must specify a file feature");
                    }
                    String loc = ASTHelper.extractAttrValue(file.get());
                    final FileObject resource;
                    try {
                        if (loc.indexOf('/') == -1) {
                            // This file is relative to our source file
                            final PackageElement pkg = elements.getPackageOf(element);
                            String pkgName;
                            if (pkg == null) {
                                pkgName = "";
                            } else {
                                pkgName = pkg.getQualifiedName().toString();
                            }
                            resource = findFile(filer, pkgName, loc);
                        } else {
                            // Treat the file as absolute classpath uri
                            resource = findFile(filer, "", loc);
                        }
                        String src = resource.getCharContent(true).toString();
                        final UiContainerExpr newContainer = JavaParser.parseUiContainer(src);
                        return newContainer;
                    } catch (IOException | ParseException e) {
                        X_Log.error(getClass(), "Error trying to resolve import", n, e);
                    }
                }
                return super.visit(n, arg);
            }
        }.visit(container, null);
    }

    protected FileObject findFile(JavaFileManager filer, String pkgName, String loc) {
        try {
            return filer.getFileForInput(
                  StandardLocation.SOURCE_PATH,
                  pkgName,
                  loc
            );
        } catch (IOException e) {
            throw X_Debug.rethrow(e);
        }
    }

    @Override
    public UiGeneratorVisitor createVisitor(ContainerMetadata metadata) {
        return new UiGeneratorVisitor(metadata);
    }

    @Override
    public UiGeneratorService getGenerator() {
        return this;
    }
}
