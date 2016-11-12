package xapi.dev.ui;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.InterestingNodeFinder.InterestingNodeResults;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.source.read.JavaModel.IsQualified;

import static xapi.fu.Immutable.immutable1;
import static xapi.inject.X_Inject.instance;

/**
 * Created by james on 6/17/16.
 */
public class ComponentBuffer {

    private final Out1<ContainerMetadata> root;

    private Out1<SourceBuilder<ContainerMetadata>> componentBinder = Lazy.deferred1(this::defaultSourceBuilder);
    private final ClassTo<ContainerMetadata> implementations;

    private Out1<DomBuffer> domBuffer = Lazy.deferred1(this::defaultDomBuffer);
    private IsQualified element;
    private InterestingNodeResults interestingNodes;

    public ComponentBuffer() {
        this(immutable1(instance(ContainerMetadata.class)), true);
    }

    public ComponentBuffer(ContainerMetadata metadata) {
        this(immutable1(metadata), true);
    }

    public ComponentBuffer(Out1<ContainerMetadata> metadata) {
        this(Lazy.deferred1(metadata), false);
    }

    public ComponentBuffer(Out1<ContainerMetadata> root, boolean immediate) {
        this.root = immediate ? immutable1(root.out1()) : Lazy.deferred1(root);
        implementations = X_Collect.newClassMap(ContainerMetadata.class);
    }

    protected SourceBuilder<ContainerMetadata> defaultSourceBuilder() {
        return root.out1().getSourceBuilder();
    }

    protected DomBuffer defaultDomBuffer() {
        return instance(DomBuffer.class);
    }

    private Out1<DomBuffer> dom = Lazy.deferred1(DomBuffer::new);

    public SourceBuilder<ContainerMetadata> getBinder() {
        return componentBinder.out1();
    }

    public ContainerMetadata getRoot() {
        return root.out1();
    }

    public DomBuffer getDom() {
        return domBuffer.out1();
    }

    public IsQualified getElement() {
        return element;
    }

    public void setElement(IsQualified element) {
        this.element = element;
    }

    public void setInterestingNodes(InterestingNodeResults interestingNodes) {
        this.interestingNodes = interestingNodes;
    }

    public InterestingNodeResults getInterestingNodes() {
        return interestingNodes;
    }

    public boolean hasDataNodes() {
        return interestingNodes != null && interestingNodes.hasDataNodes();
    }

    public boolean hasCssNodes() {
        return interestingNodes != null && interestingNodes.hasCssNodes();
    }

    public boolean hasCssOrClassname() {
        return interestingNodes != null && interestingNodes.hasCssOrClassname();
    }

    public boolean hasTemplateReferences() {
        return interestingNodes != null && interestingNodes.hasTemplateReferences();
    }

    public ContainerMetadata getImplementation(Class<? extends UiImplementationGenerator> implType) {
        final ContainerMetadata r = getRoot();
        // TODO use a ClassTo...
        implementations.getOrCompute(implType, t->r.createImplementation(implType));
        return r;
    }
}
