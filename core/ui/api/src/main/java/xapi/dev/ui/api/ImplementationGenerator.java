package xapi.dev.ui.api;

import xapi.dev.lang.gen.GeneratedTypeOwner;
import xapi.dev.ui.impl.ImplementationLayer;
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
