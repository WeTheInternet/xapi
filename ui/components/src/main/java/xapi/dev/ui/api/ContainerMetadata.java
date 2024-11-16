package xapi.dev.ui.api;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.plugin.NodeTransformer;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.collect.fifo.Fifo;
import xapi.collect.simple.SimpleLinkedList;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.source.MethodBuffer;
import xapi.dev.debug.NameGen;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceTransform;
import xapi.dev.ui.impl.ComponentMetadataFinder;
import xapi.dev.ui.impl.ComponentMetadataQuery;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.fu.itr.SizedIterable;
import xapi.source.X_Source;

import java.util.IdentityHashMap;

import static xapi.fu.Lazy.deferred1;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/1/16.
 */
public class ContainerMetadata {

    // WARNING: If you add new fields, update copyFrom method!
    private MetadataRoot root;
    private StringTo<MethodBuffer> methods;
    private SimpleLinkedList<String> panelNames;
    private boolean allowedToFail;
    private boolean sideEffects;
    private UiContainerExpr container;
    private ContainerMetadata parent;
    private Lazy<StyleMetadata> style = deferred1(StyleMetadata::new);
    private IdentityHashMap<UiContainerExpr, ContainerMetadata> children;
    private String refName;
    private String componentType;
    private String controllerType;
    private boolean searchTypes;
    private String controllerPkg;
    private String controllerName;
    private GeneratedUiImplementation implementation;
    private Out1<SourceBuilder<?>> source;
    // WARNING: If you add new fields, update copyFrom method!

    protected void copyFrom(ContainerMetadata metadata, boolean child) {
        this.allowedToFail = metadata.allowedToFail;
        this.methods = metadata.methods;
        this.panelNames = metadata.panelNames;
        this.root = metadata.root;

        this.componentType = metadata.componentType;
        this.controllerPkg = metadata.controllerPkg;
        this.controllerType = metadata.controllerType;
        this.controllerName = metadata.controllerName;
        this.searchTypes = metadata.searchTypes;
        this.implementation = metadata.implementation;
        if (!child) {
            this.source = metadata.source;
            this.style = metadata.style;
        }
    }

    public ContainerMetadata() {
        searchTypes = true;
        methods = X_Collect.newStringMap(MethodBuffer.class);
        panelNames = new SimpleLinkedList<>();
        children = new IdentityHashMap<>();
        allowedToFail = Boolean.getBoolean("xapi.component.ignore.parse.failure");
        source = ()->implementation == null ?
                root.getGeneratedComponent() == null ? null
                : root.getGeneratedComponent().getBase().getSource()
                : implementation.getSource();
    }

    public ContainerMetadata(UiContainerExpr container) {
        this();
        setContainer(container);
    }

    protected Fifo<SourceTransform> newFifo() {
        return X_Collect.newFifo();
    }

    public boolean isAllowedToFail() {
        return allowedToFail;
    }

    public void setContainer(UiContainerExpr container) {
        // Check the container for various interesting things, like method references.
        this.container = container;
    }

    public UiContainerExpr getUi() {
        return container;
    }

    public NameGen getNameGen() {
        return root.getNames();
    }

    public StyleMetadata getStyle() {
        return style.out1();
    }

    public boolean hasStyle() {
        return style.isResolved();
    }

    public ContainerMetadata createChild(UiContainerExpr n, UiGeneratorTools tools) {
        return children.computeIfAbsent(n, ui->{
            final ContainerMetadata copy = tools.createMetadata(root, n);
            copy.parent = this;
            copy.copyFrom(this, true);
            return copy;
        });
    }

    public ContainerMetadata getParent() {
        return parent;
    }

    public void recordSideEffects(UiGeneratorTools service, UiFeatureGenerator feature) {
        recordSideEffects(service, this, feature);
    }

    public void recordSideEffects(UiGeneratorTools service, ContainerMetadata source, UiFeatureGenerator feature) {
        setSideEffects(true);
        if (parent != null) {
            parent.recordSideEffects(service, source, feature);
        }
    }

    public boolean isSideEffects() {
        return sideEffects;
    }

    public void setSideEffects(boolean sideEffects) {
        this.sideEffects = sideEffects;
    }

    public void saveMethod(String key, MethodBuffer method) {
        final MethodBuffer was = methods.put(key, method);
        assert was == null || was == method : "Attempting to reassign a method that already exists to key " + key +
              ", previous: " + was + "\n new: " + method;
    }

