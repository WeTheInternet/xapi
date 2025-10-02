package xapi.model.test.service;

import xapi.model.test.service.TestModelService;

///
/// TestModelServiceClient:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/09/2025 @ 00:24
public class TestModelServiceClient extends TestModelService {
    @Override
    protected boolean isClientToServer() {
        return true;
    }

}
