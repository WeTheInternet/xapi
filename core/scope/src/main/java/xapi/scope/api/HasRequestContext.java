package xapi.scope.api;

import xapi.scope.spi.RequestContext;

/**
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 8/12/18 @ 12:57 AM.
 */
public interface HasRequestContext<Ctx extends RequestContext> {

    Ctx getContext();

}
