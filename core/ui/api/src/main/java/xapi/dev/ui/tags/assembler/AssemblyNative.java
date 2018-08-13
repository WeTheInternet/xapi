package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.GeneratedUiBase;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.UiNamespace;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.UiTagGenerator;
import xapi.dev.ui.tags.factories.GeneratedFactory;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/26/18.
 */
public class AssemblyNative extends AssembledElement {

    /**
     * If you want a no-parent version of this constructor, see {@link AssemblyRoot} (or subclass yourself)
     *
     * @param source - The ui assembly which owns this element
     * @param parent - The parent element, never null (see {@link AssemblyRoot#SUPER_ROOT}).
     * @param ast    - The {@code <dom />} ast for the current element. never null.
     */
    public AssemblyNative(AssembledUi source, AssembledElement parent, UiContainerExpr ast) {
        super(source, parent, ast);
    }

    @Override
    public GeneratedFactory startElement(
        AssembledUi ui,
        UiAssembler assembler,
        AssembledElement parent,
        UiContainerExpr el
    ) {
        final UiTagGenerator generator = ui.getGenerator();
        final UiNamespace namespace = ui.getNamespace();
        final MethodBuffer toDom = generator.toDomMethod(namespace, ui.getUi().getBase());
        final UiGeneratorTools tools = ui.getTools();
        final GeneratedUiComponent component = ui.getUi();
        final ApiGeneratorContext ctx = ui.getContext();
        final GeneratedUiBase baseClass = component.getBase();

//        final Do undo = registerNode(n, refNode, isRoot);
//        try {
//            component.createNativeFactory(tools, ctx, n, toDom, ui.getNamespace(), refFieldName);
//        } finally {
//            if (parentRef != null) {
//                toDom.println(parentRef + ".addChild(" + refFieldName + ");");
//            }
//            undo.done();
//            refFieldName = parentRef;
//        }

        return null;
    }
}
