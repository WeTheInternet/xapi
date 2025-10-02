package xapi.model.test.service;

import xapi.jre.model.ModelServiceJre;
import xapi.model.api.ModelList;
import xapi.model.test.api.ModelTestDirections;
import xapi.model.test.api.ModelTestDirectionsEmbedded;
import xapi.model.test.api.ModelTestDirectionsKeysOnly;
import xapi.model.test.api.ModelTestDirectionsList;

///
/// TestModelService:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 01/10/2025 @ 22:38
public class TestModelService extends ModelServiceJre {
    public TestModelService() {
        getOrMakeModelManifest(ModelTestDirections.class);
        getOrMakeModelManifest(ModelTestDirectionsList.class);
        getOrMakeModelManifest(ModelList.class);
        getOrMakeModelManifest(ModelTestDirectionsKeysOnly.class);
        getOrMakeModelManifest(ModelTestDirectionsEmbedded.class);
    }
}
