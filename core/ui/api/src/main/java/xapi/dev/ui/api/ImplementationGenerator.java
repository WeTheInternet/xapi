package xapi.dev.ui.api;

import xapi.dev.api.GeneratedTypeOwner;
import xapi.dev.api.ImplementationLayer;
import xapi.dev.ui.impl.UiGeneratorTools;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/2/18 @ 2:56 AM.
 */
public interface ImplementationGenerator <Owner extends GeneratedTypeOwner, Impl extends ImplementationLayer> {

    UiGeneratorService getGenerator();

    UiGeneratorTools getTools();

    String getImplName(String pkgName, String className);

    Impl getImpl(Owner component);


}