    public MethodBuffer getMethod(String key) {
        return methods.get(key);
    }

    public MethodBuffer getMethod(String key, In1Out1<String, MethodBuffer> create) {
        return methods.getOrCreate(key, create);
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getComponentType() {
        return componentType;
    }

    public String getControllerPackage() {
        return controllerPkg;
    }

    public String getControllerSimpleName() {
        return controllerName;
    }

    public void setControllerType(String pkgName, String simpleName) {
        this.controllerType = X_Source.qualifiedName(pkgName, simpleName);
        this.controllerPkg = pkgName;
        this.controllerName = simpleName;
    }

    public String getControllerType() {
        return controllerType;
    }

    public SourceBuilder<?> getSourceBuilder() {
//        return sourceBuilder == null ? getParent() == null ? null : getParent().getSourceBuilder() : sourceBuilder;
        return source.out1();
    }

    public void pushPanelName(String root) {
        panelNames.add(root);
    }

    public String peekPanelName() {
        return panelNames.tail();
    }

    public String popPanelName() {
        return panelNames.pop();
    }

    public MethodBuffer removeMethod(String key) {
        return methods.remove(key);
    }

    public MethodBuffer getParentMethod() {
        String parent = peekPanelName();
        final MethodBuffer method = getMethod(parent);
        assert method != null : "No method named " + parent + " found in " + this;
        return method;
    }

    public String getRefName() {
        return getRefName("ref");
    }

    public String getRefName(String backup) {

        if (refName == null) {
            final Maybe<UiAttrExpr> refAttr = container.getAttribute("ref");
            if (refAttr.isPresent()) {
                refName = ASTHelper.extractAttrValue(refAttr.get());
                root.reserveName(refName);
            } else {
                refName = newVarName(backup);
                container.addAttribute(true, new UiAttrExpr("ref", new StringLiteralExpr(refName)));
            }
        }
        return refName;
    }

    public void setRoot(MetadataRoot root) {
        this.root = root;
    }

    public void queryContainer(ComponentMetadataQuery query) {
        container.accept(new ComponentMetadataFinder(), query);
    }

    public String newVarName(String prefix) {
        // TODO: look up at parents for scoping...
        return root.newVarName(prefix);
    }

    public boolean isSearchTypes() {
        return searchTypes;
    }

    public void setSearchTypes(boolean searchTypes) {
        this.searchTypes = searchTypes;
    }

    public void registerFieldProvider(String ref, String fieldName, NodeTransformer accessor) {
        root.registerFieldMapping(ref, fieldName, accessor);
    }

    public NodeTransformer findReplacement(String ref, String var) {
        return root.findReplacement(ref, var);
    }

    public ContainerMetadata createImplementation(Class<? extends UiImplementationGenerator> implType) {
        final ContainerMetadata copy = new ContainerMetadata(container);
        copy.copyFrom(this, false);
        SimpleLinkedList<ContainerMetadata> all = new SimpleLinkedList<>();
        all.add(copy);
        children.entrySet().forEach(e->
            copy.children.put(e.getKey(), e.getValue().createImplementation(implType))
        );
        return copy;
    }

    public <T, Generic extends T> T getOrCreateFactory(
        Class<Generic> key,
        In1Out1<Class<? super Generic>, T> factory
    ) {
        return root.getOrCreateFactory(key, factory);
    }

    public ApiGeneratorContext getContext() {
        return root.getCtx();
    }

    public ContainerMetadata setContext(ApiGeneratorContext<?> ctx) {
        root.setCtx(ctx);
        return this;
    }

    public GeneratedUiComponent getGeneratedComponent() {
        return root.getGeneratedComponent();
    }

    public void setImplementation(GeneratedUiImplementation implementation) {
        this.implementation = implementation;
    }

    public GeneratedUiImplementation getImplementation() {
        return implementation;
    }

    public void setSource(Out1<SourceBuilder<?>> source) {
        this.source = source;
    }

    public SizedIterable<String> getRecommendedImports() {
        // we don't want to close on the current recommended imports, rather,
        // we return an abstraction over top that will delay asking the root for imports as long as possible.
        return SizedIterable.adaptSizedIterable(root.getRecommendedImports(), In1Out1.identity());
    }
}
