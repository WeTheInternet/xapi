package xapi.model.test.api;

import xapi.annotation.model.*;
import xapi.model.api.Model;

///
/// TestDirectionalModel:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/09/2025 @ 00:25
@IsModel(
        modelType = ModelTestDirections.MODEL_TEST_DIRECTIONS
        ,persistence = @Persistent(strategy=PersistenceStrategy.Remote)
        ,serializable = @Serializable(
        clientToServer=@ClientToServer(encrypted=true)
        ,serverToClient = @ServerToClient(encrypted=true)
)
)
public interface ModelTestDirections extends Model {

    static String MODEL_TEST_DIRECTIONS = "testDirMod";
    @Serializable(
            serverToClient = @ServerToClient(enabled = true),
            clientToServer = @ClientToServer(enabled = false)
    )
    String getServerToClient();

    void setServerToClient(String rendered);

    @Serializable(
            serverToClient = @ServerToClient(enabled = false),
            clientToServer = @ClientToServer(enabled = true)
    )
    String getClientToServer();

    void setClientToServer(String value);

    @Serializable(
            serverToClient = @ServerToClient(enabled = true),
            clientToServer = @ClientToServer(enabled = true)
    )
    String getBothEnabled();

    void setBothEnabled(String data);

    @Serializable(
            serverToClient = @ServerToClient(enabled = false),
            clientToServer = @ClientToServer(enabled = false)
    )
    String getBothDisabled();

    void setBothDisabled(String bothDisabled);
}
