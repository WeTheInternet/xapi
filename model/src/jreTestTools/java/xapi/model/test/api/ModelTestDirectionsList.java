package xapi.model.test.api;

import xapi.annotation.model.*;
import xapi.model.api.ModelList;

///
/// TestDirectionalModel:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/09/2025 @ 00:25
@IsModel(
        modelType = "testDirList"
        ,persistence = @Persistent(strategy=PersistenceStrategy.Remote)
        ,serializable = @Serializable(
        clientToServer=@ClientToServer(encrypted=true)
        ,serverToClient = @ServerToClient(encrypted=true)
)
)
public interface ModelTestDirectionsList extends ModelList<ModelTestDirections> {
}
