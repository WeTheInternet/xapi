package xapi.dev.components.graveyard;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceTransform;
import xapi.dev.ui.api.MetadataRoot;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.api.StyleMetadata;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.ui.service.UiService;

import static xapi.fu.Lazy.deferred1;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/1/16.
 */
public class OldContainerMetadata {

    private MetadataRoot root;
    protected final Fifo<SourceTransform> modifiers;
    private StringTo<MethodBuffer> methods;
    private boolean allowedToFail;
    private UiContainerExpr container;
    private Lazy<StyleMetadata> style = deferred1(StyleMetadata::new);
    private String elementType;
    private String componentType;
    private String type;
    private boolean $thisPrinted;
    private boolean $uiPrinted;
    private GeneratedUiImplementation implementation;
    private Out1<SourceBuilder<?>> source;

    public OldContainerMetadata() {
        modifiers = newFifo();
        methods = X_Collect.newStringMap(MethodBuffer.class);
        allowedToFail = Boolean.getBoolean("xapi.component.ignore.parse.failure");
        source = ()->implementation == null ?
            root.getGeneratedComponent() == null ? null
                : root.getGeneratedComponent().getBase().getSource()
            : implementation.getSource();
    }

    public void addModifier(SourceTransform transform) {
        modifiers.give(transform);
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

    public StyleMetadata getStyle() {
        return style.out1();
    }


    public void applyModifiers(ClassBuffer out, String input) {
        // TODO intelligent handling of multiple modifiers...
        modifiers.out(modifier -> out.printlns(modifier.transform(input)));
    }

    public MethodBuffer getMethod(String key) {
        return methods.get(key);
    }

    public MethodBuffer getMethod(String key, In1Out1<String, MethodBuffer> create) {
        return methods.getOrCreate(key, create);
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public String getElementType() {
        return elementType;
    }

    public String getElementTypeImported() {
        return getSourceBuilder().addImport(elementType);
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getTypeImported() {
        assert type.endsWith(getSourceBuilder().addImport(type))
            : "Bad type import: " + type;
        return getSourceBuilder().addImport(type);
    }

    public void ensure$this() {
        ensure$ui();
        if ($thisPrinted) {
            return;
        }
        $thisPrinted = true;
        String imported = getTypeImported();
        addModifier(ele ->
            imported + " $this = (" + imported + ") $ui.getHost(" + ele + ");"
        );
    }

    public void ensure$ui() {
        if ($uiPrinted) {
            return;
        }
        $uiPrinted = true;
        String service = getSourceBuilder().addImport(UiService.class);
        addModifier(ele ->
            service + " $ui = " + service + ".getUiService();"
        );
    }

    public SourceBuilder<?> getSourceBuilder() {
        //        return sourceBuilder == null ? getParent() == null ? null : getParent().getSourceBuilder() : sourceBuilder;
        return source.out1();
    }

    public void setRoot(MetadataRoot root) {
        this.root = root;
    }

    public ApiGeneratorContext getContext() {
        return root.getCtx();
    }

    public OldContainerMetadata setContext(ApiGeneratorContext<?> ctx) {
        root.setCtx(ctx);
        return this;
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
}

