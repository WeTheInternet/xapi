package xapi.server.model;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.Out1;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;
import xapi.model.api.ModelKey;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/24/17.
 */
public interface ModelSession extends Model {

    String MODEL_SESSION = "session";

    Out1<KeyBuilder> SESSION_KEY_BUILDER = forType(MODEL_SESSION);

    Out1<ModelBuilder<ModelSession>> SESSION_MODEL_BUILDER =
        ()->
            build(SESSION_KEY_BUILDER.out1(),
                ()->create(ModelSession.class));

    ModelKey getAuthKey();

    ModelSession setAuthKey(ModelKey authKey);

    StringTo<String> getSessionProps();

    ModelSession setSessionProps(StringTo<String> props);

    default StringTo<String> sessionProps() {
        StringTo<String> props = getSessionProps();
        if (props == null) {
            props = X_Collect.newStringMap(String.class);
            setSessionProps(props);
        }
        return props;
    }

    ModelSession setTouched(long l);

    long getTouched();
}
