package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.ui.api.GeneratedUiBase;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.dev.ui.tags.factories.MethodElementResolved;
import xapi.fu.In2Out1;
import xapi.fu.Lazy;

/**
 * A default ui assembler which visits the ui contents and creates
 * a graph of {@link AssembledElement} instances.
 *
 * For now, a single UiAssembler will be used for each ui= or shadow= element.
 *
 * The initial intent is that if you want to extend the default assembler,
 * you would do so in your generator and use this class to control how ui bits are stitched together.
 *
 * It may make sense later to also have a graph of assemblers,
 * but for now, we'll *try* to make this the one-stop shop for
 * "things user might want to control about UI assembly"
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/26/18.
 */
public class DefaultUiAssembler implements UiAssembler {

    private final GeneratedUiComponent component;
    private final GeneratedUiBase baseClass;
    private final ClassBuffer out;
    private final AssembledUi assembly;
    private final AssemblyRoot root;
    private final Lazy<MethodElementResolved> elementResolved;
    private final StringTo<In2Out1<AssembledUi, AssembledElement, TagAssembler>> tagFactories;
    private final boolean hidden;

    public DefaultUiAssembler(
        AssembledUi assembly,
        AssemblyRoot root,
        boolean hidden
    ) {
        this.assembly = assembly;
        this.component = assembly.getUi();
        this.root = root;
        this.hidden = hidden;
        baseClass = component.getBase();
        out = baseClass.getSource().getClassBuffer();
        elementResolved = Lazy.deferred1(()->assembly.getUi().getMethods().elementResolved(assembly.getNamespace()));
        tagFactories = X_Collect.newStringMapInsertionOrdered(In2Out1.class);
    }

    @Override
    public UiAssemblerResult addRoot(AssembledUi assembly, UiContainerExpr el) {

        UiAssemblerResult result = addChild(assembly, root, el);

        if (!hidden && result.isAttachToRoot()) {
            // require / append to the elementResolved method.
            elementResolved.out1().append(result.getElement());
        }
        return result;
    }

    @Override
    public UiAssemblerResult addChild(AssembledUi assembly, AssembledElement parent, UiContainerExpr el) {
        final AssembledElement e = assembly.attachChild(parent, el);

        final GeneratedFactory factory = e.startElement(assembly, this, parent, el);

        final UiAssemblerResult result = assembleContents(assembly, factory, e);

        result.setHidden(hidden);

        e.finishElement(assembly, this, factory, result, el);

        if (result.hasDefaultBehavior()) {
            result.getDefaultBehavior().done();
            result.setDefaultBehavior(null);
        }

        return result;
    }

    @Override
    public UiAssemblerResult assembleContents(
        AssembledUi assembly,
        GeneratedFactory factory,
        AssembledElement e
    ) {
        final TagAssembler assembler = selectAssembler(assembly, e);
        final UiAssemblerResult result = assembler.visit(this, factory, e);
        if (result.getElement() == null) {
            result.setElement(e);
        }
        if (result.getFactory() == null) {
            result.setFactory(factory);
        }
        return result;
    }

    @Override
    public TagAssembler selectAssembler(AssembledUi assembly, AssembledElement e) {
        final In2Out1<AssembledUi, AssembledElement, TagAssembler> factory = tagFactories.get(e.getAst().getName());
        if (factory != null) {
            return factory.io(assembly, e);
        }
        return UiAssembler.super.selectAssembler(assembly, e);
    }

    public void addTagFactory(String name, In2Out1<AssembledUi, AssembledElement, TagAssembler> assemblerFactory) {
        tagFactories.put(name, assemblerFactory);
    }

    @Override
    public AssembledUi getAssembly() {
        return assembly;
    }

    @Override
    public MethodElementResolved getElementResolved() {
        return elementResolved.out1();
    }
}
