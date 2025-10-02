package xapi.model.test.api;

import xapi.annotation.model.*;
import xapi.model.api.Model;
import xapi.model.api.ModelList;

///
/// TestDirectionalModel:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/09/2025 @ 00:25
@IsModel(
        modelType = "testDirKeysOnly"
        ,persistence = @Persistent(strategy=PersistenceStrategy.Remote)
        ,serializable = @Serializable(
        clientToServer=@ClientToServer(encrypted=true)
        ,serverToClient = @ServerToClient(encrypted=true)
)
)
public interface ModelTestDirectionsKeysOnly extends Model {

    @KeyOnly
    ModelList<ModelTestDirections> getList();
    default ModelList<ModelTestDirections> list() {
        return getOrCreateModelList(ModelTestDirections.class, this::getList, this::setList);
    }

    void setList(ModelList<ModelTestDirections> list);
}
