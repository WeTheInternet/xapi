package xapi.ui.api.component;

import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.model.X_Model;
import xapi.model.api.Model;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/16/17.
 */
public abstract class AbstractModelComponent<
    El,
    Mod extends Model,
    Api extends IsModelComponent<El, Mod>
>
extends AbstractComponent<El, Api>
implements IsModelComponent<El, Mod> {

    private final Lazy<Mod> model;
    private String modelId;

    public AbstractModelComponent(El element) {
        this(Immutable.immutable1(element));
    }

    @Override
    public Mod getModel() {
        return model.out1();
    }

    @SuppressWarnings("unchecked") //
    public AbstractModelComponent(ModelComponentOptions<El, Mod, Api> opts, ComponentConstructor<El, Api> constructor) {
        super(opts, constructor);
        model = Lazy.deferred1(this::initModel);
    }

    public AbstractModelComponent(Out1<El> element) {
        super(element);
        model = Lazy.deferred1(this::initModel);
    }

    @Override
    protected void elementResolved(El el) {
        modelId = getModelId(el);
        super.elementResolved(el);
    }

    private Mod initModel() {
        if (modelId != null) {
            final Model cached = X_Model.cache().getModel(modelId);
            if (cached != null) {
                return (Mod) cached;
            }
        }
        final Mod mod = createModel();

        // initialize to a blank, typed key
        mod.setKey(X_Model.newKey(getModelType()));
        return mod;
    }

    public AbstractModelComponent<El, Mod, Api> setModel(Mod model) {
        final Mod myModel = this.model.out1();
        myModel.absorb(model);
        return this;
    }

    @Override
    public ModelComponentOptions<El, Mod, Api> getOpts() {
        // our constructor forces this to be true...
        return (ModelComponentOptions<El, Mod, Api>) super.getOpts();
    }

    protected void initialize(Lazy<El> element) {
    }

}
