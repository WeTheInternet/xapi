package xapi.scope.impl;

import xapi.scope.api.RequestScope;
import xapi.util.api.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public class RequestScopeDefault <Req extends RequestLike, Self extends RequestScopeDefault<Req, Self>>
    extends AbstractScope <Self>
    implements RequestScope <Req> {

    private Req request;

    @Override
    public Req getRequest() {
        return request;
    }

    @Override
    public void initialize(Req req) {
        this.request = req;
    }

}
