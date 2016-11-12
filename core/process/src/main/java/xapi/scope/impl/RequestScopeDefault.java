package xapi.scope.impl;

import xapi.fu.Out2;
import xapi.scope.api.RequestScope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public class RequestScopeDefault <Request, Self extends RequestScopeDefault<Request, Self>>
    extends AbstractScope <Self>
    implements RequestScope <Request> {

    private Request request;

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public String getPath() {
        throw new UnsupportedOperationException(getClass() + " must implement getPath");
    }

    @Override
    public String getBody() {
        throw new UnsupportedOperationException(getClass() + " must implement getBody");
    }

    @Override
    public Iterable<Out2<String, Iterable<String>>> getParams() {
        throw new UnsupportedOperationException(getClass() + " must implement getParams");
    }

    @Override
    public Iterable<Out2<String, Iterable<String>>> getHeaders() {
        throw new UnsupportedOperationException(getClass() + " must implement getHeaders");
    }

    @Override
    public void setRequest(Request req) {
        this.request = req;
    }
}
