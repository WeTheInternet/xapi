package xapi.mvn.service;

import xapi.mvn.model.MvnCoords;
import xapi.mvn.model.MvnModule;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/4/16.
 */
public interface MvnCache {

    MvnModule getModule(MvnCoords<?> coords);

}
