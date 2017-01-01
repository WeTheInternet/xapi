package xapi.gwtc.api;

import xapi.fu.In1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/25/16.
 */
public interface ServerRecompiler {

    void useServer(In1<IsRecompiler> callback);
}
