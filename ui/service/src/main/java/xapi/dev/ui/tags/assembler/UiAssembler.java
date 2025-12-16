package xapi.dev.ui.tags.assembler;

import net.wti.lang.parser.ast.expr.UiContainerExpr;
import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.dev.ui.tags.factories.MethodElementResolved;

/**
 * Interface used to define behavior for assembling ui structure
 * and layout rules in {@link xapi.dev.ui.tags.UiTagUiGenerator}.
 *
 * Each UiAssembler is called for each tag within a ui= or shadow= block.
 * Currently, at most two will be created per <define-tag /> (for ui/shadow features).
 *
 * For very basic tags, a default UiAssembler which does nothing more
 * than register the element for creation during structural assembly should suffice
 * (that is, if an element is not expected to participate in layout logic,
 * or does not have any inputs which may be bound to a data source,
 * then you do not need to create or register a custom UiAssembler).
 *
 * For custom logic tags, like {@code <if />} or {@code <for />},
 * handwritten UiAssemblers will be necessary; for standard structural
 * tags, we will currently handwrite each assembler, until such time
 * that we can reliably generate a UiAssembler class based upon
 * a {@code <define-tag />} used to create a custom component.
 *
 * Once we can generate UiAssemblers, we will finally have a
 * code generator generator (a solution I've been looking for
 * a problem for for a long time).
 * [Yes, three `for`s in one sentence]. :-)
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/26/18.
 */
public interface UiAssembler {

    UiAssemblerResult addRoot(AssembledUi assembly, UiContainerExpr valueExpr);

    UiAssemblerResult addChild(AssembledUi assembly, AssembledElement parent, UiContainerExpr el);

    UiAssemblerResult assembleContents(
        AssembledUi assembly,
        GeneratedFactory factory,
        AssembledElement e
    );

    default TagAssembler selectAssembler(AssembledUi assembly, AssembledElement e) {
        return assembly.getGenerator().getDefaultAssembler(this, assembly, e);
    }

    AssembledUi getAssembly();

    MethodElementResolved getElementResolved();
}
