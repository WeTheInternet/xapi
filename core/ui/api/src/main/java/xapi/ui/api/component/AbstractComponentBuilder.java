package xapi.ui.api.component;

import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/15/17.
 */
public abstract class AbstractComponentBuilder<
    Node,
    El extends Node,
    O extends ComponentOptions<Node, El, C>,
    C extends IsComponent<Node, El>
    > implements ComponentBuilder<Node, El, O, C>{

    private ChainBuilder<ComponentBuilder<
        Node,
        ? extends Node,
        ? extends ComponentOptions<Node, ? extends Node, ?>,
        IsComponent<Node, ? extends Node>>
    > children = Chain.startChain();


}
