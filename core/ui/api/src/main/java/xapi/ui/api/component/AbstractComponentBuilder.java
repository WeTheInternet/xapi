package xapi.ui.api.component;

import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/15/17.
 */
public abstract class AbstractComponentBuilder<
    El,
    O extends ComponentOptions<El, C>,
    C extends IsComponent<El>
    > implements ComponentBuilder<El, O, C>{

    private ChainBuilder<ComponentBuilder<
        El,
        ? extends ComponentOptions<El, ?>,
        IsComponent<El>>
    > children = Chain.startChain();


}
