package xapi.event.api;

import xapi.annotation.model.IsModel;
import xapi.fu.api.Ignore;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface IsEventType {

    @Ignore(IsModel.NAMESPACE)
    String getEventType();

}
