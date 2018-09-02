package xapi.ui.api.component;

import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.lazy.ResettableLazy;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.ui.api.ElementBuilder;

import static xapi.model.X_Model.ensureKey;

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

    private final ResettableLazy<Mod> model;
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
        model = new ResettableLazy<>(()->{
            Mod mod = opts.getModel();
            if (mod == null) {
                mod = initModel();
            } else if (modelId != null){
                ensureKey(mod.getType(), mod)
                    .setId(modelId);
            }
            opts.fireListeners(mod);
            return mod;
        });
    }

    public AbstractModelComponent(Out1<El> element) {
        super(element);
        model = new ResettableLazy<>(this::initModel);
    }


    @Override
    protected void elementResolved(El el) {
        modelId = getModelId(el);
        super.elementResolved(el);
    }

    private Mod initModel() {
        final ModelKey key;
        if (modelId == null && isElementResolved()) {
            modelId = getModelId(getElement());
        }
        if (modelId != null) {
            key = X_Model.keyFromString(modelId);
            final Model cached = X_Model.cache().getModel(key);
            if (cached != null) {
                return (Mod) cached;
            }
        } else {
            key = X_Model.newKey(getModelType());
        }
        final Mod mod = createModel();

        if (modelId != null) {
            // in case user did something weird to set modelId in createModel (instead of just setting id themselves)
            key.setId(modelId);
        }
        mod.setKey(key);
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

    @Override
    public <N extends ElementBuilder<?>> N intoBuilder(IsComponent<?> logicalParent, ComponentOptions<El, Api> opts,  N into) {
        ModelComponentOptions<?, ?, ?> modelOpts = (ModelComponentOptions) opts;
        if (model.isResolved()) {
            final Mod mod = model.out1();
            X_Model.ensureKey(getModelType(), mod);
            applyAttribute(into, mod);
        } else {
            final Mod mod = (Mod) modelOpts.getModel();
            if (mod != null) {
                X_Model.ensureKey(getModelType(), mod);
                model.set(mod);
                applyAttribute(into, mod);
            }
        }

        into.onCreated(e->{
            modelOpts.fireListeners(model.out1());
        });
        return super.intoBuilder(logicalParent, opts, into);
    }
}
