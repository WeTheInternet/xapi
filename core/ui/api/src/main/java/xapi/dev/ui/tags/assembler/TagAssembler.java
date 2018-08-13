package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.ui.tags.factories.GeneratedFactory;

/**
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/28/18.
 */
public interface TagAssembler {

    UiAssemblerResult visit(
        UiAssembler assembler,
        GeneratedFactory factory,
        AssembledElement e
    );

    default String renderChild(
        AssembledUi assembly,
        UiAssembler assembler,
        AssembledElement parent,
        UiContainerExpr child
    ) {
        final UiAssemblerResult result = assembler.addChild(assembly, parent, child);
        if (result.getElement() != null) {
            return result.getElement().requireRef() + ".out1()";
        }
        return null;
    }
}
