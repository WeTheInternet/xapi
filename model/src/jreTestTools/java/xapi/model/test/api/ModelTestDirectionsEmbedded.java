package xapi.model.test.api;

import xapi.annotation.model.*;
import xapi.model.api.Model;
import xapi.model.api.ModelList;

///
/// TestDirectionalModel:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 01/10/2025 @ 23:50
@IsModel(
        modelType = ModelTestDirectionsEmbedded.MODEL_TEST_DIRECTIONS_EMBEDDED
        ,persistence = @Persistent(strategy=PersistenceStrategy.Remote)
        ,serializable = @Serializable(
        clientToServer=@ClientToServer(encrypted=true)
        ,serverToClient = @ServerToClient(encrypted=true)
)
)
public interface ModelTestDirectionsEmbedded extends Model {

    static String MODEL_TEST_DIRECTIONS_EMBEDDED = "testDirEmbedded";
    ModelList<ModelTestDirections> getList();
    default ModelList<ModelTestDirections> list() {
        return getOrCreateModelList(ModelTestDirections.class, this::getList, this::setList);
    }

    void setList(ModelList<ModelTestDirections> list);
}
