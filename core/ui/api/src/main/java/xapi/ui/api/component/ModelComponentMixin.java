package xapi.ui.api.component;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/29/17.
 */
public interface ModelComponentMixin <El> {

    String getModelId(El el);

    String getModelType();

}
