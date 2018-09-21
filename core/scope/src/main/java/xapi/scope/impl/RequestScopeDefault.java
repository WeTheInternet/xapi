package xapi.scope.impl;

import xapi.scope.request.RequestScope;
import xapi.scope.spi.RequestLike;
import xapi.scope.spi.ResponseLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public class RequestScopeDefault <Req extends RequestLike, Resp extends ResponseLike, Self extends RequestScopeDefault<Req, Resp, Self>>
    extends AbstractScope <Self>
    implements RequestScope <Req, Resp> {

    private Req request;
    private Resp response;

    @Override
    public Req getRequest() {
        return request;
    }

    @Override
    public Resp getResponse() {
        return response;
    }

    @Override
    public void initialize(Req req, Resp resp) {
        this.request = req;
        this.response = resp;
    }

    @Override
    public String getProtocol() {
        return "https";
    }

    @Override
    public String getHost() {
        return request.getHeader("Host", "0.0.0.0");
    }

}
