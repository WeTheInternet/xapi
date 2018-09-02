package xapi.ui.api.component;

import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.model.api.Model;

/**
 * Implementation of {@link ComponentOptions} which includes support for
 * passing along a typed model (using a ModelCache as the intermediate
 * for innerHTML constructed components)
 *
 * Created by James X. Nelson (james @wetheinter.net) on 5/25/17.
 */
public class ModelComponentOptions
    <E, M extends Model, C extends IsModelComponent<E, M>>
   extends ComponentOptions<E, C> {

    protected M model;
    protected In1<M> modelListener;

    public ModelComponentOptions<E, M, C> withComponent(C component) {
        super.withComponent(component);
        return this;
    }

    public C newComponent(E element) {
        if (element == null) {
            throw new NullPointerException();
        }
        return component.io(element);
    }

    public ModelComponentOptions<E, M, C> withComponent(In1Out1<E, C> componentFactory) {
        super.withComponent(componentFactory);
        return this;
    }

    public M getModel() {
        return model;
    }

    public void setModel(M model) {
        this.model = model;
    }

    public In1<M> getModelListener() {
        return modelListener;
    }

    public void setModelListener(In1<M> modelListener) {
        this.modelListener = modelListener;
    }

    public void addModelListener(In1<M> callback) {
        if (modelListener == null) {
            modelListener = callback;
        } else if (modelListener != callback){
            modelListener = modelListener.useAfterMe(callback);
        }
    }

    public <Mod extends Model> void fireListeners(Mod mod) {
        if (modelListener != null) {
            final In1<M> call = modelListener;
            modelListener = In1.ignored();
            call.in(model);
        }
    }
}
