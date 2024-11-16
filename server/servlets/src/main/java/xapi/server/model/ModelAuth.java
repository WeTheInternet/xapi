package xapi.server.model;

import xapi.fu.Out1;
import xapi.model.X_Model;
import xapi.model.api.KeyBuilder;
import xapi.model.api.Model;
import xapi.model.api.ModelBuilder;
import xapi.model.user.ModelUser;

import static xapi.model.X_Model.create;
import static xapi.model.api.KeyBuilder.forType;
import static xapi.model.api.ModelBuilder.build;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/23/17.
 */
public interface ModelAuth extends Model {

    String MODEL_AUTH = "auth";

    Out1<KeyBuilder> AUTH_KEY_BUILDER = forType(MODEL_AUTH);

    Out1<ModelBuilder<ModelAuth>> AUTH_MODEL_BUILDER =
        ()->
            build(AUTH_KEY_BUILDER.out1(),
                ()->create(ModelAuth.class));


    String getUserId();

    ModelAuth setUserId(String id);

    String getHash();

    ModelAuth setHash(String hash);

    long getExpiration();

    ModelAuth setExpiration(long time);

    ModelUser getUser();

    default ModelUser getOrCreateUser() {
        ModelUser user = getUser();
        if (user == null) {
            user = X_Model.create(ModelUser.class);
            setUser(user);
        }
        return user;
    }

    ModelAuth setUser(ModelUser user);
}
