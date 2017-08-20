package xapi.ui.api.component;

import xapi.fu.In1;

/**
 * This class is used to describe how multiple components can be stitched together.
 *
 * Instances of this are supplied to {@link ComponentBuilder} to visit the component node-graph,
 * with the assembler responsible for producing some kind of final output.
 *
 * For client-side rendering, these would produce client-side, platform-specific widgets / elements.
 * For server-side rendering, these would produce x/html, or some other set of rendering instructions
 *
 * This interface describes the assembly API, while {@link AbstractComponentAssembler} provides a basic,
 * skeletal implementation of the reusable bits of the assembly process.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/15/17.
 */
public interface ComponentAssembler {

    ComponentAssembler asParent(ComponentBuilder<?, ?, ?, ?> builder, In1<ComponentAssembler> childAssembler);

}
