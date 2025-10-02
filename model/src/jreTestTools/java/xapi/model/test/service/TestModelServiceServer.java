package xapi.model.test.service;

///
/// TestModelServiceServer:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 20/09/2025 @ 00:24
public class TestModelServiceServer extends TestModelService {
    @Override
    protected boolean isClientToServer() {
        return false;
    }
}
